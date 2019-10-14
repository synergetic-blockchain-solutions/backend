package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.Base64Utils

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class UserControllerTest {

    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var testUtils: TestUtilsService

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun beforeEach() {
        testUtils.clearDatabase()
    }

    @Nested
    inner class CreateUser {
        @Test
        fun `it should not allow blank names`() {
            val registrationRequest = UserRequest("", "example@example.com", "secret")
            client.post().uri("/user")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(registrationRequest)
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.message").value(`is`("Validation failed"))
                .jsonPath("$.errors").value(containsInAnyOrder("'name' must not be blank"))
        }

        @Test
        fun `it should not allow invalid emails`() {
            val registrationRequest = UserRequest("name", "example", "secret")
            client.post().uri("/user")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(registrationRequest)
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.message").value(`is`("Validation failed"))
                .jsonPath("$.errors").value(containsInAnyOrder("'email' must be a well-formed email address"))
        }

        @Test
        fun `it should not allow short password `() {
            val registrationRequest = UserRequest("name", "example@example.com", "")
            client.post().uri("/user")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(registrationRequest)
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.message").value(`is`("Validation failed"))
                .jsonPath("$.errors").value(
                    contains(
                        "'password' must have at least 6 characters"
                    )
                )
        }

        @Test
        fun `it should return all validation errors`() {
            val registrationRequest = UserRequest("", "example", "short")
            client.post().uri("/user")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(registrationRequest)
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.message").value(`is`("Validation failed"))
                .jsonPath("$.errors")
                .value(
                    containsInAnyOrder(
                        "'name' must not be blank",
                        "'email' must be a well-formed email address",
                        "'password' must have at least 6 characters"
                    )
                )
        }

        @Test
        fun `it should return the created user without the password`() {
            val registrationRequest = UserRequest("name", "example@example.com", "secret")
            client.post().uri("/user")
                .syncBody(registrationRequest)
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.name").value(`is`(registrationRequest.name))
                .jsonPath("$.email").value(`is`(registrationRequest.email))
                .jsonPath("$.password").doesNotHaveJsonPath()
        }

        @Test
        fun `it should not allow registering the same email twice`() {
            val email = "example@example.com"
            val registrationRequest = UserRequest("name", email, "secret")
            client.post().uri("/user")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(registrationRequest)
                .exchange()
                .expectStatus().isCreated
            client.post().uri("/user")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(registrationRequest)
                .exchange()
                .expectStatus().is5xxServerError
                .expectBody().jsonPath("$.message").isEqualTo("User already exists with email $email")
        }

        @Test
        fun `it should create a private group for the user`() {
            val registrationRequest = UserRequest("name", "example@example.com", "secret")
            val body = String(
                client.post().uri("/user")
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .syncBody(registrationRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody().jsonPath("$").exists()
                    .returnResult()
                    .responseBody!!
            )

            val user = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(body)

            @Suppress("UNCHECKED_CAST")
            val privateGroup = user["privateGroup"] as Map<String, Any>

            val groupId = (privateGroup.getValue("id") as Int).toLong()
            val group = groupRepository.findByIdOrNull(groupId)!!
            assertEquals(1, group.members.size)
            assertEquals(group.members.first().id, (user["id"] as Int).toLong())
        }
    }

    @Nested
    inner class ExistingUser {
        lateinit var token: String
        lateinit var user: User

        @BeforeEach
        fun beforeEach() {
            user = userService.createUser("user1", "example@example.com", "password")
            user = userService.update(
                user.email,
                user.id,
                profilePicture = ClassPathResource("test-image.jpg").file.readBytes(),
                contentType = null
            )
            val resp = client.post()
                .uri("/login")
                .syncBody(LoginRequest(email = user.email, password = "password"))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.token").exists()
                .returnResult()
                .responseBody!!
            token = ObjectMapper().registerKotlinModule().readValue<LoginResponse>(resp).token
        }

        @Nested
        inner class GetMe {
            @Test
            fun `it should return the currently authenticated user`() {
                userService.createUser("user2", "example2@example.com", "password")
                userService.createUser("user3", "example3@example.com", "password")
                userService.createUser("user4", "example4@example.com", "password")
                val body = client.get().uri("/user/me")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.id").value(`is`(user.id.toInt()))
                    .jsonPath("$.name").value(`is`(user.name))
                    .jsonPath("$.email").value(`is`(user.email))
            }
        }

        @Nested
        inner class GetUserById {
            @Test
            fun `it should return the user with the given Id`() {
                val otherUser = userService.createUser("user4", "example4@example.com", "password")
                val body = client.get().uri("/user/${otherUser.id}")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.id").value(`is`(otherUser.id.toInt()))
                    .jsonPath("$.name").value(`is`(otherUser.name))
                    .jsonPath("$.email").value(`is`(otherUser.email))
            }

            @Test
            fun `it should return a 404 if no user with that Id exists`() {
                val body = client.get().uri("/user/100")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isNotFound
            }
        }

        @Nested
        inner class GetUserByNameOrEmail {
            lateinit var user2: User
            lateinit var user3: User
            lateinit var user4: User
            lateinit var user5: User

            @BeforeEach
            fun beforeEach() {
                user2 = userService.createUser("user2", "example2@example.com", "password")
                user3 = userService.createUser("user3", "example3@example.com", "password")
                user4 = userService.createUser("user4", "example4@example.com", "password")
                user5 = userService.createUser("user4", "example5@example.com", "password")
            }

            @Test
            fun `it should get all the users if no parameters are specified`() {
                client.get().uri("/user")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(
                        containsInAnyOrder(
                            hasEntry("id", user.id.toInt()),
                            hasEntry("id", user2.id.toInt()),
                            hasEntry("id", user3.id.toInt()),
                            hasEntry("id", user4.id.toInt()),
                            hasEntry("id", user5.id.toInt())
                    ))
            }

            @Test
            fun `it should get all the users with the given name`() {
                client.get().uri("/user?name=user4")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(
                        containsInAnyOrder(
                            hasEntry("id", user4.id.toInt()),
                            hasEntry("id", user5.id.toInt())
                        )
                    )
            }

            @Test
            fun `it should get the user with the given email`() {
                client.get().uri("/user?email=${user4.email}")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(contains(hasEntry("id", user4.id.toInt())))
            }

            @Test
            fun `it should get the user with given name and given email`() {
                client.get().uri("/user?email=${user4.email}&name=user4")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(contains(hasEntry("id", user4.id.toInt())))
            }

            @Test
            fun `it should find users with a partial name match`() {
                client.get().uri("/user?name=user")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(
                        containsInAnyOrder(
                            hasEntry("id", user.id.toInt()),
                            hasEntry("id", user2.id.toInt()),
                            hasEntry("id", user3.id.toInt()),
                            hasEntry("id", user4.id.toInt()),
                            hasEntry("id", user5.id.toInt())
                        )
                    )
            }

            @Test
            fun `it should find users with a partial email match`() {
                client.get().uri("/user?email=example")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(
                        containsInAnyOrder(
                            hasEntry("id", user.id.toInt()),
                            hasEntry("id", user2.id.toInt()),
                            hasEntry("id", user3.id.toInt()),
                            hasEntry("id", user4.id.toInt()),
                            hasEntry("id", user5.id.toInt())
                        )
                    )
            }
        }

        @Nested
        inner class UpdateUserMetadata {

            lateinit var user2: User

            @BeforeEach
            fun beforeEach() {
                user2 = userService.createUser("user2", "example2@example.com", "password")
            }

            @Test
            fun `it should not allow users to update other user's metadata`() {
                client.put().uri("/user/${user2.id}")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(UserUpdateRequest("test", "test", "test"))
                    .exchange()
                    .expectStatus().isForbidden
                    .expectBody()
                    .jsonPath("$.message").exists()
            }

            @Test
            fun `it should not update the user's password if one is not specified`() {
                val updatedName = "Updated name"
                val updatedEmail = "updatedemail@example.com"
                client.put().uri("/user/${user.id}")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(UserUpdateRequest(updatedName, updatedEmail))
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.id").value(`is`(user.id.toInt()))
                    .jsonPath("$.name").value(`is`(updatedName))
                    .jsonPath("$.email").value(`is`(updatedEmail))
                    .jsonPath("$.password").doesNotExist()

                val updatedUser = userRepository.findByIdOrNull(user.id)!!
                assertEquals(updatedName, updatedUser.name)
                assertEquals(updatedEmail, updatedUser.email)
                assertEquals(user.password, updatedUser.password)
            }

            @Test
            fun `it should update the user's password if it is specified`() {
                val updatedName = "Updated name"
                val updatedEmail = "updatedemail@example.com"
                val updatedPassword = "updatedPassword"
                client.put().uri("/user/${user.id}")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(UserUpdateRequest(updatedName, updatedEmail, updatedPassword))
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.id").value(`is`(user.id.toInt()))
                    .jsonPath("$.name").value(`is`(updatedName))
                    .jsonPath("$.email").value(`is`(updatedEmail))
                    .jsonPath("$.password").doesNotExist()

                val updatedUser = userRepository.findByIdOrNull(user.id)!!
                assertEquals(updatedName, updatedUser.name)
                assertEquals(updatedEmail, updatedUser.email)
                assertNotEquals(user.password, updatedUser.password)
            }
        }

        @Nested
        inner class UpdateUserImage {

            lateinit var user2: User

            @BeforeEach
            fun beforeEach() {
                user2 = userService.createUser("user2", "example2@example.com", "password")
            }

            @Test
            fun `it should not allow users to update other user's images`() {
                client.put().uri("/user/${user2.id}/image")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(ClassPathResource("test-image.jpg"))
                    .exchange()
                    .expectStatus().isForbidden
                    .expectBody()
                    .jsonPath("$.message").exists()
            }

            @Test
            fun `it should allow users to update their own images`() {
                client.put().uri("/user/${user.id}/image")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(ClassPathResource("test-image.jpg"))
                    .exchange()
                    .expectStatus().isOk

                val updatedUser = userRepository.findByIdOrNull(user.id)!!
                assertTrue(ClassPathResource("test-image.jpg").file.readBytes().contentEquals(updatedUser.image))
            }
        }

        @Nested
        inner class DeleteUser {

            lateinit var user2: User

            @BeforeEach
            fun beforeEach() {
                user2 = userService.createUser("user2", "example2@example.com", "password")
            }

            @Test
            fun `it should not allow users to delete other users`() {
                client.delete().uri("/user/${user2.id}")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isForbidden
                    .expectBody()
                    .jsonPath("$.message").exists()
                assertNotNull(userRepository.findByIdOrNull(user.id))
            }

            @Test
            fun `it should allow users to delete themselves`() {
                client.delete().uri("/user/${user.id}")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                assertNull(userRepository.findByIdOrNull(user.id))
            }
        }

        @Nested
        inner class GetUserImage {
            lateinit var user2: User

            @BeforeEach
            fun beforeEach() {
                user2 = userService.createUser("user2", "example2@example.com", "password")
                user2 = userService.update(
                    user.email,
                    user.id,
                    profilePicture = ClassPathResource("test-image.jpg").file.readBytes(),
                    contentType = null
                )
            }

            @Test
            fun `it should get the profile picture for the user with ID in base64 encoding`() {
                val responseBody = client.get().uri("/user/${user2.id}/image")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .returnResult()
                    .responseBody!!
                assertEquals(ByteArrayResource(Base64Utils.encode(user2.image)), ByteArrayResource(responseBody))
            }

            @Test
            fun `it should return 404 if the user does not exist`() {
                client.get().uri("/user/10000/image")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isNotFound
                    .expectBody()
                    .jsonPath("$.message").exists()
            }
        }

        @Nested
        inner class GetMyImage {
            @Test
            fun `it should get the profile picture of the authenticated user in base64 encoding`() {
                val responseBody = client.get().uri("/user/me/image")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .returnResult()
                    .responseBody!!
                assertEquals(ByteArrayResource(Base64Utils.encode(user.image)), ByteArrayResource(responseBody))
            }
        }
    }
}

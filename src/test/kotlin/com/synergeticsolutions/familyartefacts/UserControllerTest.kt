package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

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

    @BeforeEach
    fun beforeEach() {
        testUtils.clearDatabase()
    }

    @Test
    fun `it should not allow blank names`() {
        val registrationRequest = RegistrationRequest("", "example@example.com", "secret")
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
        val registrationRequest = RegistrationRequest("name", "example", "secret")
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
        val registrationRequest = RegistrationRequest("name", "example@example.com", "")
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
        val registrationRequest = RegistrationRequest("", "example", "short")
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
        val registrationRequest = RegistrationRequest("name", "example@example.com", "secret")
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
        val registrationRequest = RegistrationRequest("name", email, "secret")
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
        val registrationRequest = RegistrationRequest("name", "example@example.com", "secret")
        val body = String(client.post().uri("/user")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(registrationRequest)
                .exchange()
                .expectStatus().isCreated
                .expectBody().jsonPath("$").exists()
                .returnResult()
                .responseBody!!)

        val user = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(body)

        @Suppress("UNCHECKED_CAST")
        val privateGroup = user["privateGroup"] as Map<String, Any>

        val groupId = (privateGroup.getValue("id") as Int).toLong()
        val group = groupRepository.findByIdOrNull(groupId)!!
        assertEquals(1, group.members.size)
        assertEquals(group.members.first().id, (user["id"] as Int).toLong())
    }
}
package com.synergeticsolutions.familyartefacts

import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
class RegistrationControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun clearRepository() {
        userRepository.deleteAll()
    }

    @Test
    fun `it should not allow blank names`() {
        val registrationRequest = RegistrationRequest("", "example@example.com", "secret", "secret")
        client.post().uri("/register")
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
        val registrationRequest = RegistrationRequest("name", "example", "secret", "secret")
        client.post().uri("/register")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .syncBody(registrationRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").value(`is`("Validation failed"))
            .jsonPath("$.errors").value(containsInAnyOrder("'email' must be a well-formed email address"))
    }

    @Test
    fun `it should not allow short password or confirm passwords`() {
        val registrationRequest = RegistrationRequest("name", "example@example.com", "", "")
        client.post().uri("/register")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .syncBody(registrationRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").value(`is`("Validation failed"))
            .jsonPath("$.errors").value(
                containsInAnyOrder(
                    "'password' must have at least 6 characters",
                    "'confirmPassword' must have at least 6 characters"
                )
            )
    }

    @Test
    fun `it should not allow different password and confirm passwords`() {
        val registrationRequest = RegistrationRequest("name", "example@example.com", "secret1", "secret2")
        client.post().uri("/register")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .syncBody(registrationRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").value(`is`("Validation failed"))
            .jsonPath("$.errors").value(containsInAnyOrder("'password' and 'confirmPassword' are not matching"))
    }

    @Test
    fun `it should return all validation errors`() {
        val registrationRequest = RegistrationRequest("", "example@example.com", "secret1", "secret2")
        client.post().uri("/register")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .syncBody(registrationRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").value(`is`("Validation failed"))
            .jsonPath("$.errors")
            .value(containsInAnyOrder("'password' and 'confirmPassword' are not matching", "'name' must not be blank"))
    }

    @Test
    fun `it should return the created user without the password`() {
        val registrationRequest = RegistrationRequest("name", "example@example.com", "secret", "secret")
        client.post().uri("/register")
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
        val registrationRequest = RegistrationRequest("name", email, "secret", "secret")
        client.post().uri("/register")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .syncBody(registrationRequest)
            .exchange()
            .expectStatus().isCreated
        client.post().uri("/register")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .syncBody(registrationRequest)
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody().jsonPath("$.message").isEqualTo("User already exists with email $email")
    }
}
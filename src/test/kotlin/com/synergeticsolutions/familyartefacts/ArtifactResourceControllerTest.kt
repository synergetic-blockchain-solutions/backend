package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ArtifactResourceControllerTest(
    @Autowired
    val client: WebTestClient,
    @Autowired
    val testUtils: TestUtilsService,

    @Autowired
    val userService: UserService
) {
    val email: String = "example@example.com"
    val password: String = "password"
    lateinit var token: String

    fun getToken(userEmail: String, userPassword: String): String {
        val resp = client.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .syncBody(LoginRequest(email = userEmail, password = userPassword))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()
            .responseBody!!
        return ObjectMapper().registerKotlinModule().readValue<LoginResponse>(resp).token
    }

    @BeforeEach
    fun beforeEach() {
        testUtils.clearDatabase()
        userService.createUser("name", email, password)
        token = getToken(email, password)
    }

    @Nested
    inner class CreateArtifactResource {
        @Test
        fun `it should create the artifact resource and associate it with the artifact`() {
            TODO()
        }
    }

    @Nested
    inner class GetArtifactResources {
        @Test
        fun `it should allow users with artifact access to access to the resources`() {
            TODO()
        }

        @Test
        fun `it should not allow users without artifact access to access the resources`() {
            TODO()
        }
    }

    @Nested
    inner class GetArtifactResource {
        @Test
        fun `it should allow users with artifact access to access to the resource`() {
            TODO()
        }

        @Test
        fun `it should not allow users without artifact access to access the resource`() {
            TODO()
        }
    }

    @Nested
    inner class UpdateArtifactResource {
        @Test
        fun `it should allow artifact owners to update the resource`() {
            TODO()
        }

        @Test
        fun `it should not allow users who are not the artifact's owners to update the resource`() {
            TODO()
        }

        @Test
        fun `it should allow updating the metadata only`() {
            TODO()
        }

        @Test
        fun `it should allow updating the object only`() {
            TODO()
        }
    }

    @Nested
    inner class DeleteArtifactResource {
        @Test
        fun `it should allow artifact owners to delete the resource`() {
            TODO()
        }

        @Test
        fun `it should not allow users who are not the artifact's owners to delete the resource`() {
            TODO()
        }

        @Test
        fun `it should delete the artifact from the database`() {
            TODO()
        }
    }
}

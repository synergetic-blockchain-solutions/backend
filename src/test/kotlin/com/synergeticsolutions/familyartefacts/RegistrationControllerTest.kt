package com.synergeticsolutions.familyartefacts

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RegistrationControllerTest {
    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `it should redirect to POST user endpoint`() {
        val registrationRequest = RegistrationRequest("user1", "example@example.com", "secret")
        client.post().uri("/register")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .syncBody(registrationRequest)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.MOVED_PERMANENTLY)
            .expectHeader().valueMatches(HttpHeaders.LOCATION, "/user/?")
    }
}

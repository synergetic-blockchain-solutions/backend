package com.synergeticsolutions.familyartefacts

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class LoginTest() {
    @Autowired
    lateinit var client: WebTestClient
    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var groupRepository: GroupRepository
    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun clearRepository() {
        groupRepository.saveAll(groupRepository.findAll().map { it.copy(members = mutableListOf()) })
        userRepository.saveAll(userRepository.findAll().map { it.copy(groups = mutableListOf()) })
        groupRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `it should return token to registered user with correct password`() {
        val createdUser = userService.createUser("name", "example@example.com", "password")
        val loginRequest = LoginRequest("example@example.com", "password")

        client.post().uri("/login")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(loginRequest)
                .exchange()
                .expectStatus().isOk
                .expectBody().jsonPath("$.token").exists()
    }

    @Test
    fun `it should not return token to registered user with wrong password`() {
        val createdUser = userService.createUser("name", "example@example.com", "password")
        val loginRequest = LoginRequest("example@example.com", "secret")
        client.post().uri("/login")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(loginRequest)
                .exchange()
                .expectStatus().isForbidden
                .expectBody()
    }

    @Test
    fun `it should not return token to unregistered user`() {
        val createdUser = userService.createUser("name", "example@example.com", "password")
        val loginRequest = LoginRequest("example2@example.com", "secret")
        client.post().uri("/login")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(loginRequest)
                .exchange()
                .expectStatus().isForbidden
                .expectBody()
    }
    @Test
    fun `it should respond with bad request to blank username and password`() {
        val createdUser = userService.createUser("name", "example@example.com", "password")
        val loginRequest = LoginRequest("", "")
        client.post().uri("/login")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(loginRequest)
                .exchange()
                .expectStatus().isForbidden
                .expectBody()
    }
}

package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ArtifactControllerTest {
    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var groupRepository: GroupRepository

    @Autowired
    lateinit var artifactRepository: ArtifactRepository

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var artifactService: ArtifactService

    @Autowired
    private lateinit var testUtils: TestUtilsService

    val email: String = "example@example.com"
    val password: String = "password"
    lateinit var token: String

    @BeforeEach
    fun beforeEach() {
        testUtils.clearDatabase()
    }

    @BeforeEach
    fun getToken() {
        val user = userService.createUser("name", email, password)
        userRepository.save(user)
        val result = client.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .syncBody(LoginRequest(email = email, password = password))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()
            .responseBody!!
        println(String(result))
        val response = ObjectMapper().registerKotlinModule().readValue<LoginResponse>(result)
        token = response.token
    }

    @Test
    fun `it should get all the artifacts accessible by the user`() {
        val artifacts = listOf("artifact1", "artifact2", "artifact3").map {
            artifactService.createArtifact(
                email = email,
                name = it,
                description = "description",
                groupIDs = listOf(),
                sharedWith = listOf()
            )
        }
        client.get()
            .uri("/artifact")
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$").value(hasSize<Artifact>(3))
            .jsonPath("$").value(containsInAnyOrder(artifacts))
    }

    @Test
    fun `it should filter the artifacts by the given group ID`() {
        val artifacts = mapOf(
            "artifact1" to 1,
            "artifact2" to 1,
            "artifact3" to 2
        ).map {
            artifactService.createArtifact(
                email = email,
                name = it.key,
                description = "description",
                groupIDs = listOf(it.value.toLong()),
                sharedWith = listOf()
            )
        }
        client.get()
            .uri("/artifact?groupID=1")
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$").value(hasSize<Artifact>(2))
            .jsonPath("$").value(containsInAnyOrder(artifacts.filter { it.groups.first().id == (1).toLong() }))
    }

    @Test
    fun `it should filter the artifacts by the given owner ID`() {
        val artifacts = mapOf(
            "artifact1" to 1,
            "artifact2" to 1,
            "artifact3" to 2
        ).map {
            artifactService.createArtifact(
                email = email,
                name = it.key,
                description = "description",
                groupIDs = listOf(),
                sharedWith = listOf(it.value.toLong())
            )
        }
        client.get()
            .uri("/artifact?ownerID=1")
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$").value(hasSize<Artifact>(2))
            .jsonPath("$").value(containsInAnyOrder(artifacts.filter { it.sharedWith.first().id == (1).toLong() }))
    }

    @Test
    fun `it should filter the artifacts by the given group ID and owner ID`() {
        val artifacts = mapOf(
            "artifact1" to mapOf("groupID" to 1, "ownerID" to 1),
            "artifact2" to mapOf("groupID" to 1, "ownerID" to 2),
            "artifact3" to mapOf("groupID" to 1, "ownerID" to 2)
        ).map {
            artifactService.createArtifact(
                email = email,
                name = it.key,
                description = "description",
                groupIDs = listOf(
                    it.value.getValue("groupID").toLong()
                ),
                sharedWith = listOf(
                    it.value.getValue("ownerID").toLong()
                )
            )
        }
        client.get()
            .uri("/artifact?ownerID=1&groupID=1")
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$").value(hasSize<Artifact>(1))
            .jsonPath("$").value(containsInAnyOrder(artifacts.filter {
                it.sharedWith.first().id == (1).toLong() && it.groups.first().id == (1).toLong()
            }))
    }
}
package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "36000")
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
        val resp = client.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .syncBody(LoginRequest(email = email, password = password))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()
            .responseBody!!
        token = ObjectMapper().registerKotlinModule().readValue<LoginResponse>(resp).token
    }

    @Nested
    inner class GetArtifact {

        @Test
        fun `it should get all the artifacts accessible by the user`() {
            val artifacts = listOf("artifact1", "artifact2", "artifact3").map {
                artifactService.createArtifact(
                    email = email,
                    name = it,
                    description = "description",
                    groupIDs = listOf(),
                    sharedWith = listOf(),
                    ownerIDs = listOf()
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
                .jsonPath("$").value(containsInAnyOrder(artifacts.map { hasEntry("id", it.id.toInt()) }))
        }

        @Test
        fun `it should filter the artifacts by the given group ID`() {
            val usr = userRepository.findByEmail(email)!!
            val grp1 =
                groupRepository.save(Group(name = "group1", members = mutableListOf(usr)))
            val grp2 = groupRepository.save(Group(name = "group2", members = mutableListOf(usr)))
            val artifacts = mapOf(
                "artifact1" to grp1.id,
                "artifact2" to grp1.id,
                "artifact3" to grp2.id
            ).map {
                artifactService.createArtifact(
                    email = email,
                    name = it.key,
                    description = "description",
                    groupIDs = listOf(it.value),
                    sharedWith = listOf(),
                    ownerIDs = listOf()
                )
            }
            client.get()
                .uri("/artifact?group=${grp1.id}")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$").isArray
                .jsonPath("$").value(hasSize<Artifact>(2))
                .jsonPath("$").value(containsInAnyOrder(artifacts.filter { it.groups.first().id == grp1.id }.map {
                    hasEntry(
                        "id",
                        it.id.toInt()
                    )
                }))
        }

        @Test
        fun `it should filter the artifacts by the given owner ID`() {
            val usr = userRepository.findByEmail(email)!!
            userService.createUser("name2", "example2@example.com", "password")
            val artifacts = listOf(
                "artifact1",
                "artifact2"
            ).map {
                artifactService.createArtifact(
                    email = email,
                    name = it,
                    description = "description",
                    groupIDs = listOf(),
                    sharedWith = listOf(),
                    ownerIDs = listOf()
                )
            }
            artifactService.createArtifact(
                email = "example2@example.com",
                name = "artifact3",
                description = "desc",
                groupIDs = listOf(),
                sharedWith = listOf(),
                ownerIDs = listOf()
            )
            client.get()
                .uri("/artifact?owner=${usr.id}")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$").isArray
                .jsonPath("$").value(hasSize<Artifact>(2))
                .jsonPath("$").value(containsInAnyOrder(artifacts.map { hasEntry("id", it.id.toInt()) }))
        }

        @Test
        fun `it should filter the artifacts by the given group ID and owner ID`() {
            val usr1 = userRepository.findByEmail(email)!!
            val usr2 = userService.createUser("user2", "exampl2@example.com", "password")
            val grp1 =
                groupRepository.save(Group(name = "group1", members = mutableListOf(usr1)))
            val grp2 = groupRepository.save(Group(name = "group2", members = mutableListOf(usr1)))
            val artifacts = listOf(
                artifactService.createArtifact(
                    email,
                    "artifact1",
                    "",
                    groupIDs = listOf(grp1.id),
                    sharedWith = listOf(),
                    ownerIDs = listOf()
                ),
                artifactService.createArtifact(
                    usr2.email,
                    "artifact2",
                    "",
                    groupIDs = listOf(grp1.id),
                    sharedWith = listOf(),
                    ownerIDs = listOf()
                ),
                artifactService.createArtifact(
                    email,
                    "artifact3",
                    "",
                    groupIDs = listOf(grp2.id),
                    sharedWith = listOf(),
                    ownerIDs = listOf()
                )
            )
            client.get()
                .uri("/artifact?owner=${usr1.id}&group=${grp1.id}")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$").isArray
                .jsonPath("$").value(hasSize<Artifact>(1))
                .jsonPath("$")
                .value(containsInAnyOrder(artifacts.filter { it.groups.first().id == grp1.id && it.owners.first().id == usr1.id }.map {
                    hasEntry(
                        "id",
                        it.id.toInt()
                    )
                }))
        }
    }

    @Nested
    inner class Post {
        @Test
        fun `it should create the artifact`() {
            val artifactRequest = ArtifactRequest(
                name = "Artifact 1",
                description = "Description",
                owners = listOf(),
                groups = listOf(),
                sharedWith = listOf()
            )
            val response = client.post()
                .uri("/artifact")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .syncBody(artifactRequest)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .returnResult()
                .responseBody!!
            val returnedArtifact = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(response))
            assertTrue((returnedArtifact["id"] as Int) > 0)
            assertEquals(returnedArtifact["name"] as String, artifactRequest.name)
            assertEquals(returnedArtifact["description"] as String, artifactRequest.description)

            val createdArtifact = artifactRepository.findByIdOrNull((returnedArtifact["id"] as Int).toLong())
            assertNotNull(createdArtifact)
            assertEquals(mutableListOf(userRepository.findByEmail(email)!!.id), createdArtifact!!.owners.map(User::id))
        }
    }
}

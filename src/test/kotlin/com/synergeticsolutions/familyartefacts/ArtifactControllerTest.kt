package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasProperty
import org.hamcrest.Matchers.not
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.data.repository.findByIdOrNull
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
    lateinit var artifactResourceRepository: ArtifactResourceRepository

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var artifactService: ArtifactService

    @Autowired
    private lateinit var testUtils: TestUtilsService

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
    inner class GetArtifact {

        @Test
        fun `it should get all the artifacts accessible by the user`() {
            val artifacts = listOf("artifact1", "artifact2", "artifact3").map {
                artifactService.createArtifact(
                    email = email,
                    name = it,
                    description = "description",
                    ownerIDs = listOf(),
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
                .jsonPath("$").value(containsInAnyOrder(artifacts.map { hasEntry("id", it.id.toInt()) }))
        }

        @Test
        fun `it should filter the artifacts by the given group ID`() {
            val usr = userRepository.findByEmail(email)!!
            val grp1 =
                groupRepository.save(Group(name = "group1", members = mutableListOf(usr), description = ""))
            val grp2 = groupRepository.save(Group(name = "group2", members = mutableListOf(usr), description = ""))
            val artifacts = mapOf(
                "artifact1" to grp1.id,
                "artifact2" to grp1.id,
                "artifact3" to grp2.id
            ).map {
                artifactService.createArtifact(
                    email = email,
                    name = it.key,
                    description = "description",
                    ownerIDs = listOf(),
                    groupIDs = listOf(it.value),
                    sharedWith = listOf()
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
                    ownerIDs = listOf(),
                    groupIDs = listOf(),
                    sharedWith = listOf()
                )
            }
            artifactService.createArtifact(
                email = "example2@example.com",
                name = "artifact3",
                description = "desc",
                ownerIDs = listOf(),
                groupIDs = listOf(),
                sharedWith = listOf()
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
                groupRepository.save(Group(name = "group1", members = mutableListOf(usr1), description = ""))
            val grp2 = groupRepository.save(Group(name = "group2", members = mutableListOf(usr1), description = ""))
            val artifacts = listOf(
                artifactService.createArtifact(
                    email,
                    "artifact1",
                    "",
                    ownerIDs = listOf(),
                    groupIDs = listOf(grp1.id),
                    sharedWith = listOf()
                ),
                artifactService.createArtifact(
                    usr2.email,
                    "artifact2",
                    "",
                    ownerIDs = listOf(),
                    groupIDs = listOf(grp1.id),
                    sharedWith = listOf()
                ),
                artifactService.createArtifact(
                    email,
                    "artifact3",
                    "",
                    ownerIDs = listOf(),
                    groupIDs = listOf(grp2.id),
                    sharedWith = listOf()
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
    inner class CreateArtifact {
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

            val createdArtifact = artifactRepository.findByIdOrNull((returnedArtifact["id"] as Int).toLong())!!
            assertEquals(mutableListOf(userRepository.findByEmail(email)!!.id), createdArtifact.owners.map(User::id))
        }
    }

        @Nested
        inner class UpdateArtifact {
            @Test
            fun `it should allow owners to update the artifact`() {
                val artifactRequest = ArtifactRequest(
                    name = "Artifact 1",
                    description = "Description",
                    owners = listOf(),
                    groups = listOf(),
                    sharedWith = listOf()
                )
                val createArtifactResponse = client.post()
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
                val returnedArtifact =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createArtifactResponse))
                @Suppress("UNCHECKED_CAST")
                val updateArtifactRequest =
                    ArtifactRequest(
                        name = returnedArtifact["name"] as String,
                        description = returnedArtifact["description"] as String,
                        owners = (returnedArtifact["owners"] as List<Int>).map(Int::toLong),
                        groups = (returnedArtifact["groups"] as List<Int>).map(Int::toLong),
                        sharedWith = (returnedArtifact["owners"] as List<Int>).map(Int::toLong)
                    )
                val updateArtifactResponse = client.put()
                    .uri("/artifact/${returnedArtifact["id"]}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(updateArtifactRequest)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .returnResult()
                    .responseBody!!
                val updatedArtifactResponse =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateArtifactResponse)
                assertEquals((updatedArtifactResponse["id"] as Int).toLong(), (returnedArtifact["id"] as Int).toLong())
                assertEquals(updatedArtifactResponse["name"] as String, artifactRequest.name)
                assertEquals(updatedArtifactResponse["description"] as String, artifactRequest.description)

                val updatedArtifact =
                    artifactRepository.findByIdOrNull((updatedArtifactResponse["id"] as Int).toLong())!!
                assertEquals(
                    mutableListOf(userRepository.findByEmail(email)!!.id),
                    updatedArtifact.owners.map(User::id)
                )
                assertEquals(
                    mutableListOf(userRepository.findByEmail(email)!!.id),
                    updatedArtifact.sharedWith.map(User::id)
                )
            }

            @Test
            fun `it should allow group owners to remove the artifact from the group`() {
                var user = userRepository.findByEmail(email)!!
                var ownedGroup = groupRepository.save(
                    Group(
                        name = "Group 1",
                        description = "description",
                        artifacts = mutableListOf(),
                        admins = mutableListOf(user),
                        members = mutableListOf(user)
                    )
                )
                userRepository.save(user.copy(ownedGroups = mutableListOf(ownedGroup)))
                val artifact = artifactRepository.save(
                    Artifact(
                        name = "Artifact 1",
                        description = "Description",
                        owners = mutableListOf(),
                        groups = mutableListOf(ownedGroup),
                        sharedWith = mutableListOf()
                    )
                )
                ownedGroup = groupRepository.save(ownedGroup.copy(artifacts = mutableListOf(artifact)))
                val updateArtifactRequest =
                    ArtifactRequest(
                        name = artifact.name,
                        description = artifact.description,
                        owners = artifact.owners.map(User::id),
                        groups = artifact.groups.map(Group::id).filter { it != ownedGroup.id },
                        sharedWith = artifact.sharedWith.map(User::id)
                    )
                val updateArtifactResponse = client.put()
                    .uri("/artifact/${artifact.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(updateArtifactRequest)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .returnResult()
                    .responseBody!!
                val updatedArtifactResponse =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateArtifactResponse)
                assertEquals((updatedArtifactResponse["id"] as Int).toLong(), artifact.id)
                assertEquals(updatedArtifactResponse["name"] as String, artifact.name)
                assertEquals(updatedArtifactResponse["description"] as String, artifact.description)

                val updatedArtifact =
                    artifactRepository.findByIdOrNull((updatedArtifactResponse["id"] as Int).toLong())!!
                assertThat(updatedArtifact, hasProperty("groups", not(hasItem(hasProperty("id", `is`(ownedGroup.id))))))
            }

            @Test
            fun `it should not allow normal users to update the artifact`() {
                val artifactRequest = ArtifactRequest(
                    name = "Artifact 1",
                    description = "Description",
                    owners = listOf(),
                    groups = listOf(),
                    sharedWith = listOf()
                )

                val createArtifactResponse = client.post()
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
                val returnedArtifact =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createArtifactResponse))

                @Suppress("UNCHECKED_CAST")
                val updateArtifactRequest =
                    ArtifactRequest(
                        name = returnedArtifact["name"] as String,
                        description = returnedArtifact["description"] as String,
                        owners = (returnedArtifact["owners"] as List<Int>).map(Int::toLong),
                        groups = (returnedArtifact["groups"] as List<Int>).map(Int::toLong),
                        sharedWith = (returnedArtifact["owners"] as List<Int>).map(Int::toLong)
                    )
                val altUser = userService.createUser("user 2", "exampl2@example.com", "password")
                val altToken = getToken(altUser.email, "password")
                client.put()
                    .uri("/artifact/${returnedArtifact["id"]}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $altToken")
                    .syncBody(updateArtifactRequest)
                    .exchange()
                    .expectStatus().isForbidden
            }

            @Test
            fun `it should not allow the removal of associated resources`() {
                val artifactRequest = ArtifactRequest(
                    name = "Artifact 1",
                    description = "Description",
                    owners = listOf(),
                    groups = listOf(),
                    sharedWith = listOf()
                )
                val createArtifactResponse = client.post()
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
                val returnedArtifact =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createArtifactResponse))

                val artifact = artifactRepository.findByIdOrNull((returnedArtifact["id"] as Int).toLong())!!
                val resource = artifactResourceRepository.save(
                    ArtifactResource(
                        name = "Resource name",
                        description = "Resource description",
                        resource = ClassPathResource("test-image.jpg").file.readBytes(),
                        contentType = MediaType.IMAGE_PNG_VALUE,
                        artifact = artifact
                    )
                )

                artifactRepository.save(artifact.copy(resources = mutableListOf(resource)))

                @Suppress("UNCHECKED_CAST")
                val updateArtifactRequest = mapOf<String, Any>(
                    "name" to returnedArtifact.getValue("id"),
                    "description" to returnedArtifact.getValue("description"),
                    "owners" to returnedArtifact.getValue("owners"),
                    "sharedWith" to returnedArtifact.getValue("sharedWith"),
                    "resources" to listOf<Long>()
                )

                client.put()
                    .uri("/artifact/${returnedArtifact["id"]}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(updateArtifactRequest)
                    .exchange()
                    .expectStatus().isBadRequest
                    .expectBody()
                    .jsonPath("$.message").value(`is`("Cannot remove resources in artifact update"))
            }
        }

        @Nested
        inner class DeleteArtifact {
            @Test
            fun `it should allow owners to delete the artifact`() {
                val artifactRequest = ArtifactRequest(
                    name = "Artifact 1",
                    description = "Description",
                    owners = listOf(),
                    groups = listOf(),
                    sharedWith = listOf()
                )
                val createArtifactResponse = client.post()
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
                val returnedArtifact =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createArtifactResponse))
                client.delete()
                    .uri("/artifact/${returnedArtifact["id"]}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .returnResult()
                    .responseBody!!
                assertFalse(artifactRepository.existsById((returnedArtifact["id"] as Int).toLong()))
                val user = userRepository.findByEmail(email)!!
                val artifactId = (returnedArtifact["id"] as Int).toLong()
                assertThat(user.ownedArtifacts, not(hasItems(hasProperty("id", `is`(artifactId)))))
            }

            @Test
            fun `it should not allow normal users to delete the artifact`() {
                val artifactRequest = ArtifactRequest(
                    name = "Artifact 1",
                    description = "Description",
                    owners = listOf(),
                    groups = listOf(),
                    sharedWith = listOf()
                )

                val createArtifactResponse = client.post()
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
                val returnedArtifact =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createArtifactResponse))

                userService.createUser("user 2", "exampl2@example.com", "password")
                val altToken = getToken("exampl2@example.com", "password")
                client.delete()
                    .uri("/artifact/${returnedArtifact["id"]}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $altToken")
                    .exchange()
                    .expectStatus().isForbidden
            }

            @Test
            fun `it should delete associated resources as well`() {
                val artifactRequest = ArtifactRequest(
                    name = "Artifact 1",
                    description = "Description",
                    owners = listOf(),
                    groups = listOf(),
                    sharedWith = listOf()
                )
                val createArtifactResponse = client.post()
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
                val returnedArtifact =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createArtifactResponse))

                val artifact = artifactRepository.findByIdOrNull((returnedArtifact["id"] as Int).toLong())!!
                val resource = artifactResourceRepository.save(
                    ArtifactResource(
                        name = "Resource name",
                        description = "Resource description",
                        resource = ClassPathResource("test-image.jpg").file.readBytes(),
                        contentType = MediaType.IMAGE_PNG_VALUE,
                        artifact = artifact
                    )
                )

                artifactRepository.save(artifact.copy(resources = mutableListOf(resource)))
                client.delete()
                    .uri("/artifact/${returnedArtifact["id"]}/resource/${resource.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .returnResult()
                    .responseBody!!

                assertFalse(artifactResourceRepository.existsById(resource.id))
            }
        }
    }

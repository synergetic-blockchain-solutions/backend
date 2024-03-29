package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.synergeticsolutions.familyartefacts.dtos.ArtifactRequest
import com.synergeticsolutions.familyartefacts.dtos.LoginRequest
import com.synergeticsolutions.familyartefacts.dtos.LoginResponse
import com.synergeticsolutions.familyartefacts.entities.Album
import com.synergeticsolutions.familyartefacts.entities.Artifact
import com.synergeticsolutions.familyartefacts.entities.ArtifactResource
import com.synergeticsolutions.familyartefacts.entities.Group
import com.synergeticsolutions.familyartefacts.entities.User
import com.synergeticsolutions.familyartefacts.repositories.AlbumRepository
import com.synergeticsolutions.familyartefacts.repositories.ArtifactRepository
import com.synergeticsolutions.familyartefacts.repositories.ArtifactResourceRepository
import com.synergeticsolutions.familyartefacts.repositories.GroupRepository
import com.synergeticsolutions.familyartefacts.repositories.UserRepository
import com.synergeticsolutions.familyartefacts.services.AlbumService
import com.synergeticsolutions.familyartefacts.services.ArtifactService
import com.synergeticsolutions.familyartefacts.services.GroupService
import com.synergeticsolutions.familyartefacts.services.UserService
import java.time.Instant
import java.util.Date
import org.assertj.core.util.DateUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.hasProperty
import org.hamcrest.Matchers.not
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
    lateinit var albumRepository: AlbumRepository

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var artifactService: ArtifactService

    @Autowired
    lateinit var groupService: GroupService

    @Autowired
    lateinit var albumService: AlbumService

    @Autowired
    private lateinit var testUtils: TestUtilsService

    val email: String = "example@example.com"
    val password: String = "password"
    lateinit var token: String

    fun getToken(userEmail: String, userPassword: String): String {
        val resp = client.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(
                    LoginRequest(
                        email = userEmail,
                        password = userPassword
                    )
                )
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
                    .jsonPath("$").value(containsInAnyOrder(artifacts.map {
                        allOf(hasEntry("id", it.id.toInt()), hasKey("dateTaken"))
                }))
        }

        @Test
        fun `it should filter the artifacts by the given group ID`() {
            val usr = userRepository.findByEmail(email)!!
            val grp1 =
                    groupRepository.save(
                        Group(
                            name = "group1",
                            members = mutableListOf(usr),
                            description = ""
                        )
                    )
            val grp2 = groupRepository.save(
                Group(
                    name = "group2",
                    members = mutableListOf(usr),
                    description = ""
                )
            )
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
                        sharedWith = listOf(),
                        dateTaken = Date.from(Instant.now())
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
        fun `it should filter the artifacts by the given tag`() {
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
                        sharedWith = listOf(),
                        tags = listOf("test")
                )
            }
            artifactService.createArtifact(
                    email = email,
                    name = "artifact3",
                    description = "desc",
                    ownerIDs = listOf(),
                    groupIDs = listOf(),
                    sharedWith = listOf()
            )
            client.get()
                    .uri("/artifact?tag=test")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(hasSize<Artifact>(artifacts.size))
                    .jsonPath("$").value(containsInAnyOrder(artifacts.map { hasEntry("id", it.id.toInt()) }))
        }

        @Test
        fun `it should filter the artifacts by the given group ID and owner ID`() {
            val usr1 = userRepository.findByEmail(email)!!
            val usr2 = userService.createUser("user2", "exampl2@example.com", "password")
            val grp1 =
                    groupRepository.save(
                        Group(
                            name = "group1",
                            members = mutableListOf(usr1),
                            description = ""
                        )
                    )
            val grp2 = groupRepository.save(
                Group(
                    name = "group2",
                    members = mutableListOf(usr1),
                    description = ""
                )
            )
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

        @Test
        fun `it should get the artifact by ID`() {
            val usr = userRepository.findByEmail(email)!!
            val artifact = artifactService.createArtifact(
                    email = usr.email,
                    name = "artifact3",
                    description = "desc",
                    ownerIDs = listOf(),
                    groupIDs = listOf(),
                    sharedWith = listOf(),
                    tags = listOf("test1", "test2")
            )
            client.get()
                    .uri("/artifact/${artifact.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isNotEmpty
                    .jsonPath("$.id").value(`is`(artifact.id.toInt()))
                    .jsonPath("$.name").value(`is`(artifact.name))
                    .jsonPath("$.description").value(`is`(artifact.description))
                    .jsonPath("$.owners").value(hasItems(*(artifact.owners.map { hasEntry("id", it.id.toInt()) }.toTypedArray())))
                    .jsonPath("$.groups").value(hasItems(*(artifact.groups.map { hasEntry("id", it.id.toInt()) }).toTypedArray()))
                    .jsonPath("$.sharedWith").value(`is`(artifact.sharedWith.map { it.id.toInt() }))
                    .jsonPath("$.tags").value(`is`(artifact.tags))
        }

        @Test
        fun `it should filter by artifact name`() {
            val usr = userRepository.findByEmail(email)!!
            val grp1 = groupService.createGroup(usr.email, "group1", "description", memberIDs = listOf(), adminIDs = listOf())
            val grp2 = groupService.createGroup(usr.email, "group2", "description", memberIDs = listOf(), adminIDs = listOf())
            val artifacts = listOf(
                listOf("artifact1", grp1.id),
                listOf("artifact1", grp1.id),
                listOf("artifact3", grp2.id)
            ).map {
                artifactService.createArtifact(
                    email = email,
                    name = it[0] as String,
                    description = "description",
                    ownerIDs = listOf(),
                    groupIDs = listOf(it[1] as Long),
                    sharedWith = listOf()
                )
            }
            client.get()
                .uri("/artifact?name=artifact1")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$").isArray
                .jsonPath("$").value(hasSize<Artifact>(2))
                .jsonPath("$").value(containsInAnyOrder(artifacts.filter { it.name == "artifact1" }.map {
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
            val user = userRepository.findByEmail(email)!!
            val user2 = userService.createUser("user2", "example2@example.com", "password")
            val group = groupService.createGroup(user.email, "group1", "description", memberIDs = listOf(user2.id), adminIDs = listOf())
            val album = albumService.createAlbum(user.email, "Album", description = "Description", ownerIDs = listOf(), groupIDs = listOf(), sharedWithIDs = listOf(), artifactIDs = listOf())
            val artifactRequest = ArtifactRequest(
                name = "Artifact 1",
                description = "Description",
                owners = listOf(),
                groups = listOf(group.id),
                sharedWith = listOf(user2.id),
                tags = listOf("test1", "test2"),
                albums = listOf(album.id)
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
                    .jsonPath("$.id").value(greaterThan(0))
                    .jsonPath("$.name").value(`is`(artifactRequest.name))
                    .jsonPath("$.description").value(`is`(artifactRequest.description))
                    .jsonPath("$.owners").value(contains(hasEntry("id", user.id.toInt())))
                    .jsonPath("$.groups").value(containsInAnyOrder(hasEntry("id", user.privateGroup.id.toInt()), hasEntry("id", group.id.toInt())))
                    .jsonPath("$.sharedWith").value(contains(hasEntry("id", user2.id.toInt())))
                    .jsonPath("$.albums").value(contains(hasEntry("id", album.id.toInt())))
                    .jsonPath("$.tags").value(`is`(artifactRequest.tags))
                    .returnResult()
                    .responseBody!!
            val returnedArtifact = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(response))

            val createdArtifact = artifactRepository.findByIdOrNull((returnedArtifact["id"] as Int).toLong())!!
            // Check owners
            assertThat(createdArtifact.owners.map(User::id), contains(`is`(user.id)))

            // Check groups
            assertThat(createdArtifact.groups.map(Group::id), containsInAnyOrder(group.id, user.privateGroup.id))

            // Check shared with
            assertThat(createdArtifact.sharedWith.map(User::id), contains(user2.id))

            // Check albums
            assertThat(createdArtifact.albums.map(Album::id), contains(album.id))

            // Check tags
            assertThat(createdArtifact.tags, containsInAnyOrder(*(artifactRequest.tags!!.toTypedArray())))
        }

        @Test
        fun `it should allow the creation of artifacts with large descriptions`() {
            val user = userRepository.findByEmail(email)!!
            val user2 = userService.createUser("user2", "example2@example.com", "password")
            val group = groupService.createGroup(user.email, "group1", "description", memberIDs = listOf(user2.id), adminIDs = listOf())
            val artifactRequest = ArtifactRequest(
                name = "Artifact 1",
                description = "Description".repeat(1000),
                owners = listOf(),
                groups = listOf(group.id),
                sharedWith = listOf(user2.id),
                tags = listOf("test1", "test2")
            )
            val response = client.post()
                .uri("/artifact")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .syncBody(artifactRequest)
                .exchange()
                .expectStatus().isCreated
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
            val dateTaken = Date()
            @Suppress("UNCHECKED_CAST")
            val updateArtifactRequest =
                ArtifactRequest(
                    name = returnedArtifact["name"] as String,
                    description = returnedArtifact["description"] as String,
                    owners = (returnedArtifact["owners"] as List<Map<String, Any>>).map { (it["id"] as Int).toLong() },
                    groups = (returnedArtifact["groups"] as List<Map<String, Any>>).map { (it["id"] as Int).toLong() },
                    sharedWith = (returnedArtifact["owners"] as List<Map<String, Any>>).map { (it["id"] as Int).toLong() },
                    dateTaken = dateTaken
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

            assertEquals(DateUtil.truncateTime(dateTaken), DateUtil.truncateTime(updatedArtifact.dateTaken))
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
                    owners = (returnedArtifact["owners"] as List<Map<String, Any>>).map { (it["id"] as Int).toLong() },
                    groups = (returnedArtifact["groups"] as List<Map<String, Any>>).map { (it["id"] as Int).toLong() },
                    sharedWith = (returnedArtifact["owners"] as List<Map<String, Any>>).map { (it["id"] as Int).toLong() }
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
            val updateArtifactRequest = ArtifactRequest(
                name = returnedArtifact.getValue("name") as String,
                description = returnedArtifact.getValue("description") as String,
                owners = (returnedArtifact.getValue("owners") as List<Map<String, Any>>).map { (it.getValue("id") as Int).toLong() },
                sharedWith = (returnedArtifact.getValue("sharedWith") as List<Map<String, Any>>).map { (it.getValue("id") as Int).toLong() },
                resources = listOf(),
                groups = (returnedArtifact.getValue("groups") as List<Map<String, Any>>).map { (it.getValue("id") as Int).toLong() }
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

        @Test
        fun `it should add the specified users as owners`() {
            val artifact = artifactService.createArtifact(email, "artifact", "description")
            val user = userRepository.findByEmail(email)!!
            val user2 = userService.createUser("user2", "example2@example.com", "password")
            val updateArtifactRequest = ArtifactRequest(
                name = artifact.name,
                description = artifact.description,
                owners = artifact.owners.map(User::id) + listOf(user2.id),
                groups = listOf(),
                sharedWith = listOf(),
                tags = listOf()
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
                    .jsonPath("$.id").value(`is`(artifact.id.toInt()))
                    .jsonPath("$.name").value(`is`(updateArtifactRequest.name))
                    .jsonPath("$.description").value(`is`(updateArtifactRequest.description))
                    .jsonPath("$.owners").value(containsInAnyOrder(*(updateArtifactRequest.owners!!.map { hasEntry("id", it.toInt()) }.toTypedArray())))
                    .jsonPath("$.groups").value(containsInAnyOrder(*(updateArtifactRequest.groups!!.map { hasEntry("id", it.toInt()) }.toTypedArray())))
                    .jsonPath("$.sharedWith").value(containsInAnyOrder(*(updateArtifactRequest.sharedWith!!.map { hasEntry("id", it.toInt()) }.toTypedArray())))
                    .jsonPath("$.tags").value(`is`(updateArtifactRequest.tags))
                    .returnResult()
                    .responseBody!!
            val response = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateArtifactResponse)
            val updatedArtifact = artifactRepository.findByIdOrNull((response.getValue("id") as Int).toLong())!!
            val updatedUser = userRepository.findByIdOrNull(user.id)!!
            val updatedUser2 = userRepository.findByIdOrNull(user2.id)!!
            assertThat(updatedArtifact.owners.map(User::id), containsInAnyOrder(user.id, user2.id))
        }

        @Test
        fun `it should add the artifact to the specified groups`() {
            val artifact = artifactService.createArtifact(email, "artifact", "description")
            val user = userRepository.findByEmail(email)
            val group = groupService.createGroup(email, "group 1", "description", memberIDs = listOf(), adminIDs = listOf())
            val updateArtifactRequest = ArtifactRequest(
                name = artifact.name,
                description = artifact.description,
                owners = listOf(),
                groups = listOf(group.id),
                sharedWith = listOf(),
                tags = listOf()
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
                    .jsonPath("$.id").value(`is`(artifact.id.toInt()))
                    .jsonPath("$.name").value(`is`(updateArtifactRequest.name))
                    .jsonPath("$.description").value(`is`(updateArtifactRequest.description))
                    .jsonPath("$.owners").value(containsInAnyOrder(*(updateArtifactRequest.owners!!.map { hasEntry("id", it.toInt()) }.toTypedArray())))
                    .jsonPath("$.groups").value(containsInAnyOrder(*(updateArtifactRequest.groups!!.map { hasEntry("id", it.toInt()) }.toTypedArray())))
                    .jsonPath("$.sharedWith").value(containsInAnyOrder(*(updateArtifactRequest.sharedWith!!.map { hasEntry("id", it.toInt()) }.toTypedArray())))
                    .jsonPath("$.tags").value(`is`(updateArtifactRequest.tags))
                    .returnResult()
                    .responseBody!!
            val response = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateArtifactResponse)
            val updatedArtifact = artifactRepository.findByIdOrNull((response.getValue("id") as Int).toLong())!!
            assertThat(updatedArtifact.groups.map(Group::id), containsInAnyOrder(group.id))
        }

        @Test
        fun `it should share the artifact with the specified users`() {
            val artifact = artifactService.createArtifact(email, "artifact", "description")
            val user = userRepository.findByEmail(email)
            val user2 = userService.createUser("user2", "example2@example.com", "password")
            val updateArtifactRequest = ArtifactRequest(
                name = artifact.name,
                description = artifact.description,
                owners = listOf(),
                groups = listOf(),
                sharedWith = listOf(user2.id),
                tags = listOf()
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
                    .jsonPath("$.id").value(`is`(artifact.id.toInt()))
                    .jsonPath("$.name").value(`is`(updateArtifactRequest.name))
                    .jsonPath("$.description").value(`is`(updateArtifactRequest.description))
                    .jsonPath("$.owners").value(`is`(updateArtifactRequest.owners!!.map(Long::toInt)))
                    .jsonPath("$.groups").value(`is`(updateArtifactRequest.groups!!.map(Long::toInt)))
                    .jsonPath("$.sharedWith").value(containsInAnyOrder(*(updateArtifactRequest.sharedWith!!.map { hasEntry("id", it.toInt()) }.toTypedArray())))
                    .jsonPath("$.tags").value(`is`(updateArtifactRequest.tags))
                    .returnResult()
                    .responseBody!!
            val response = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateArtifactResponse)
            val updatedArtifact = artifactRepository.findByIdOrNull((response.getValue("id") as Int).toLong())!!
            assertThat(updatedArtifact.sharedWith.map(User::id), containsInAnyOrder(user2.id))
        }

        @Test
        fun `it should add the specified tags to the artifact`() {
            val artifact = artifactService.createArtifact(email, "artifact", "description")
            val updateArtifactRequest = ArtifactRequest(
                name = artifact.name,
                description = artifact.description,
                owners = listOf(),
                groups = listOf(),
                sharedWith = listOf(),
                tags = listOf("tag1", "tag2")
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
                    .jsonPath("$.id").value(`is`(artifact.id.toInt()))
                    .jsonPath("$.name").value(`is`(updateArtifactRequest.name))
                    .jsonPath("$.description").value(`is`(updateArtifactRequest.description))
                    .jsonPath("$.owners").value(`is`(updateArtifactRequest.owners!!.map(Long::toInt)))
                    .jsonPath("$.groups").value(`is`(updateArtifactRequest.groups!!.map(Long::toInt)))
                    .jsonPath("$.sharedWith").value(`is`(updateArtifactRequest.sharedWith!!.map(Long::toInt)))
                    .jsonPath("$.tags").value(`is`(updateArtifactRequest.tags))
                    .returnResult()
                    .responseBody!!
            val response = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateArtifactResponse)
            val updatedArtifact = artifactRepository.findByIdOrNull((response.getValue("id") as Int).toLong())!!
            assertThat(updatedArtifact.tags, containsInAnyOrder(*(updateArtifactRequest.tags!!.toTypedArray())))
        }

        @Test
        fun `it should allow the associated albums to be updated`() {
            val album = albumService.createAlbum(email, "Album", "Description", listOf(), listOf(), listOf(), listOf())
            val artifact = artifactService.createArtifact(email, "artifact", "description")
            val updateArtifactRequest = ArtifactRequest(
                name = artifact.name,
                description = artifact.description,
                owners = listOf(),
                groups = listOf(),
                sharedWith = listOf(),
                tags = listOf("tag1", "tag2"),
                albums = listOf(album.id)
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
                .jsonPath("$.id").value(`is`(artifact.id.toInt()))
                .jsonPath("$.name").value(`is`(updateArtifactRequest.name))
                .jsonPath("$.description").value(`is`(updateArtifactRequest.description))
                .jsonPath("$.owners").value(`is`(updateArtifactRequest.owners!!.map(Long::toInt)))
                .jsonPath("$.groups").value(`is`(updateArtifactRequest.groups!!.map(Long::toInt)))
                .jsonPath("$.sharedWith").value(`is`(updateArtifactRequest.sharedWith!!.map(Long::toInt)))
                .jsonPath("$.albums").value(containsInAnyOrder(updateArtifactRequest.albums!!.map { hasEntry("id", it.toInt()) }))
                .jsonPath("$.tags").value(`is`(updateArtifactRequest.tags))
                .returnResult()
                .responseBody!!
            val response = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateArtifactResponse)
            val updatedArtifact = artifactRepository.findByIdOrNull((response.getValue("id") as Int).toLong())!!
            assertThat(updatedArtifact.albums, contains(hasProperty("id", `is`(album.id))))
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

        @Test
        fun `it should remove the artifact from associated albums when delete`() {
            val album = albumService.createAlbum(email, "Album", "Description", listOf(), listOf(), listOf(), listOf())
            val artifactRequest = ArtifactRequest(
                name = "Artifact 1",
                description = "Description",
                owners = listOf(),
                groups = listOf(),
                sharedWith = listOf(),
                albums = listOf(album.id)
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
            val updatedAlbum = albumRepository.findByIdOrNull(album.id)!!
            val artifactId = (returnedArtifact["id"] as Int).toLong()
            assertThat(updatedAlbum.artifacts, not(hasItems(hasProperty("id", `is`(artifactId)))))
        }
    }
}

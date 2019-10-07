package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.collection.IsCollectionWithSize.hasSize
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
@AutoConfigureWebTestClient
class AlbumControllerTest {
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
    lateinit var groupService: GroupService

    @Autowired
    lateinit var albumService: AlbumService

    @Autowired
    lateinit var albumRepository: AlbumRepository

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
    inner class GetAlbum {

        @Test
        fun `it should get all the albums accessible by the user`() {
            val user = userRepository.findByEmail(email)!!
            val albums = listOf("album1", "album2", "album3").map {
                albumService.createAlbum(
                        email = email,
                        name = it,
                        description = "description",
                        ownerIDs = listOf(),
                        groupIDs = listOf(),
                        sharedWithIDs = listOf(),
                        artifactIDs = listOf()
                )
            }
            client.get()
                    .uri("/album")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(hasSize<Album>(3))
                    // .jsonPath("$").value(containsInAnyOrder<Album>(albums.map { hasEntry("id", it.id.toInt()) }))
        }

        @Test
        fun `it should filter the albums by the given group ID`() {
            val usr = userRepository.findByEmail(email)!!
            val grp1 =
                    groupRepository.save(Group(name = "group1", members = mutableListOf(usr), description = ""))
            val grp2 = groupRepository.save(Group(name = "group2", members = mutableListOf(usr), description = ""))
            val albums = mapOf(
                    "album1" to grp1.id,
                    "album2" to grp1.id,
                    "album3" to grp2.id
            ).map {
                albumService.createAlbum(
                        email = email,
                        name = it.key,
                        description = "description",
                        ownerIDs = listOf(),
                        groupIDs = listOf(it.value),
                        sharedWithIDs = listOf(),
                        artifactIDs = listOf()
                )
            }
            client.get()
                    .uri("/album?group=${grp1.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(hasSize<Album>(2))
                    // .jsonPath("$").value(containsInAnyOrder(albums.filter { it.groups.first().id == grp1.id }.map {
                    //    hasEntry(
                    //            "id",
                    //            it.id.toInt()
                    //    )
                    // }))
        }

        @Test
        fun `it should filter the albums by the given owner ID`() {
            val usr = userRepository.findByEmail(email)!!
            userService.createUser("name2", "example2@example.com", "password")
            val albums = listOf(
                    "album1",
                    "album2"
            ).map {
                albumService.createAlbum(
                        email = email,
                        name = it,
                        description = "description",
                        ownerIDs = listOf(),
                        groupIDs = listOf(),
                        sharedWithIDs = listOf(),
                        artifactIDs = listOf()
                )
            }
            albumService.createAlbum(
                    email = "example2@example.com",
                    name = "album3",
                    description = "desc",
                    ownerIDs = listOf(),
                    groupIDs = listOf(),
                    sharedWithIDs = listOf(),
                    artifactIDs = listOf()
            )
            client.get()
                    .uri("/album?owner=${usr.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(hasSize<Artifact>(2))
                    // .jsonPath("$").value(containsInAnyOrder(albums.map { hasEntry("id", it.id.toInt()) }))
        }

        @Test
        fun `it should filter the albums by the given group ID and owner ID`() {
            val usr1 = userRepository.findByEmail(email)!!
            val usr2 = userService.createUser("user2", "exampl2@example.com", "password")
            val grp1 =
                    groupRepository.save(Group(name = "group1", members = mutableListOf(usr1), description = ""))
            val grp2 = groupRepository.save(Group(name = "group2", members = mutableListOf(usr1), description = ""))
            val albums = listOf(
                    albumService.createAlbum(
                            email,
                            "album1",
                            "",
                            ownerIDs = listOf(),
                            groupIDs = listOf(grp1.id),
                            sharedWithIDs = listOf(),
                            artifactIDs = listOf()
                    ),
                    albumService.createAlbum(
                            usr2.email,
                            "album2",
                            "",
                            ownerIDs = listOf(),
                            groupIDs = listOf(grp1.id),
                            sharedWithIDs = listOf(),
                            artifactIDs = listOf()
                    ),
                    albumService.createAlbum(
                            email,
                            "album3",
                            "",
                            ownerIDs = listOf(),
                            groupIDs = listOf(grp2.id),
                            sharedWithIDs = listOf(),
                            artifactIDs = listOf()
                    )
            )
            client.get()
                    .uri("/album?owner=${usr1.id}&group=${grp1.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(hasSize<Artifact>(1))
                    .jsonPath("$")
                    // .value(containsInAnyOrder(albums.filter { it.groups.first().id == grp1.id && it.owners.first().id == usr1.id }.map {
                    //    hasEntry(
                    //            "id",
                    //            it.id.toInt()
                    //    )
                    // }))
        }

        @Test
        fun `it should get the album by ID`() {
            val usr = userRepository.findByEmail(email)!!
            val album = albumService.createAlbum(
                    email = usr.email,
                    name = "album3",
                    description = "desc",
                    ownerIDs = listOf(),
                    groupIDs = listOf(),
                    sharedWithIDs = listOf(),
                    artifactIDs = listOf()
            )
            client.get()
                    .uri("/album/${album.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isNotEmpty
                    .jsonPath("$.id").value(`is`(album.id.toInt()))
                    .jsonPath("$.name").value(`is`(album.name))
                    .jsonPath("$.description").value(`is`(album.description))
                    .jsonPath("$.owners").value(`is`(album.owners.map { it.id.toInt() }))
                    .jsonPath("$.groups").value(`is`(album.groups.map { it.id.toInt() }))
                    .jsonPath("$.sharedWith").value(`is`(album.sharedWith.map { it.id.toInt() }))
                    .jsonPath("$.artifacts").value(`is`(album.artifacts.map { it.id.toInt() }))
        }
    }

    @Nested
    inner class CreateAlbum {
        @Test
        fun `it should create the album`() {
            val user = userRepository.findByEmail(email)!!
            val user2 = userService.createUser("user2", "example2@example.com", "password")
            val group = groupService.createGroup(user.email, "group1", "description", memberIDs = listOf(user2.id), adminIDs = listOf())
            val albumRequest = AlbumRequest(
                    name = "Album 1",
                    description = "Description",
                    owners = listOf(),
                    groups = listOf(group.id),
                    sharedWith = listOf(user2.id),
                    artifacts = listOf()
            )
            val response = client.post()
                    .uri("/album")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(albumRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody()
                    .jsonPath("$.id").value(greaterThan(0))
                    .jsonPath("$.name").value(`is`(albumRequest.name))
                    .jsonPath("$.description").value(`is`(albumRequest.description))
                    .jsonPath("$.owners").value(`is`(listOf(user.id.toInt())))
                    .jsonPath("$.groups").value(containsInAnyOrder(user.privateGroup.id.toInt(), group.id.toInt()))
                    .jsonPath("$.sharedWith").value(`is`(listOf(user2.id.toInt())))
                    .jsonPath("$.artifacts").value(`is`(albumRequest.artifacts))
                    .returnResult()
                    .responseBody!!
            val returnedAlbum = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(response))

            val createdAlbum = albumRepository.findByIdOrNull((returnedAlbum["id"] as Int).toLong())!!
            // Check owners
            // assertEquals(createdAlbum.owners.first().id, user.id)
            assertThat(createdAlbum.owners.map(User::id), contains(`is`(user.id)))

            // Check groups
            assertThat(createdAlbum.groups.map(Group::id), containsInAnyOrder(group.id, user.privateGroup.id))

            // Check shared with
            assertThat(createdAlbum.sharedWith.map(User::id), contains(user2.id))
        }
    }
}

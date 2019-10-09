package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasItems
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
                    .jsonPath("$").value(hasSize<Album>(2))
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
                    .jsonPath("$").value(hasSize<Album>(1))
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

    @Nested
    inner class UpdateAlbum {
        @Test
        fun `it should allow owners to update the album`() {
            val albumRequest = AlbumRequest(
                    name = "Album 1",
                    description = "Description",
                    owners = listOf(),
                    groups = listOf(),
                    sharedWith = listOf(),
                    artifacts = listOf()
            )
            val createAlbumResponse = client.post()
                    .uri("/album")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(albumRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody()
                    .returnResult()
                    .responseBody!!
            val returnedAlbum =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createAlbumResponse))
            @Suppress("UNCHECKED_CAST")
            val updateAlbumRequest =
                    AlbumRequest(
                            name = returnedAlbum["name"] as String,
                            description = returnedAlbum["description"] as String,
                            owners = (returnedAlbum["owners"] as List<Int>).map(Int::toLong),
                            groups = (returnedAlbum["groups"] as List<Int>).map(Int::toLong),
                            sharedWith = (returnedAlbum["sharedWith"] as List<Int>).map(Int::toLong),
                            artifacts = (returnedAlbum["artifacts"] as List<Int>).map(Int::toLong)
                    )
            val updateAlbumResponse = client.put()
                    .uri("/album/${returnedAlbum["id"]}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(updateAlbumRequest)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .returnResult()
                    .responseBody!!
            val updatedAlbumResponse =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateAlbumResponse)
            assertEquals((updatedAlbumResponse["id"] as Int).toLong(), (returnedAlbum["id"] as Int).toLong())
            assertEquals(updatedAlbumResponse["name"] as String, albumRequest.name)
            assertEquals(updatedAlbumResponse["description"] as String, albumRequest.description)

            val updatedAlbum =
                    albumRepository.findByIdOrNull((updatedAlbumResponse["id"] as Int).toLong())!!
            assertEquals(
                    mutableListOf(userRepository.findByEmail(email)!!.id),
                    updatedAlbum.owners.map(User::id)
            )
        }

        @Test
        fun `it should allow group owners to remove the album from the group`() {
            var user = userRepository.findByEmail(email)!!
            var ownedGroup = groupRepository.save(
                    Group(
                            name = "Group 1",
                            description = "description",
                            artifacts = mutableListOf(),
                            admins = mutableListOf(user),
                            members = mutableListOf(user),
                            albums = mutableListOf()
                    )
            )
            userRepository.save(user.copy(ownedGroups = mutableListOf(ownedGroup)))
            val album = albumRepository.save(
                    Album(
                            name = "Album 1",
                            description = "Description",
                            owners = mutableListOf(),
                            groups = mutableListOf(ownedGroup),
                            sharedWith = mutableListOf()
                    )
            )
            ownedGroup = groupRepository.save(ownedGroup.copy(albums = mutableListOf(album)))
            val updateAlbumRequest =
                    AlbumRequest(
                            name = album.name,
                            description = album.description,
                            owners = album.owners.map(User::id),
                            groups = album.groups.map(Group::id).filter { it != ownedGroup.id },
                            sharedWith = album.sharedWith.map(User::id),
                            artifacts = album.artifacts.map(Artifact::id)
                    )
            val updateAlbumResponse = client.put()
                    .uri("/album/${album.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(updateAlbumRequest)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .returnResult()
                    .responseBody!!
            val updatedAlbumResponse =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateAlbumResponse)
            assertEquals((updatedAlbumResponse["id"] as Int).toLong(), album.id)
            assertEquals(updatedAlbumResponse["name"] as String, album.name)
            assertEquals(updatedAlbumResponse["description"] as String, album.description)

            val updatedAlbum =
                    albumRepository.findByIdOrNull((updatedAlbumResponse["id"] as Int).toLong())!!
            assertThat(updatedAlbum, hasProperty("groups", not(hasItem(hasProperty("id", `is`(ownedGroup.id))))))
        }

        @Test
        fun `it should not allow normal users to update the album`() {
            val albumRequest = AlbumRequest(
                    name = "Album 1",
                    description = "Description",
                    owners = listOf(),
                    groups = listOf(),
                    sharedWith = listOf(),
                    artifacts = listOf()
            )

            val createAlbumResponse = client.post()
                    .uri("/album")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(albumRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody()
                    .returnResult()
                    .responseBody!!
            val returnedAlbum =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createAlbumResponse))

            @Suppress("UNCHECKED_CAST")
            val updateAlbumRequest =
                    AlbumRequest(
                            name = returnedAlbum["name"] as String,
                            description = returnedAlbum["description"] as String,
                            owners = (returnedAlbum["owners"] as List<Int>).map(Int::toLong),
                            groups = (returnedAlbum["groups"] as List<Int>).map(Int::toLong),
                            sharedWith = (returnedAlbum["owners"] as List<Int>).map(Int::toLong),
                            artifacts = (returnedAlbum["artifacts"] as List<Int>).map(Int::toLong)
                    )
            val altUser = userService.createUser("user 2", "exampl2@example.com", "password")
            val altToken = getToken(altUser.email, "password")
            client.put()
                    .uri("/album/${returnedAlbum["id"]}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $altToken")
                    .syncBody(updateAlbumRequest)
                    .exchange()
                    .expectStatus().isForbidden
        }

        @Test
        fun `it should add the specified users as owners`() {
            val album = albumService.createAlbum(email, "album", "description", mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
            val user = userRepository.findByEmail(email)!!
            val user2 = userService.createUser("user2", "example2@example.com", "password")
            val updateAlbumRequest = AlbumRequest(
                    name = album.name,
                    description = album.description,
                    owners = album.owners.map(User::id) + listOf(user2.id),
                    groups = listOf(),
                    sharedWith = listOf(),
                    artifacts = listOf()
            )
            val updateAlbumResponse = client.put()
                    .uri("/album/${album.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(updateAlbumRequest)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.id").value(`is`(album.id.toInt()))
                    .jsonPath("$.name").value(`is`(updateAlbumRequest.name))
                    .jsonPath("$.description").value(`is`(updateAlbumRequest.description))
                    .jsonPath("$.owners").value(`is`(updateAlbumRequest.owners!!.map(Long::toInt)))
                    .jsonPath("$.groups").value(`is`(updateAlbumRequest.groups!!.map(Long::toInt)))
                    .jsonPath("$.sharedWith").value(`is`(updateAlbumRequest.sharedWith!!.map(Long::toInt)))
                    .jsonPath("$.artifacts").value(`is`(updateAlbumRequest.artifacts!!.map(Long::toInt)))
                    .returnResult()
                    .responseBody!!
            val response = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateAlbumResponse)
            val updatedAlbum = albumRepository.findByIdOrNull((response.getValue("id") as Int).toLong())!!
            val updatedUser = userRepository.findByIdOrNull(user.id)!!
            val updatedUser2 = userRepository.findByIdOrNull(user2.id)!!
            assertThat(updatedAlbum.owners.map(User::id), containsInAnyOrder(user.id, user2.id))
        }

        @Test
        fun `it should add the album to the specified groups`() {
            val album = albumService.createAlbum(email, "album", "description", mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
            val user = userRepository.findByEmail(email)
            val group = groupService.createGroup(email, "group 1", "description", memberIDs = listOf(), adminIDs = listOf())
            val updateAlbumRequest = AlbumRequest(
                    name = album.name,
                    description = album.description,
                    owners = listOf(),
                    groups = listOf(group.id),
                    sharedWith = listOf(),
                    artifacts = listOf()
            )
            val updateAlbumResponse = client.put()
                    .uri("/album/${album.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(updateAlbumRequest)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.id").value(`is`(album.id.toInt()))
                    .jsonPath("$.name").value(`is`(updateAlbumRequest.name))
                    .jsonPath("$.description").value(`is`(updateAlbumRequest.description))
                    .jsonPath("$.owners").value(`is`(updateAlbumRequest.owners!!.map(Long::toInt)))
                    .jsonPath("$.groups").value(`is`(updateAlbumRequest.groups!!.map(Long::toInt)))
                    .jsonPath("$.sharedWith").value(`is`(updateAlbumRequest.sharedWith!!.map(Long::toInt)))
                    .jsonPath("$.artifacts").value(`is`(updateAlbumRequest.artifacts!!.map(Long::toInt)))
                    .returnResult()
                    .responseBody!!
            val response = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateAlbumResponse)
            val updatedAlbum = albumRepository.findByIdOrNull((response.getValue("id") as Int).toLong())!!
            assertThat(updatedAlbum.groups.map(Group::id), containsInAnyOrder(group.id))
        }

        @Test
        fun `it should share the album with the specified users`() {
            val album = albumService.createAlbum(email, "album", "description", mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
            val user = userRepository.findByEmail(email)
            val user2 = userService.createUser("user2", "example2@example.com", "password")
            val updateAlbumRequest = AlbumRequest(
                    name = album.name,
                    description = album.description,
                    owners = listOf(),
                    groups = listOf(),
                    sharedWith = listOf(user2.id),
                    artifacts = listOf()
            )
            val updateAlbumResponse = client.put()
                    .uri("/album/${album.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(updateAlbumRequest)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.id").value(`is`(album.id.toInt()))
                    .jsonPath("$.name").value(`is`(updateAlbumRequest.name))
                    .jsonPath("$.description").value(`is`(updateAlbumRequest.description))
                    .jsonPath("$.owners").value(`is`(updateAlbumRequest.owners!!.map(Long::toInt)))
                    .jsonPath("$.groups").value(`is`(updateAlbumRequest.groups!!.map(Long::toInt)))
                    .jsonPath("$.sharedWith").value(`is`(updateAlbumRequest.sharedWith!!.map(Long::toInt)))
                    .jsonPath("$.artifacts").value(`is`(updateAlbumRequest.artifacts!!.map(Long::toInt)))
                    .returnResult()
                    .responseBody!!
            val response = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateAlbumResponse)
            val updatedAlbum = albumRepository.findByIdOrNull((response.getValue("id") as Int).toLong())!!
            assertThat(updatedAlbum.sharedWith.map(User::id), containsInAnyOrder(user2.id))
        }
    }

    @Nested
    inner class DeleteAlbum {
        @Test
        fun `it should allow owners to delete the album`() {
            val albumRequest = AlbumRequest(
                    name = "Album 1",
                    description = "Description",
                    owners = listOf(),
                    groups = listOf(),
                    sharedWith = listOf(),
                    artifacts = listOf()
            )
            val createAlbumResponse = client.post()
                    .uri("/album")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(albumRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody()
                    .returnResult()
                    .responseBody!!
            val returnedAlbum =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createAlbumResponse))
            client.delete()
                    .uri("/album/${returnedAlbum["id"]}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .returnResult()
                    .responseBody!!
            assertFalse(albumRepository.existsById((returnedAlbum["id"] as Int).toLong()))
            val user = userRepository.findByEmail(email)!!
            val albumId = (returnedAlbum["id"] as Int).toLong()
            assertThat(user.ownedAlbums, not(hasItems(hasProperty("id", `is`(albumId)))))
        }

        @Test
        fun `it should not allow normal users to delete the album`() {
            val albumRequest = AlbumRequest(
                    name = "Album 1",
                    description = "Description",
                    owners = listOf(),
                    groups = listOf(),
                    sharedWith = listOf(),
                    artifacts = listOf()
            )

            val createAlbumResponse = client.post()
                    .uri("/album")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(albumRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody()
                    .returnResult()
                    .responseBody!!
            val returnedAlbum =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createAlbumResponse))

            userService.createUser("user 2", "exampl2@example.com", "password")
            val altToken = getToken("exampl2@example.com", "password")
            client.delete()
                    .uri("/album/${returnedAlbum["id"]}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $altToken")
                    .exchange()
                    .expectStatus().isForbidden
        }
    }
}

package com.synergeticsolutions.familyartefacts

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.beans.HasPropertyWithValue
import org.hamcrest.core.IsCollectionContaining
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.data.repository.findByIdOrNull
import java.util.*

class AlbumServiceImplTest {
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val groupRepository: GroupRepository = Mockito.mock(GroupRepository::class.java)
    private val artifactRepository: ArtifactRepository = Mockito.mock(ArtifactRepository::class.java)
    private val albumRepository: AlbumRepository = Mockito.mock(AlbumRepository::class.java)
    private val albumService: AlbumService = AlbumServiceImpl(userRepository, groupRepository, artifactRepository, albumRepository)

    @Nested
    inner class FindAlbumsByOwner {

        @Test
        fun `it should find all the albums accessible by the user`() {
            val email = "example@example.com"
            val groupAlbums = listOf(
                    Album(1, "Album 1", "Description", owners = mutableListOf(), groups = mutableListOf()),
                    Album(2, "Album 2", "Description", owners = mutableListOf(), groups = mutableListOf())
            )
            val ownedAlbums = listOf(
                    Album(3, "Album 3", "Description", owners = mutableListOf(), groups = mutableListOf()),
                    Album(4, "Album 4", "Description", owners = mutableListOf(), groups = mutableListOf())
            )
            val sharedAlbums = listOf(
                    Album(5, "Album 5", "Description", owners = mutableListOf(), groups = mutableListOf()),
                    Album(6, "Album 6", "Description", owners = mutableListOf(), groups = mutableListOf())
            )
            Mockito.`when`(userRepository.findByEmail(email))
                    .thenReturn(
                            User(
                                    id = 1, name = "User 1", email = email, password = "password", groups = mutableListOf(
                                    Group(id = 1, name = "Group 1", members = mutableListOf(), description = "")
                            ), privateGroup = Group(2, "Group 2", members = mutableListOf(), description = "")
                            )
                    )
            Mockito.`when`(albumRepository.findByGroups_Id(ArgumentMatchers.anyLong())).thenReturn(groupAlbums)
            Mockito.`when`(albumRepository.findByOwners_Email(ArgumentMatchers.anyString())).thenReturn(ownedAlbums)
            Mockito.`when`(albumRepository.findBySharedWith_Email(ArgumentMatchers.anyString())).thenReturn(sharedAlbums)

            val foundAlbums = albumService.findAlbumsByOwner(email, groupID = null, sharedID = null, ownerID = null)
            val allAlbums = ownedAlbums + groupAlbums + sharedAlbums
            Assertions.assertEquals(allAlbums.size, foundAlbums.size)
            MatcherAssert.assertThat(foundAlbums, Matchers.containsInAnyOrder(*allAlbums.toTypedArray()))
        }

        @Test
        fun `it should filter the accessible albums by the group ID if specified`() {
            val email = "example@example.com"
            val group = Group(id = 1, name = "Group 1", members = mutableListOf(), description = "")
            val group2 = Group(id = 2, name = "Group 2", members = mutableListOf(), description = "")
            val user = User(
                    id = 1, name = "User 1", email = email, password = "password",
                    groups = mutableListOf(group),
                    privateGroup = Group(3, "Group 3", members = mutableListOf(), description = "")
            )
            val groupAlbums = listOf(
                    Album(1, "Album 1", "Description", owners = mutableListOf(), groups = mutableListOf(group), sharedWith = mutableListOf(), artifacts = mutableListOf()),
                    Album(2, "Album 2", "Description", owners = mutableListOf(), groups = mutableListOf(group), sharedWith = mutableListOf(), artifacts = mutableListOf())
            )
            val ownedAlbums = listOf(
                    Album(3, "Album 3", "Description", owners = mutableListOf(user), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf()),
                    Album(4, "Album 4", "Description", owners = mutableListOf(user), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf())
            )
            val sharedAlbums = listOf(
                    Album(5, "Album 5", "Description", owners = mutableListOf(), groups = mutableListOf(group2), sharedWith = mutableListOf(), artifacts = mutableListOf()),
                    Album(6, "Album 6", "Description", owners = mutableListOf(), groups = mutableListOf(group2), sharedWith = mutableListOf(), artifacts = mutableListOf())
            )
            Mockito.`when`(userRepository.findByEmail(email))
                    .thenReturn(user)
            Mockito.`when`(albumRepository.findByGroups_Id(ArgumentMatchers.anyLong())).thenReturn(groupAlbums)
            Mockito.`when`(albumRepository.findByOwners_Email(ArgumentMatchers.anyString())).thenReturn(ownedAlbums)
            Mockito.`when`(albumRepository.findBySharedWith_Email(ArgumentMatchers.anyString())).thenReturn(sharedAlbums)

            val foundAlbums = albumService.findAlbumsByOwner(email, groupID = 1, sharedID = null, ownerID = null)
            val expectedAlbums =
                    (groupAlbums + ownedAlbums + sharedAlbums).filter { it.groups.firstOrNull()?.id == (1).toLong() }
            Assertions.assertEquals(expectedAlbums.size, foundAlbums.size)
            MatcherAssert.assertThat(foundAlbums, Matchers.containsInAnyOrder(*expectedAlbums.toTypedArray()))
        }

        @Test
        fun `it should filter the accessible albums by the owner ID if specified`() {
            val email = "example@example.com"
            val user = User(
                    id = 1, name = "User 1", email = email, password = "password", groups = mutableListOf(
                    Group(id = 1, name = "Group 1", members = mutableListOf(), description = "")
            ), privateGroup = Group(2, "Group 2", members = mutableListOf(), description = "")
            )
            val groupAlbums = listOf(
                    Album(1, "Album 1", "Description", owners = mutableListOf(), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf()),
                    Album(2, "Album 2", "Description", owners = mutableListOf(), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf())
            )
            val ownedAlbums = listOf(
                    Album(3, "Album 3", "Description", owners = mutableListOf(user), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf()),
                    Album(4, "Album 4", "Description", owners = mutableListOf(user), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf())
            )
            val sharedAlbums = listOf(
                    Album(5, "Album 5", "Description", owners = mutableListOf(), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf()),
                    Album(6, "Album 6", "Description", owners = mutableListOf(), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf())
            )
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(albumRepository.findByGroups_Id(ArgumentMatchers.anyLong())).thenReturn(groupAlbums)
            Mockito.`when`(albumRepository.findByOwners_Email(ArgumentMatchers.anyString())).thenReturn(ownedAlbums)
            Mockito.`when`(albumRepository.findBySharedWith_Email(ArgumentMatchers.anyString())).thenReturn(sharedAlbums)

            val foundAlbums = albumService.findAlbumsByOwner(email, groupID = null, sharedID = null, ownerID = user.id)
            val expectedAlbums =
                    (groupAlbums + ownedAlbums + sharedAlbums).filter { it.owners.firstOrNull()?.id == user.id }
            Assertions.assertEquals(expectedAlbums.size, foundAlbums.size)
            MatcherAssert.assertThat(foundAlbums, Matchers.containsInAnyOrder(*expectedAlbums.toTypedArray()))
        }

        @Test
        fun `it should filter the accessible albums by the shared ID if specified`() {
            val email = "example@example.com"
            val user = User(
                    id = 1, name = "User 1", email = email, password = "password", groups = mutableListOf(
                    Group(id = 1, name = "Group 1", members = mutableListOf(), description = "")
            ), privateGroup = Group(2, "Group 2", members = mutableListOf(), description = "")
            )
            val groupAlbums = listOf(
                    Album(1, "Album 1", "Description", owners = mutableListOf(), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf()),
                    Album(2, "Album 2", "Description", owners = mutableListOf(), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf())
            )
            val ownedAlbums = listOf(
                    Album(3, "Album 3", "Description", owners = mutableListOf(), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf()),
                    Album(4, "Album 4", "Description", owners = mutableListOf(), groups = mutableListOf(), sharedWith = mutableListOf(), artifacts = mutableListOf())
            )
            val sharedAlbums = listOf(
                    Album(5, "Album 5", "Description", owners = mutableListOf(), groups = mutableListOf(), sharedWith = mutableListOf(user), artifacts = mutableListOf()),
                    Album(6, "Album 6", "Description", owners = mutableListOf(), groups = mutableListOf(), sharedWith = mutableListOf(user), artifacts = mutableListOf())
            )
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(albumRepository.findByGroups_Id(ArgumentMatchers.anyLong())).thenReturn(groupAlbums)
            Mockito.`when`(albumRepository.findByOwners_Email(ArgumentMatchers.anyString())).thenReturn(ownedAlbums)
            Mockito.`when`(albumRepository.findBySharedWith_Email(ArgumentMatchers.anyString())).thenReturn(sharedAlbums)

            val foundAlbums = albumService.findAlbumsByOwner(email, groupID = null, sharedID = user.id, ownerID = null)
            val expectedAlbums =
                    (groupAlbums + ownedAlbums + sharedAlbums).filter { it.sharedWith.firstOrNull()?.id == user.id }
            Assertions.assertEquals(expectedAlbums.size, foundAlbums.size)
            MatcherAssert.assertThat(foundAlbums, Matchers.containsInAnyOrder(*expectedAlbums.toTypedArray()))
        }

        @Test
        fun `it should not return duplicate albums`() {
            val email = "example@example.com"
            val user = User(
                    id = 1, name = "User 1", email = email, password = "password", groups = mutableListOf(
                    Group(id = 1, name = "Group 1", members = mutableListOf(), description = "")
            ), privateGroup = Group(2, "Group 2", members = mutableListOf(), description = "")
            )
            val groupAlbums = listOf(
                    Album(1, "Album 1", "Description", owners = mutableListOf(), groups = mutableListOf()),
                    Album(2, "Album 2", "Description", owners = mutableListOf(), groups = mutableListOf())
            )
            val ownedAlbums = listOf(
                    Album(3, "Album 3", "Description", owners = mutableListOf(), groups = mutableListOf()),
                    Album(4, "Album 4", "Description", owners = mutableListOf(), groups = mutableListOf())
            )
            val sharedAlbums = listOf(
                    Album(5, "Album 5", "Description", owners = mutableListOf(), groups = mutableListOf()),
                    Album(6, "Album 6", "Description", owners = mutableListOf(), groups = mutableListOf())
            )
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(albumRepository.findByGroups_Id(ArgumentMatchers.anyLong())).thenReturn(groupAlbums)
            Mockito.`when`(albumRepository.findByOwners_Email(ArgumentMatchers.anyString())).thenReturn(ownedAlbums)
            Mockito.`when`(albumRepository.findBySharedWith_Email(ArgumentMatchers.anyString())).thenReturn(sharedAlbums)

            val foundAlbums = albumService.findAlbumsByOwner(email, groupID = null, sharedID = null, ownerID = null)
            val expectedAlbums = (groupAlbums + ownedAlbums + sharedAlbums).toSet().toList()
            Assertions.assertEquals(expectedAlbums.size, foundAlbums.size)
            MatcherAssert.assertThat(foundAlbums, Matchers.containsInAnyOrder(*expectedAlbums.toTypedArray()))
        }
    }

    @Nested
    inner class CreateAlbum {
        @Test
        fun `it should include the creator as one of the album owners`() {
        Mockito.`when`(albumRepository.save(ArgumentMatchers.any<Album>())).then { it.arguments[0] as Album }
        val user = User(
                1,
                "User1",
                "example@example.com",
                "password",
                privateGroup = Group(1, "Group 1", members = mutableListOf(), description = "")
        )
        val album = Album(
                id = 1,
                name = "Album 1",
                description = "Album description",
                owners = mutableListOf(user),
                groups = mutableListOf(),
                sharedWith = mutableListOf(),
                artifacts = mutableListOf()
        )
        Mockito.`when`(userRepository.findByEmail(ArgumentMatchers.anyString())).thenReturn(user)
        Mockito.`when`(userRepository.findByIdOrNull(ArgumentMatchers.anyLong())).then { Optional.empty<User>() }
        Mockito.`when`(albumRepository.findById(ArgumentMatchers.anyLong())).thenReturn(Optional.of(album))
        albumService.createAlbum(
                "example@example.com",
                "Album 1",
                description = "Album description",
                ownerIDs = listOf(),
                groupIDs = listOf(),
                sharedWithIDs = listOf(),
                artifactIDs = listOf()
        )
        val argCapturer = ArgumentCaptor.forClass(Album::class.java)
        Mockito.verify(albumRepository).save(argCapturer.capture())
        val matcher = HasPropertyWithValue.hasProperty<Album>("owners", Matchers.contains(HasPropertyWithValue.hasProperty<User>("id", Matchers.equalTo(1L))))
        MatcherAssert.assertThat(argCapturer.value, matcher)
    }

        @Test
        fun `it should include the user's personal group as one of the album's associated groups`() {
            Mockito.`when`(albumRepository.save(ArgumentMatchers.any<Album>())).then { it.arguments[0] as Album }
            Mockito.`when`(userRepository.findByEmail(ArgumentMatchers.anyString()))
                    .thenReturn(
                            User(
                                    1,
                                    "User1",
                                    "example@example.com",
                                    "password",
                                    privateGroup = Group(2, "Group 1", members = mutableListOf(), description = "")
                            )
                    )
            Mockito.`when`(userRepository.findByIdOrNull(ArgumentMatchers.anyLong())).thenReturn(null)
            albumService.createAlbum(
                    "example@example.com",
                    "Album 1",
                    description = "Album description",
                    ownerIDs = listOf(),
                    groupIDs = listOf(),
                    sharedWithIDs = listOf(),
                    artifactIDs = listOf()
            )
            val argumentCaptor = ArgumentCaptor.forClass(Album::class.java)
            Mockito.verify(albumRepository).save(argumentCaptor.capture())
            val matcher = HasPropertyWithValue.hasProperty<Album>("groups", Matchers.contains(HasPropertyWithValue.hasProperty<Group>("id", Matchers.equalTo(2L))))
            MatcherAssert.assertThat(argumentCaptor.value, matcher)
        }

        @Test
        fun `it should make the specified owner IDs the owners of the album`() {
            Mockito.`when`(groupRepository.save(ArgumentMatchers.any<Group>())).then { it.arguments[0] as Group }
            Mockito.`when`(albumRepository.save(ArgumentMatchers.any<Album>())).then { it.arguments[0] as Album }
            Mockito.`when`(userRepository.existsById(ArgumentMatchers.anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(ArgumentMatchers.anyString()))
                    .thenReturn(
                            User(
                                    1,
                                    "User1",
                                    "example@example.com",
                                    "password",
                                    privateGroup = Group(2, "Group 1", members = mutableListOf(), description = "")
                            )
                    )
            Mockito.`when`(userRepository.findByIdOrNull(ArgumentMatchers.anyLong())).then {
                User(
                        it.arguments[0] as Long,
                        "User ${it.arguments[0]}",
                        "example${it.arguments[0]}@email.com",
                        "password",
                        privateGroup = Group(2, "Group 1", members = mutableListOf(), description = "")
                )
            }
            Mockito.`when`(userRepository.findAllById(ArgumentMatchers.any<Iterable<Long>>())).then {
                (it.arguments[0] as Iterable<Long>).map { id ->
                    User(
                            id = id,
                            name = "User $id",
                            email = "example$id@example.com",
                            password = "password",
                            privateGroup = Group(
                                    1, "Group1", members = mutableListOf(), description = ""
                            )
                    )
                }
            }
            albumService.createAlbum(
                    "example@example.com",
                    "Album 1",
                    description = "Album description",
                    ownerIDs = listOf(2, 3),
                    groupIDs = listOf(),
                    sharedWithIDs = listOf(),
                    artifactIDs = listOf()
            )
            val argCapturer = ArgumentCaptor.forClass(Album::class.java)
            Mockito.verify(albumRepository).save(argCapturer.capture())
            val matcher =
                    HasPropertyWithValue.hasProperty<Album>(
                            "owners",
                            IsCollectionContaining.hasItems<User>(
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(2L)),
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(3L))
                            )
                    )
            MatcherAssert.assertThat(argCapturer.value, matcher)
        }

        @Test
        fun `it should make the specified group IDs associated with the album`() {
            Mockito.`when`(albumRepository.save(ArgumentMatchers.any<Album>())).then { it.arguments[0] as Album }
            Mockito.`when`(groupRepository.existsById(ArgumentMatchers.anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(ArgumentMatchers.anyString()))
                    .thenReturn(
                            User(
                                    1,
                                    "User1",
                                    "example@example.com",
                                    "password",
                                    privateGroup = Group(2, "Group 1", members = mutableListOf(), description = "")
                            )
                    )
            Mockito.`when`(userRepository.findByIdOrNull(ArgumentMatchers.anyLong())).then {
                User(
                        it.arguments[0] as Long,
                        "User ${it.arguments[0]}",
                        "example${it.arguments[0]}@email.com",
                        "password",
                        privateGroup = Group(2, "Group 1", members = mutableListOf(), description = "")
                )
            }
            Mockito.`when`(groupRepository.findAllById(ArgumentMatchers.any<Iterable<Long>>())).then {
                (it.arguments[0] as Iterable<Long>).map { id ->
                    Group(
                            id = id,
                            name = "Group $id",
                            members = mutableListOf(), description = ""
                    )
                }
            }
            albumService.createAlbum(
                    "example@example.com",
                    "Album 1",
                    description = "Album description",
                    ownerIDs = listOf(),
                    groupIDs = listOf(2, 3),
                    sharedWithIDs = listOf(),
                    artifactIDs = listOf()
            )
            val argCapturer = ArgumentCaptor.forClass(Album::class.java)
            Mockito.verify(albumRepository).save(argCapturer.capture())
            val matcher =
                    HasPropertyWithValue.hasProperty<Album>(
                            "groups",
                            IsCollectionContaining.hasItems<Group>(
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(2L)),
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(3L))
                            )
                    )
            MatcherAssert.assertThat(argCapturer.value, matcher)
        }

        @Test
        fun `it should share the album with the specified user IDs`() {
            Mockito.`when`(albumRepository.save(ArgumentMatchers.any<Album>())).then { it.arguments[0] as Album }
            Mockito.`when`(userRepository.existsById(ArgumentMatchers.anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(ArgumentMatchers.anyString()))
                    .thenReturn(
                            User(
                                    1,
                                    "User1",
                                    "example@example.com",
                                    "password",
                                    privateGroup = Group(2, "Group 1", members = mutableListOf(), description = "")
                            )
                    )
            Mockito.`when`(userRepository.findByIdOrNull(ArgumentMatchers.anyLong())).then {
                User(
                        it.arguments[0] as Long,
                        "User ${it.arguments[0]}",
                        "example${it.arguments[0]}@email.com",
                        "password",
                        privateGroup = Group(2, "Group 1", members = mutableListOf(), description = "")
                )
            }
            Mockito.`when`(userRepository.findAllById(ArgumentMatchers.any<Iterable<Long>>())).then {
                (it.arguments[0] as Iterable<Long>).map { id ->
                    User(
                            id = id,
                            name = "Artifact $id",
                            email = "example$id@example.com",
                            password = "password",
                            privateGroup = Group(
                                    1, "Group1", members = mutableListOf(), description = ""
                            )
                    )
                }
            }
            albumService.createAlbum(
                    "example@example.com",
                    "Album 1",
                    description = "Album description",
                    ownerIDs = listOf(),
                    groupIDs = listOf(),
                    sharedWithIDs = listOf(2, 3),
                    artifactIDs = listOf()
            )
            val argCapturer = ArgumentCaptor.forClass(Album::class.java)
            Mockito.verify(albumRepository).save(argCapturer.capture())
            val matcher =
                    HasPropertyWithValue.hasProperty<Album>(
                            "sharedWith",
                            IsCollectionContaining.hasItems<User>(
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(2L)),
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(3L))
                            )
                    )
            MatcherAssert.assertThat(argCapturer.value, matcher)
        }

        @Test
        fun `it should not double up if the creator's ID is specified in the owners`() {
            Mockito.`when`(albumRepository.save(ArgumentMatchers.any<Album>())).then { it.arguments[0] as Album }
            Mockito.`when`(userRepository.existsById(ArgumentMatchers.anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(ArgumentMatchers.anyString()))
                    .thenReturn(
                            User(
                                    1,
                                    "User 1",
                                    "example1@example.com",
                                    "password",
                                    privateGroup = Group(2, "Group 1", members = mutableListOf(), description = "")
                            )
                    )
            Mockito.`when`(userRepository.findByIdOrNull(ArgumentMatchers.anyLong())).then {
                User(
                        it.arguments[0] as Long,
                        "User ${it.arguments[0]}",
                        "example${it.arguments[0]}@email.com",
                        "password",
                        privateGroup = Group(2, "Group 1", members = mutableListOf(), description = "")
                )
            }
            Mockito.`when`(userRepository.findAllById(ArgumentMatchers.any<Iterable<Long>>())).then {
                (it.arguments[0] as Iterable<Long>).map { id ->
                    User(
                            id = id,
                            name = "User $id",
                            email = "example$id@example.com",
                            password = "password",
                            privateGroup = Group(
                                    2, "Group 1", members = mutableListOf(), description = ""
                            )
                    )
                }
            }
            albumService.createAlbum(
                    "example@example.com",
                    "Album 1",
                    description = "Album description",
                    ownerIDs = listOf(1, 2, 3),
                    groupIDs = listOf(),
                    sharedWithIDs = listOf(),
                    artifactIDs = listOf()
            )
            val argCapturer = ArgumentCaptor.forClass(Album::class.java)
            Mockito.verify(albumRepository).save(argCapturer.capture())
            val matcher =
                    HasPropertyWithValue.hasProperty<Album>(
                            "owners",
                            Matchers.containsInAnyOrder<User>(
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(1L)),
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(2L)),
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(3L))
                            )
                    )
            MatcherAssert.assertThat(argCapturer.value, matcher)
        }

        @Test
        fun `it should not double up if the creator's personal group is specified in the associated groups`() {
            Mockito.`when`(albumRepository.save(ArgumentMatchers.any<Album>())).then { it.arguments[0] as Album }
            Mockito.`when`(groupRepository.existsById(ArgumentMatchers.anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(ArgumentMatchers.anyString()))
                    .thenReturn(
                            User(
                                    1,
                                    "User1",
                                    "example@example.com",
                                    "password",
                                    privateGroup = Group(2, "Group 2", members = mutableListOf(), description = "")
                            )
                    )
            Mockito.`when`(userRepository.findByIdOrNull(ArgumentMatchers.anyLong())).then {
                User(
                        it.arguments[0] as Long,
                        "User ${it.arguments[0]}",
                        "example${it.arguments[0]}@email.com",
                        "password",
                        privateGroup = Group(
                                2, "Group 1", members = mutableListOf(), description = ""
                        )
                )
            }
            Mockito.`when`(groupRepository.findAllById(ArgumentMatchers.any<Iterable<Long>>())).then {
                (it.arguments[0] as Iterable<Long>).map { id ->
                    Group(
                            id = id,
                            name = "Group $id",
                            members = mutableListOf(), description = ""
                    )
                }
            }
            albumService.createAlbum(
                    "example@example.com",
                    "Album 1",
                    description = "Album description",
                    ownerIDs = listOf(),
                    groupIDs = listOf(1, 2, 3),
                    sharedWithIDs = listOf(),
                    artifactIDs = listOf()
            )
            val argCapturer = ArgumentCaptor.forClass(Album::class.java)
            Mockito.verify(albumRepository).save(argCapturer.capture())
            val matcher =
                    HasPropertyWithValue.hasProperty<Album>(
                            "groups",
                            Matchers.containsInAnyOrder<Group>(
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(1L)),
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(2L)),
                                    HasPropertyWithValue.hasProperty("id", Matchers.equalTo(3L))
                            )
                    )
            MatcherAssert.assertThat(argCapturer.value, matcher)
        }
    }

}

package com.synergeticsolutions.familyartefacts

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.beans.HasPropertyWithValue
import org.hamcrest.core.IsCollectionContaining
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.data.repository.findByIdOrNull

class AlbumServiceImplTest {
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val groupRepository: GroupRepository = Mockito.mock(GroupRepository::class.java)
    private val artifactRepository: ArtifactRepository = Mockito.mock(ArtifactRepository::class.java)
    private val albumRepository: AlbumRepository = Mockito.mock(AlbumRepository::class.java)
    private val albumService: AlbumService =
            AlbumServiceImpl(userRepository, groupRepository, artifactRepository, albumRepository)

    @Test
    fun `it should include the creator as one of the album owners`() {
        Mockito.`when`(albumRepository.save(ArgumentMatchers.any<Album>())).then { it.arguments[0] as Album }
        Mockito.`when`(userRepository.findByEmail(ArgumentMatchers.anyString()))
                .thenReturn(
                        User(
                                1,
                                "User1",
                                "example@example.com",
                                "password",
                                privateGroup = Group(1, "Group 1", members = mutableListOf(), description = "")
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

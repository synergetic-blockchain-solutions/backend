package com.synergeticsolutions.familyartefacts

import com.synergeticsolutions.familyartefacts.dtos.ArtifactRequest
import com.synergeticsolutions.familyartefacts.entities.Album
import com.synergeticsolutions.familyartefacts.entities.Artifact
import com.synergeticsolutions.familyartefacts.entities.Group
import com.synergeticsolutions.familyartefacts.entities.User
import com.synergeticsolutions.familyartefacts.exceptions.ActionNotAllowedException
import com.synergeticsolutions.familyartefacts.exceptions.GroupNotFoundException
import com.synergeticsolutions.familyartefacts.exceptions.UserNotFoundException
import com.synergeticsolutions.familyartefacts.repositories.AlbumRepository
import com.synergeticsolutions.familyartefacts.repositories.ArtifactRepository
import com.synergeticsolutions.familyartefacts.repositories.ArtifactResourceRepository
import com.synergeticsolutions.familyartefacts.repositories.GroupRepository
import com.synergeticsolutions.familyartefacts.repositories.UserRepository
import com.synergeticsolutions.familyartefacts.services.ArtifactService
import com.synergeticsolutions.familyartefacts.services.ArtifactServiceImpl
import com.synergeticsolutions.familyartefacts.services.UserService
import java.util.Optional
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.beans.HasPropertyWithValue.hasProperty
import org.hamcrest.core.IsCollectionContaining.hasItems
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.junit.jupiter.SpringExtension

@Suppress("UNCHECKED_CAST")
class ArtifactServiceImplTest {
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val groupRepository: GroupRepository = Mockito.mock(GroupRepository::class.java)
    private val artifactRepository: ArtifactRepository = Mockito.mock(
        ArtifactRepository::class.java)
    private val artifactResourceRepository: ArtifactResourceRepository =
        Mockito.mock(ArtifactResourceRepository::class.java)
    private val albumRepository: AlbumRepository = Mockito.mock(AlbumRepository::class.java)

    private val artifactService: ArtifactService =
        ArtifactServiceImpl(
            artifactRepository,
            userRepository,
            groupRepository,
            artifactResourceRepository,
            albumRepository
        )

    @Nested
    inner class CreateArtifact {

        @Test
        fun `it should not create the artifact if the creating user's email are not actual users`() {
            Mockito.`when`(userRepository.findByEmail(anyString())).thenReturn(null)
            assertThrows<UserNotFoundException> {
                artifactService.createArtifact(
                    "example@example.com",
                    "Artifact 1",
                    description = "Artifact description"
                )
            }
        }

        @Test
        fun `it should not create the artifact if one of the owning users' IDs are not in the database`() {
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(
                            1,
                            "Group1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).thenReturn(null)
            assertThrows<UserNotFoundException> {
                artifactService.createArtifact(
                    "example@example.com",
                    "Artifact 1",
                    description = "Artifact description",
                    ownerIDs = listOf(2)
                )
            }
        }

        @Test
        fun `it should not create the artifact if one of the associated group IDs do not exist`() {
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(
                            1,
                            "Group1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong())).thenReturn(null)
            assertThrows<GroupNotFoundException> {
                artifactService.createArtifact(
                    "example@example.com",
                    "Artifact 1",
                    description = "Artifact description",
                    groupIDs = listOf(2)
                )
            }
        }

        @Test
        fun `it should not create the artifact if one of the shared user's IDs do not exist`() {
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(
                            1, "Group 1", members = mutableListOf(), description = ""
                        )
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).thenReturn(null)
            assertThrows<UserNotFoundException> {
                artifactService.createArtifact(
                    "example@example.com",
                    "Artifact 1",
                    description = "Artifact description",
                    sharedWith = listOf(2)
                )
            }
        }

        @Test
        fun `it should include the creator as one of the artifact owners`() {
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(
                            1,
                            "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).thenReturn(null)
            artifactService.createArtifact(
                "example@example.com",
                "Artifact 1",
                description = "Artifact description"
            )
            val argCapturer = ArgumentCaptor.forClass(Artifact::class.java)
            Mockito.verify(artifactRepository).save(argCapturer.capture())
            val matcher = hasProperty<Artifact>("owners", contains(hasProperty<User>("id", equalTo(1L))))
            assertThat(argCapturer.value, matcher)
        }

        @Test
        fun `it should include the user's personal group as one of the artifact's associated groups`() {
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(
                            2,
                            "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).thenReturn(null)
            artifactService.createArtifact(
                "example@example.com",
                "Artifact 1",
                description = "Artifact description"
            )
            val argumentCaptor = ArgumentCaptor.forClass(Artifact::class.java)
            Mockito.verify(artifactRepository).save(argumentCaptor.capture())
            val matcher = hasProperty<Artifact>("groups", contains(hasProperty<Group>("id", equalTo(2L))))
            assertThat(argumentCaptor.value, matcher)
        }

        @Test
        fun `it should make the specified owner IDs the owners of the artifact`() {
            Mockito.`when`(groupRepository.save(any<Group>())).then { it.arguments[0] as Group }
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(userRepository.existsById(anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(
                            2,
                            "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
                User(
                    it.arguments[0] as Long,
                    "User ${it.arguments[0]}",
                    "example${it.arguments[0]}@email.com",
                    "password",
                    privateGroup = Group(
                        2,
                        "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                )
            }
            Mockito.`when`(userRepository.findAllById(any<Iterable<Long>>())).then {
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
            artifactService.createArtifact(
                "example@example.com",
                "Artifact 1",
                description = "Artifact description",
                ownerIDs = listOf(2, 3)
            )
            val argCapturer = ArgumentCaptor.forClass(Artifact::class.java)
            Mockito.verify(artifactRepository).save(argCapturer.capture())
            val matcher =
                hasProperty<Artifact>(
                    "owners",
                    hasItems<User>(
                        hasProperty("id", equalTo(2L)),
                        hasProperty("id", equalTo(3L))
                    )
                )
            assertThat(argCapturer.value, matcher)
        }

        @Test
        fun `it should make the specified group IDs associated with the artifact`() {
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(groupRepository.existsById(anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(
                            2,
                            "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
                User(
                    it.arguments[0] as Long,
                    "User ${it.arguments[0]}",
                    "example${it.arguments[0]}@email.com",
                    "password",
                    privateGroup = Group(
                        2,
                        "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                )
            }
            Mockito.`when`(groupRepository.findAllById(any<Iterable<Long>>())).then {
                (it.arguments[0] as Iterable<Long>).map { id ->
                    Group(
                        id = id,
                        name = "Artifact $id",
                        members = mutableListOf(), description = ""
                    )
                }
            }
            artifactService.createArtifact(
                "example@example.com",
                "Artifact 1",
                description = "Artifact description",
                groupIDs = listOf(2, 3)
            )
            val argCapturer = ArgumentCaptor.forClass(Artifact::class.java)
            Mockito.verify(artifactRepository).save(argCapturer.capture())
            val matcher =
                hasProperty<Artifact>(
                    "groups",
                    hasItems<Group>(
                        hasProperty("id", equalTo(2L)),
                        hasProperty("id", equalTo(3L))
                    )
                )
            assertThat(argCapturer.value, matcher)
        }

        @Test
        fun `it should share the artifact with the specified user IDs`() {
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(userRepository.existsById(anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(
                            2,
                            "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
                User(
                    it.arguments[0] as Long,
                    "User ${it.arguments[0]}",
                    "example${it.arguments[0]}@email.com",
                    "password",
                    privateGroup = Group(
                        2,
                        "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                )
            }
            Mockito.`when`(userRepository.findAllById(any<Iterable<Long>>())).then {
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
            artifactService.createArtifact(
                "example@example.com",
                "Artifact 1",
                description = "Artifact description",
                sharedWith = listOf(2, 3)
            )
            val argCapturer = ArgumentCaptor.forClass(Artifact::class.java)
            Mockito.verify(artifactRepository).save(argCapturer.capture())
            val matcher =
                hasProperty<Artifact>(
                    "sharedWith",
                    hasItems<User>(
                        hasProperty("id", equalTo(2L)),
                        hasProperty("id", equalTo(3L))
                    )
                )
            assertThat(argCapturer.value, matcher)
        }

        @Test
        fun `it should not double up if the creator's ID is specified in the owners`() {
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(userRepository.existsById(anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User 1",
                        "example1@example.com",
                        "password",
                        privateGroup = Group(
                            2,
                            "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
                User(
                    it.arguments[0] as Long,
                    "User ${it.arguments[0]}",
                    "example${it.arguments[0]}@email.com",
                    "password",
                    privateGroup = Group(
                        2,
                        "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                )
            }
            Mockito.`when`(userRepository.findAllById(any<Iterable<Long>>())).then {
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
            artifactService.createArtifact(
                "example@example.com",
                "Artifact 1",
                description = "Artifact description",
                ownerIDs = listOf(1, 2, 3)
            )
            val argCapturer = ArgumentCaptor.forClass(Artifact::class.java)
            Mockito.verify(artifactRepository).save(argCapturer.capture())
            val matcher =
                hasProperty<Artifact>(
                    "owners",
                    containsInAnyOrder<User>(
                        hasProperty("id", equalTo(1L)),
                        hasProperty("id", equalTo(2L)),
                        hasProperty("id", equalTo(3L))
                    )
                )
            assertThat(argCapturer.value, matcher)
        }

        @Test
        fun `it should not double up if the creator's personal group is specified in the associated groups`() {
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(groupRepository.existsById(anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(
                            2,
                            "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
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
            Mockito.`when`(groupRepository.findAllById(any<Iterable<Long>>())).then {
                (it.arguments[0] as Iterable<Long>).map { id ->
                    Group(
                        id = id,
                        name = "Group $id",
                        members = mutableListOf(), description = ""
                    )
                }
            }
            artifactService.createArtifact(
                "example@example.com",
                "Artifact 1",
                description = "Artifact description",
                groupIDs = listOf(1, 2, 3)
            )
            val argCapturer = ArgumentCaptor.forClass(Artifact::class.java)
            Mockito.verify(artifactRepository).save(argCapturer.capture())
            val matcher =
                hasProperty<Artifact>(
                    "groups",
                    containsInAnyOrder<Group>(
                        hasProperty("id", equalTo(1L)),
                        hasProperty("id", equalTo(2L)),
                        hasProperty("id", equalTo(3L))
                    )
                )
            assertThat(argCapturer.value, matcher)
        }

        @Test
        fun `it should not allow creating an artifact associated with albums that don't exist`() {
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(1, "Group1", members = mutableListOf(), description = "")
                    )
                )
            Mockito.`when`(albumRepository.findByIdOrNull(anyLong())).thenReturn(null)
            assertThrows<AlbumNotFoundException> {
                artifactService.createArtifact(
                    "example@example.com",
                    "Artifact 1",
                    description = "Artifact description",
                    albumIDs = listOf(2)
                )
            }
        }

        @Test
        fun `it should create the artifact with the specified albums`() {
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(albumRepository.existsById(anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(2, "Group 1", members = mutableListOf(), description = "")
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
                User(
                    it.arguments[0] as Long,
                    "User ${it.arguments[0]}",
                    "example${it.arguments[0]}@email.com",
                    "password",
                    privateGroup = Group(2, "Group 1", members = mutableListOf(), description = "")
                )
            }
            Mockito.`when`(albumRepository.findAllById(any<Iterable<Long>>())).then {
                (it.arguments[0] as Iterable<Long>).map { id ->
                    Album(
                        id = id,
                        name = "Artifact $id",
                        description = "Description",
                        owners = mutableListOf()
                    )
                }
            }
            artifactService.createArtifact(
                "example@example.com",
                "Artifact 1",
                description = "Artifact description",
                albumIDs = listOf(2, 3)
            )
            val argCapturer = ArgumentCaptor.forClass(Artifact::class.java)
            Mockito.verify(artifactRepository).save(argCapturer.capture())
            val matcher =
                hasProperty<Artifact>(
                    "albums",
                    hasItems<Group>(
                        hasProperty("id", equalTo(2L)),
                        hasProperty("id", equalTo(3L))
                    )
                )
            assertThat(argCapturer.value, matcher)
        }
    }

    @Nested
    inner class FindArtifactsByOwner {

        @Test
        fun `it should find all the artifacts accessible by the user`() {
            val email = "example@example.com"
            val groupArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    2,
                    "Artifact 2",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    3,
                    "Artifact 3",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    4,
                    "Artifact 4",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                )
            )
            val ownerArtifacts = listOf(
                Artifact(
                    5,
                    "Artifact 7",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    6,
                    "Artifact 8",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    7,
                    "Artifact 9",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    8,
                    "Artifact 10",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                )
            )
            val sharedArtifacts = listOf(
                Artifact(
                    11,
                    "Artifact 11",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    12,
                    "Artifact 12",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    13,
                    "Artifact 13",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    14,
                    "Artifact 14",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                )
            )
            val albumArtifacts = listOf(
                Artifact(
                    15,
                    "Artifact 15",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    16,
                    "Artifact 16",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    17,
                    "Artifact 17",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                ),
                Artifact(
                    18,
                    "Artifact 18",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf()
                )
            )
            val album = Album(
                1,
                "Album 1",
                "Description",
                owners = mutableListOf(),
                groups = mutableListOf(),
                sharedWith = mutableListOf()
            )
            val user = User(
                id = 1,
                name = "User 1",
                email = email,
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                ),
                ownedAlbums = mutableListOf(album)
            )

            Mockito.`when`(userRepository.findByEmail(email))
                .thenReturn(user)
            Mockito.`when`(artifactRepository.findByGroups_Id(anyLong())).thenReturn(groupArtifacts)
            Mockito.`when`(artifactRepository.findByOwners_Email(anyString())).thenReturn(ownerArtifacts)
            Mockito.`when`(artifactRepository.findBySharedWith_Email(anyString())).thenReturn(sharedArtifacts)
            Mockito.`when`(artifactRepository.findByAlbums_Id(anyLong())).thenReturn(albumArtifacts)

            val foundArtifacts = artifactService.findArtifactsByOwner(email, artifactName = null)
            val allArtifacts = ownerArtifacts + groupArtifacts + sharedArtifacts + albumArtifacts
            assertEquals(allArtifacts.size, foundArtifacts.size)
            assertThat(foundArtifacts, containsInAnyOrder(*allArtifacts.toTypedArray()))
        }

        @Test
        fun `it should filter the accessible artifacts by the group ID if specified`() {
            val email = "example@example.com"
            val groupArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    2,
                    "Artifact 2",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    3,
                    "Artifact 3",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    4,
                    "Artifact 4",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            val ownerArtifacts = listOf(
                Artifact(
                    5,
                    "Artifact 7",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    6,
                    "Artifact 8",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    7,
                    "Artifact 9",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    8,
                    "Artifact 10",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            val sharedArtifacts = listOf(
                Artifact(
                    11,
                    "Artifact 11",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    12,
                    "Artifact 12",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    13,
                    "Artifact 13",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    14,
                    "Artifact 14",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            Mockito.`when`(userRepository.findByEmail(email))
                .thenReturn(
                    User(
                        id = 1,
                        name = "User 1",
                        email = email,
                        password = "password",
                        groups = mutableListOf(
                            Group(
                                id = 1,
                                name = "Group 1",
                                members = mutableListOf(),
                                description = ""
                            )
                        ),
                        privateGroup = Group(
                            2,
                            "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            Mockito.`when`(groupRepository.existsById(anyLong())).thenReturn(true)
            Mockito.`when`(artifactRepository.findByGroups_Id(anyLong())).thenReturn(groupArtifacts)
            Mockito.`when`(artifactRepository.findByOwners_Email(anyString())).thenReturn(ownerArtifacts)
            Mockito.`when`(artifactRepository.findBySharedWith_Email(anyString())).thenReturn(sharedArtifacts)

            val foundArtifacts = artifactService.findArtifactsByOwner(email, groupID = 1, artifactName = null)
            val expectedArtifacts =
                (groupArtifacts + ownerArtifacts + sharedArtifacts).filter { it.groups.first().id == (1).toLong() }
            assertEquals(expectedArtifacts.size, foundArtifacts.size)
            assertThat(foundArtifacts, containsInAnyOrder(*expectedArtifacts.toTypedArray()))
        }

        @Test
        fun `it should filter the accessible artifacts by the owner ID if specified`() {
            val email = "example@example.com"
            val user = User(
                id = 1,
                name = "User 1",
                email = email,
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            val groupArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    2,
                    "Artifact 2",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    3,
                    "Artifact 3",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    4,
                    "Artifact 4",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            val ownerArtifacts = listOf(
                Artifact(
                    5,
                    "Artifact 7",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    6,
                    "Artifact 8",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    7,
                    "Artifact 9",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    8,
                    "Artifact 10",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            val sharedArtifacts = listOf(
                Artifact(
                    11,
                    "Artifact 11",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    12,
                    "Artifact 12",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    13,
                    "Artifact 13",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    14,
                    "Artifact 14",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(artifactRepository.findByGroups_Id(anyLong())).thenReturn(groupArtifacts)
            Mockito.`when`(artifactRepository.findByOwners_Email(anyString())).thenReturn(ownerArtifacts)
            Mockito.`when`(artifactRepository.findBySharedWith_Email(anyString())).thenReturn(sharedArtifacts)

            val foundArtifacts = artifactService.findArtifactsByOwner(email, ownerID = user.id, artifactName = null)
            val expectedArtifacts =
                (groupArtifacts + ownerArtifacts + sharedArtifacts).filter { it.owners.firstOrNull()?.id == user.id }
            assertEquals(expectedArtifacts.size, foundArtifacts.size)
            assertThat(foundArtifacts, containsInAnyOrder(*expectedArtifacts.toTypedArray()))
        }

        @Test
        fun `it should filter the accessible artifacts by the shared ID if specified`() {
            val email = "example@example.com"
            val user = User(
                id = 1,
                name = "User 1",
                email = email,
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            val groupArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    2,
                    "Artifact 2",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    3,
                    "Artifact 3",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    4,
                    "Artifact 4",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            val ownerArtifacts = listOf(
                Artifact(
                    5,
                    "Artifact 7",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    6,
                    "Artifact 8",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    7,
                    "Artifact 9",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    8,
                    "Artifact 10",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            val sharedArtifacts = listOf(
                Artifact(
                    11,
                    "Artifact 11",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    ),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    12,
                    "Artifact 12",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    ),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    13,
                    "Artifact 13",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    ),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    14,
                    "Artifact 14",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    ),
                    sharedWith = mutableListOf(user)
                )
            )
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(artifactRepository.findByGroups_Id(anyLong())).thenReturn(groupArtifacts)
            Mockito.`when`(artifactRepository.findByOwners_Email(anyString())).thenReturn(ownerArtifacts)
            Mockito.`when`(artifactRepository.findBySharedWith_Email(anyString())).thenReturn(sharedArtifacts)

            val foundArtifacts = artifactService.findArtifactsByOwner(email, sharedID = user.id, artifactName = null)
            val expectedArtifacts =
                (groupArtifacts + ownerArtifacts + sharedArtifacts).filter { it.sharedWith.firstOrNull()?.id == user.id }
            assertEquals(expectedArtifacts.size, foundArtifacts.size)
            assertThat(foundArtifacts, containsInAnyOrder(*expectedArtifacts.toTypedArray()))
        }

        @Test
        fun `it should not return duplicate artifacts`() {
            val email = "example@example.com"
            val user = User(
                id = 1,
                name = "User 1",
                email = email,
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            val groupArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    2,
                    "Artifact 2",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    3,
                    "Artifact 3",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    4,
                    "Artifact 4",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            val ownerArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    6,
                    "Artifact 8",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    7,
                    "Artifact 9",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    8,
                    "Artifact 10",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            val sharedArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    12,
                    "Artifact 12",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    ),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    13,
                    "Artifact 13",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    ),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    14,
                    "Artifact 14",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    ),
                    sharedWith = mutableListOf(user)
                )
            )
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(artifactRepository.findByGroups_Id(anyLong())).thenReturn(groupArtifacts)
            Mockito.`when`(artifactRepository.findByOwners_Email(anyString())).thenReturn(ownerArtifacts)
            Mockito.`when`(artifactRepository.findBySharedWith_Email(anyString())).thenReturn(sharedArtifacts)

            val foundArtifacts = artifactService.findArtifactsByOwner(email, artifactName = null)
            val expectedArtifacts = (groupArtifacts + ownerArtifacts + sharedArtifacts).toSet().toList()
            assertEquals(expectedArtifacts.size, foundArtifacts.size)
            assertThat(foundArtifacts, containsInAnyOrder(*expectedArtifacts.toTypedArray()))
        }

        @Test
        fun `it should filter by artifact name`() {
            val email = "example@example.com"
            val user = User(
                id = 1,
                name = "User 1",
                email = email,
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            val groupArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 100",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    2,
                    "Artifact 2",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    3,
                    "Artifact 3",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    4,
                    "Artifact 100",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 1,
                            name = "Group 1",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            val ownerArtifacts = listOf(
                Artifact(
                    5,
                    "Artifact 7",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    6,
                    "Artifact 8",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    7,
                    "Artifact 9",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                ),
                Artifact(
                    8,
                    "Artifact 10",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    )
                )
            )
            val sharedArtifacts = listOf(
                Artifact(
                    11,
                    "Artifact 100",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    ),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    12,
                    "Artifact 12",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    ),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    13,
                    "Artifact 13",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    ),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    14,
                    "Artifact 100",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(
                        Group(
                            id = 2,
                            name = "Group 2",
                            members = mutableListOf(),
                            description = ""
                        )
                    ),
                    sharedWith = mutableListOf(user)
                )
            )
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(artifactRepository.findByGroups_Id(anyLong())).thenReturn(groupArtifacts)
            Mockito.`when`(artifactRepository.findByOwners_Email(anyString())).thenReturn(ownerArtifacts)
            Mockito.`when`(artifactRepository.findBySharedWith_Email(anyString())).thenReturn(sharedArtifacts)

            val foundArtifacts = artifactService.findArtifactsByOwner(email, artifactName = "Artifact 100")
            val expectedArtifacts =
                (groupArtifacts + ownerArtifacts + sharedArtifacts).filter { it.name == "Artifact 100" }
            assertEquals(expectedArtifacts.size, foundArtifacts.size)
            assertThat(foundArtifacts, containsInAnyOrder(*expectedArtifacts.toTypedArray()))
        }
    }

    @Nested
    inner class UpdateArtifact {
        @Test
        fun `it should not allow users without permission to modify the artifact`() {
            val email = "example@example.com"
            var owningUser = User(
                id = 2,
                name = "User 2",
                email = "example@example2.com",
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            val artifact = Artifact(
                1,
                "Artifact 1",
                "Description",
                mutableListOf(owningUser),
                mutableListOf()
            )
            owningUser = owningUser.copy(ownedArtifacts = mutableListOf(artifact))
            val user = User(
                id = 1,
                name = "User 1",
                email = email,
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong())).then {
                if (it.arguments[0] == artifact.id) {
                    Optional.of(artifact)
                } else {
                    Optional.empty()
                }
            }
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when (it.arguments[0]) {
                    user.email -> user
                    owningUser.email -> owningUser
                    else -> throw RuntimeException("${it.arguments[0]} not handled")
                }
            }
            assertThrows<ActionNotAllowedException> {
                artifactService.updateArtifact(
                    user.email, artifact.id, ArtifactRequest(
                        artifact.name,
                        "updated description",
                        artifact.owners.map(User::id),
                        artifact.groups.map(Group::id),
                        artifact.sharedWith.map(User::id)
                    )
                )
            }
        }

        @Test
        fun `it should allow group owners to remove their group from the artifact`() {
            val email = "example@example.com"
            var group = Group(
                id = 1,
                name = "Group 1",
                members = mutableListOf(),
                description = ""
            )
            val groupOwner = User(
                id = 2,
                name = "User 2",
                email = "example@example2.com",
                password = "password",
                groups = mutableListOf(
                    group
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                ),
                ownedGroups = mutableListOf(group)
            )
            val artifact = Artifact(
                1,
                "Artifact 1",
                "Description",
                owners = mutableListOf(),
                groups = mutableListOf(group)
            )
            group = group.copy(artifacts = mutableListOf(artifact))
            val user = User(
                id = 1,
                name = "User 1",
                email = email,
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong())).then {
                if (it.arguments[0] == artifact.id) {
                    Optional.of(artifact)
                } else {
                    Optional.empty()
                }
            }
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when (it.arguments[0]) {
                    user.email -> user
                    groupOwner.email -> groupOwner
                    else -> throw RuntimeException("${it.arguments[0]} not handled")
                }
            }
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong())).then { Optional.of(group) }
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }

            val updatedArtifact =
                artifactService.updateArtifact(
                    groupOwner.email, artifact.id, ArtifactRequest(
                        artifact.name,
                        artifact.description,
                        artifact.owners.map(User::id),
                        emptyList(),
                        artifact.sharedWith.map(User::id)
                    )
                )
            assertThat(
                updatedArtifact, equalTo(artifact.copy(groups = mutableListOf()))
            )
            Mockito.verify(artifactRepository).save(artifact.copy(groups = mutableListOf()))
        }

        @Test
        fun `it should not allow group owners to make changes except for their group`() {
            val email = "example@example.com"
            var group = Group(
                id = 1,
                name = "Group 1",
                members = mutableListOf(),
                description = ""
            )
            val groupOwner = User(
                id = 2,
                name = "User 2",
                email = "example@example2.com",
                password = "password",
                groups = mutableListOf(
                    group
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                ),
                ownedGroups = mutableListOf(group)
            )
            val artifact = Artifact(
                1,
                "Artifact 1",
                "Description",
                owners = mutableListOf(),
                groups = mutableListOf(group)
            )
            group = group.copy(artifacts = mutableListOf(artifact))
            val user = User(
                id = 1,
                name = "User 1",
                email = email,
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong())).then {
                if (it.arguments[0] == artifact.id) {
                    Optional.of(artifact)
                } else {
                    Optional.empty()
                }
            }
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when (it.arguments[0]) {
                    user.email -> user
                    groupOwner.email -> groupOwner
                    else -> throw RuntimeException("${it.arguments[0]} not handled")
                }
            }
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong())).then { Optional.of(group) }
            assertThrows<ActionNotAllowedException> {
                artifactService.updateArtifact(
                    user.email, artifact.id, ArtifactRequest(
                        artifact.name,
                        "updated description",
                        artifact.owners.map(User::id),
                        artifact.groups.map(Group::id),
                        artifact.sharedWith.map(User::id)
                    )
                )
            }
        }

        @Test
        fun `it should not allow group owners to remove the artifact from other groups`() {
            val email = "example@example.com"
            var group = Group(
                id = 1,
                name = "Group 1",
                members = mutableListOf(),
                description = ""
            )
            val otherGroup = Group(
                id = 2,
                name = "Group 2",
                members = mutableListOf(),
                description = ""
            )
            val groupOwner = User(
                id = 2,
                name = "User 2",
                email = "example@example2.com",
                password = "password",
                groups = mutableListOf(
                    group
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                ),
                ownedGroups = mutableListOf(group)
            )
            val artifact = Artifact(
                1,
                "Artifact 1",
                "Description",
                owners = mutableListOf(),
                groups = mutableListOf(group, otherGroup)
            )
            group = group.copy(artifacts = mutableListOf(artifact))
            val user = User(
                id = 1,
                name = "User 1",
                email = email,
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong())).then {
                if (it.arguments[0] == artifact.id) {
                    Optional.of(artifact)
                } else {
                    Optional.empty()
                }
            }
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when (it.arguments[0]) {
                    user.email -> user
                    groupOwner.email -> groupOwner
                    else -> throw RuntimeException("${it.arguments[0]} not handled")
                }
            }
            Mockito.`when`(groupRepository.findByIdOrNull(anyLong())).then { Optional.of(group) }
            val exception = assertThrows<ActionNotAllowedException> {
                artifactService.updateArtifact(
                    user.email, artifact.id, ArtifactRequest(
                        artifact.name,
                        artifact.description,
                        artifact.owners.map(User::id),
                        artifact.groups.map(Group::id).filter { it != otherGroup.id },
                        artifact.sharedWith.map(User::id)
                    )
                )
            }
            assertEquals(
                "User ${user.id} is not an admin of all the groups they attempted to remove",
                exception.message
            )
        }

        @Test
        fun `it should allow artifact owners to make changes to the artifact`() {
            var owningUser = User(
                id = 2,
                name = "User 2",
                email = "example@example2.com",
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            var artifact = Artifact(
                1,
                "Artifact 1",
                "Description",
                mutableListOf(owningUser),
                mutableListOf()
            )
            owningUser = owningUser.copy(ownedArtifacts = mutableListOf(artifact))
            artifact = artifact.copy(owners = mutableListOf(owningUser))
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong())).then {
                if (it.arguments[0] == artifact.id) {
                    Optional.of(artifact)
                } else {
                    Optional.empty()
                }
            }
            Mockito.`when`(userRepository.existsById(anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when (it.arguments[0]) {
                    owningUser.email -> owningUser
                    else -> throw RuntimeException("${it.arguments[0]} not handled")
                }
            }
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(userRepository.findAllById(any<Iterable<Long>>())).then {
                val args = it.arguments[0] as Iterable<Long>
                if (args.toList() == artifact.owners.map(User::id)) {
                    artifact.owners
                } else {
                    listOf<User>()
                }
            }
            val updatedArtifact = artifactService.updateArtifact(
                owningUser.email, artifact.id, ArtifactRequest(
                    artifact.name,
                    "updated description",
                    artifact.owners.map(User::id),
                    artifact.groups.map(Group::id),
                    artifact.sharedWith.map(User::id)
                )
            )
            assertThat(
                updatedArtifact, equalTo(artifact.copy(description = "updated description"))
            )
            Mockito.verify(artifactRepository).save(artifact.copy(description = "updated description"))
        }

        @Test
        fun `it should not allow the updating of the artifact with albums that don't exist`() {
            val artifact = Artifact(
                id = 1,
                name = "Artifact 1",
                description = "Description",
                groups = mutableListOf(),
                owners = mutableListOf(User(
                    1,
                    "User1",
                    "example@example.com",
                    "password",
                    privateGroup = Group(1, "Group1", members = mutableListOf(), description = "")
                ))
            )
            Mockito.`when`(artifactRepository.findById(anyLong())).thenReturn(Optional.of(artifact))
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(1, "Group1", members = mutableListOf(), description = "")
                    )
                )
            Mockito.`when`(albumRepository.findByIdOrNull(anyLong())).thenReturn(null)
            assertThrows<AlbumNotFoundException> {
                artifactService.updateArtifact(
                    "example@example.com",
                    1,
                    ArtifactRequest(
                        name = artifact.name,
                        description = artifact.description,
                        albums = listOf(2),
                        groups = null,
                        owners = null,
                        sharedWith = null
                    )
                )
            }
        }

        @Test
        fun `it should update the artifact with the specified albums`() {
            val user = User(
                1,
                "User1",
                "example@example.com",
                "password",
                privateGroup = Group(1, "Group1", members = mutableListOf(), description = "")
            )
            val artifact = Artifact(
                id = 1,
                name = "Artifact 1",
                description = "Description",
                groups = mutableListOf(),
                owners = mutableListOf(user)
            )
            val album = Album(
                id = 2,
                name = "Album 2",
                description = "Description",
                owners = mutableListOf()
            )
            Mockito.`when`(artifactRepository.findById(anyLong())).thenReturn(Optional.of(artifact))
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(userRepository.findByEmail(anyString())).thenReturn(user)
            Mockito.`when`(albumRepository.findById(anyLong())).thenReturn(Optional.of(album))
            Mockito.`when`(albumRepository.existsById(anyLong())).thenReturn(true)
                artifactService.updateArtifact(
                    "example@example.com",
                    1,
                    ArtifactRequest(
                        name = artifact.name,
                        description = artifact.description,
                        albums = listOf(2),
                        groups = null,
                        owners = null,
                        sharedWith = null
                    )
                )
        }
    }

    @Nested
    inner class DeleteArtifact {
        @Test
        fun `it should not allow user's who are not the artifact's owners to delete it`() {
            val email = "example@example.com"
            var owningUser = User(
                id = 2,
                name = "User 2",
                email = "example@example2.com",
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            val artifact = Artifact(
                1,
                "Artifact 1",
                "Description",
                mutableListOf(owningUser),
                mutableListOf()
            )
            owningUser = owningUser.copy(ownedArtifacts = mutableListOf(artifact))
            val user = User(
                id = 1,
                name = "User 1",
                email = email,
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong())).then {
                if (it.arguments[0] == artifact.id) {
                    Optional.of(artifact)
                } else {
                    Optional.empty()
                }
            }
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when (it.arguments[0]) {
                    user.email -> user
                    owningUser.email -> owningUser
                    else -> throw RuntimeException("${it.arguments[0]} not handled")
                }
            }
            assertThrows<ActionNotAllowedException> { artifactService.deleteArtifact(user.email, artifact.id) }
        }

        @Test
        fun `it should allow the artifact's owners to delete it`() {
            var owningUser = User(
                id = 2,
                name = "User 2",
                email = "example@example2.com",
                password = "password",
                groups = mutableListOf(
                    Group(
                        id = 1,
                        name = "Group 1",
                        members = mutableListOf(),
                        description = ""
                    )
                ),
                privateGroup = Group(
                    2,
                    "Group 2",
                    members = mutableListOf(),
                    description = ""
                )
            )
            var artifact = Artifact(
                1,
                "Artifact 1",
                "Description",
                mutableListOf(owningUser),
                mutableListOf()
            )
            owningUser = owningUser.copy(ownedArtifacts = mutableListOf(artifact))
            artifact = artifact.copy(owners = mutableListOf(owningUser))
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong())).then {
                if (it.arguments[0] == artifact.id) {
                    Optional.of(artifact)
                } else {
                    Optional.empty()
                }
            }
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when (it.arguments[0]) {
                    owningUser.email -> owningUser
                    else -> throw RuntimeException("${it.arguments[0]} not handled")
                }
            }
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(userRepository.findAllById(any<Iterable<Long>>())).then {
                val args = it.arguments[0] as Iterable<Long>
                if (args.toList() == artifact.owners.map(User::id)) {
                    artifact.owners
                } else {
                    listOf<User>()
                }
            }
            val deletedArtifact = artifactService.deleteArtifact(owningUser.email, artifact.id)
            assertThat(deletedArtifact, equalTo(artifact))
            Mockito.verify(artifactRepository).delete(artifact)
        }
    }
}

@SpringBootTest
@ExtendWith(SpringExtension::class)
class ArtifactServiceImplIntegrationTest {
    @Autowired
    private lateinit var artifactService: ArtifactService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var artifactRepository: ArtifactRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var testUtils: TestUtilsService

    val email = "example@example.com"

    @BeforeEach
    fun beforeEach() {
        testUtils.clearDatabase()
    }

    @Nested
    inner class CreateArtifact {
        @Test
        fun `it should insert the artifact in the database`() {
            val user = userService.createUser("User1", email, "password")
            val createdArtifact = artifactService.createArtifact(
                user.email,
                "Artifact 1",
                "Description of artifact",
                ownerIDs = listOf(),
                groupIDs = listOf(),
                sharedWith = listOf()
            )
            assertTrue(artifactRepository.existsById(createdArtifact.id))
        }

        @Test
        fun `it should update the creating user's relationship with the artifact`() {
            val user = userService.createUser("User1", email, "password")
            val createdArtifact = artifactService.createArtifact(
                user.email,
                "Artifact 1",
                "Description of artifact",
                ownerIDs = listOf(),
                groupIDs = listOf(),
                sharedWith = listOf()
            )

            val updatedUser = userRepository.findByIdOrNull(user.id)!!
            assertThat(
                updatedUser,
                hasProperty("ownedArtifacts", contains(hasProperty<Artifact>("id", equalTo(createdArtifact.id))))
            )
        }

        @Test
        fun `it should update the owning user's relationship with the artifact`() {
            val user = userService.createUser("User1", email, "password")
            val owningUsers = listOf(
                userService.createUser("User2", "example2@example.com", "password"),
                userService.createUser("User3", "example3@example.com", "password"),
                userService.createUser("User4", "example4@example.com", "password")
            )
            val createdArtifact = artifactService.createArtifact(
                user.email,
                "Artifact 1",
                "Description of artifact",
                ownerIDs = owningUsers.map(User::id),
                groupIDs = listOf(),
                sharedWith = listOf()
            )

            val updatedOwningUsers = userRepository.findAllById(owningUsers.map(User::id))
            updatedOwningUsers.forEach {
                println("ownedArtifacts = ${it.ownedArtifacts}")
            }
            updatedOwningUsers.forEach {
                assertThat(
                    it.ownedArtifacts,
                    contains(hasProperty("id", equalTo(createdArtifact.id)))
                )
            }
        }

        @Test
        fun `it should update the groups's relationship with the artifact`() {
            val user = userService.createUser("User1", email, "password")
            val groups = listOf(
                groupRepository.save(
                    Group(
                        name = "Group1",
                        members = mutableListOf(user),
                        artifacts = mutableListOf(), description = ""
                    )
                ),
                groupRepository.save(
                    Group(
                        name = "Group2",
                        members = mutableListOf(user),
                        artifacts = mutableListOf(), description = ""
                    )
                ),
                groupRepository.save(
                    Group(
                        name = "Group3",
                        members = mutableListOf(user),
                        artifacts = mutableListOf(),
                        description = ""
                    )
                )
            )
            val createdArtifact = artifactService.createArtifact(
                user.email,
                "Artifact 1",
                "Description of artifact",
                ownerIDs = listOf(),
                groupIDs = groups.map(Group::id),
                sharedWith = listOf()
            )
            groupRepository.findAllById(groups.map(Group::id)).forEach {
                assertThat(
                    it,
                    hasProperty("artifacts", contains(hasProperty<Artifact>("id", equalTo(createdArtifact.id))))
                )
            }
        }
    }
}

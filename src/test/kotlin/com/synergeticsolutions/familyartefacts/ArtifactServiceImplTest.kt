package com.synergeticsolutions.familyartefacts

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

class ArtifactServiceImplTest {
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val groupRepository: GroupRepository = Mockito.mock(GroupRepository::class.java)
    private val artifactRepository: ArtifactRepository = Mockito.mock(ArtifactRepository::class.java)

    private val artifactService: ArtifactService =
        ArtifactServiceImpl(artifactRepository, userRepository, groupRepository)

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
                        privateGroup = Group(1, "Group1", members = mutableListOf())
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
                        privateGroup = Group(1, "Group1", members = mutableListOf())
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
                            1, "Group 1", members = mutableListOf()
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
                        privateGroup = Group(1, "Group 1", members = mutableListOf())
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
            val matcher = hasProperty<Artifact>("owners", contains(hasProperty("id", equalTo(1L))))
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
                        privateGroup = Group(2, "Group 1", members = mutableListOf())
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
            val matcher = hasProperty<Artifact>("groups", contains(hasProperty("id", equalTo(2L))))
            assertThat(argumentCaptor.value, matcher)
        }

        @Test
        fun `it should make the specified owner IDs the owners of the artifact`() {
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] as Artifact }
            Mockito.`when`(userRepository.existsById(anyLong())).thenReturn(true)
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        1,
                        "User1",
                        "example@example.com",
                        "password",
                        privateGroup = Group(2, "Group 1", members = mutableListOf())
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
                User(
                    it.arguments[0] as Long,
                    "User ${it.arguments[0]}",
                    "example${it.arguments[0]}@email.com",
                    "password",
                    privateGroup = Group(2, "Group 1", members = mutableListOf())
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
                            1, "Group1", members = mutableListOf()
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
                        privateGroup = Group(2, "Group 1", members = mutableListOf())
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
                User(
                    it.arguments[0] as Long,
                    "User ${it.arguments[0]}",
                    "example${it.arguments[0]}@email.com",
                    "password",
                    privateGroup = Group(2, "Group 1", members = mutableListOf())
                )
            }
            Mockito.`when`(groupRepository.findAllById(any<Iterable<Long>>())).then {
                (it.arguments[0] as Iterable<Long>).map { id ->
                    Group(
                        id = id,
                        name = "Artifact $id",
                        members = mutableListOf()
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
                        privateGroup = Group(2, "Group 1", members = mutableListOf())
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
                User(
                    it.arguments[0] as Long,
                    "User ${it.arguments[0]}",
                    "example${it.arguments[0]}@email.com",
                    "password",
                    privateGroup = Group(2, "Group 1", members = mutableListOf())
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
                            1, "Group1", members = mutableListOf()
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
                        privateGroup = Group(2, "Group 1", members = mutableListOf())
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
                User(
                    it.arguments[0] as Long,
                    "User ${it.arguments[0]}",
                    "example${it.arguments[0]}@email.com",
                    "password",
                    privateGroup = Group(2, "Group 1", members = mutableListOf())
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
                            2, "Group 1", members = mutableListOf()
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
                        privateGroup = Group(2, "Group 2", members = mutableListOf())
                    )
                )
            Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then {
                User(
                    it.arguments[0] as Long,
                    "User ${it.arguments[0]}",
                    "example${it.arguments[0]}@email.com",
                    "password",
                    privateGroup = Group(2, "Group 1", members = mutableListOf())
                )
            }
            Mockito.`when`(groupRepository.findAllById(any<Iterable<Long>>())).then {
                (it.arguments[0] as Iterable<Long>).map { id ->
                    Group(
                        id = id,
                        name = "Group $id",
                        members = mutableListOf()
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
    }

    @Nested
    inner class FindArtifactsByOwner {

        @Test
        fun `it should find all the artifacts accessible by the user`() {
            val email = "example@example.com"
            val groupArtifacts = listOf(
                Artifact(1, "Artifact 1", "Description", owners = mutableListOf(), groups = mutableListOf()),
                Artifact(2, "Artifact 2", "Description", owners = mutableListOf(), groups = mutableListOf()),
                Artifact(3, "Artifact 3", "Description", owners = mutableListOf(), groups = mutableListOf()),
                Artifact(4, "Artifact 4", "Description", owners = mutableListOf(), groups = mutableListOf())
            )
            val ownerArtifacts = listOf(
                Artifact(5, "Artifact 7", "Description", owners = mutableListOf(), groups = mutableListOf()),
                Artifact(6, "Artifact 8", "Description", owners = mutableListOf(), groups = mutableListOf()),
                Artifact(7, "Artifact 9", "Description", owners = mutableListOf(), groups = mutableListOf()),
                Artifact(8, "Artifact 10", "Description", owners = mutableListOf(), groups = mutableListOf())
            )
            val sharedArtifacts = listOf(
                Artifact(11, "Artifact 11", "Description", owners = mutableListOf(), groups = mutableListOf()),
                Artifact(12, "Artifact 12", "Description", owners = mutableListOf(), groups = mutableListOf()),
                Artifact(13, "Artifact 13", "Description", owners = mutableListOf(), groups = mutableListOf()),
                Artifact(14, "Artifact 14", "Description", owners = mutableListOf(), groups = mutableListOf())
            )
            Mockito.`when`(userRepository.findByEmail(email))
                .thenReturn(
                    User(
                        id = 1, name = "User 1", email = email, password = "password", groups = mutableListOf(
                            Group(id = 1, name = "Group 1", members = mutableListOf())
                        ), privateGroup = Group(2, "Group 2", members = mutableListOf())
                    )
                )
            Mockito.`when`(artifactRepository.findByGroups_Id(anyLong())).thenReturn(groupArtifacts)
            Mockito.`when`(artifactRepository.findByOwners_Email(anyString())).thenReturn(ownerArtifacts)
            Mockito.`when`(artifactRepository.findBySharedWith_Email(anyString())).thenReturn(sharedArtifacts)

            val foundArtifacts = artifactService.findArtifactsByOwner(email)
            val allArtifacts = ownerArtifacts + groupArtifacts + sharedArtifacts
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
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    2,
                    "Artifact 2",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    3,
                    "Artifact 3",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    4,
                    "Artifact 4",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                )
            )
            val ownerArtifacts = listOf(
                Artifact(
                    5,
                    "Artifact 7",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    6,
                    "Artifact 8",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    7,
                    "Artifact 9",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    8,
                    "Artifact 10",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                )
            )
            val sharedArtifacts = listOf(
                Artifact(
                    11,
                    "Artifact 11",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    12,
                    "Artifact 12",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    13,
                    "Artifact 13",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    14,
                    "Artifact 14",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                )
            )
            Mockito.`when`(userRepository.findByEmail(email))
                .thenReturn(
                    User(
                        id = 1, name = "User 1", email = email, password = "password", groups = mutableListOf(
                            Group(id = 1, name = "Group 1", members = mutableListOf())
                        ), privateGroup = Group(2, "Group 2", members = mutableListOf())
                    )
                )
            Mockito.`when`(artifactRepository.findByGroups_Id(anyLong())).thenReturn(groupArtifacts)
            Mockito.`when`(artifactRepository.findByOwners_Email(anyString())).thenReturn(ownerArtifacts)
            Mockito.`when`(artifactRepository.findBySharedWith_Email(anyString())).thenReturn(sharedArtifacts)

            val foundArtifacts = artifactService.findArtifactsByOwner(email, groupID = 1)
            val expectedArtifacts =
                (groupArtifacts + ownerArtifacts + sharedArtifacts).filter { it.groups.first().id == (1).toLong() }
            assertEquals(expectedArtifacts.size, foundArtifacts.size)
            assertThat(foundArtifacts, containsInAnyOrder(*expectedArtifacts.toTypedArray()))
        }

        @Test
        fun `it should filter the accessible artifacts by the owner ID if specified`() {
            val email = "example@example.com"
            val user = User(
                id = 1, name = "User 1", email = email, password = "password", groups = mutableListOf(
                    Group(id = 1, name = "Group 1", members = mutableListOf())
                ), privateGroup = Group(2, "Group 2", members = mutableListOf())
            )
            val groupArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    2,
                    "Artifact 2",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    3,
                    "Artifact 3",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    4,
                    "Artifact 4",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                )
            )
            val ownerArtifacts = listOf(
                Artifact(
                    5,
                    "Artifact 7",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    6,
                    "Artifact 8",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    7,
                    "Artifact 9",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    8,
                    "Artifact 10",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                )
            )
            val sharedArtifacts = listOf(
                Artifact(
                    11,
                    "Artifact 11",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    12,
                    "Artifact 12",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    13,
                    "Artifact 13",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    14,
                    "Artifact 14",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                )
            )
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(artifactRepository.findByGroups_Id(anyLong())).thenReturn(groupArtifacts)
            Mockito.`when`(artifactRepository.findByOwners_Email(anyString())).thenReturn(ownerArtifacts)
            Mockito.`when`(artifactRepository.findBySharedWith_Email(anyString())).thenReturn(sharedArtifacts)

            val foundArtifacts = artifactService.findArtifactsByOwner(email, ownerID = user.id)
            val expectedArtifacts =
                (groupArtifacts + ownerArtifacts + sharedArtifacts).filter { it.owners.firstOrNull()?.id == user.id }
            assertEquals(expectedArtifacts.size, foundArtifacts.size)
            assertThat(foundArtifacts, containsInAnyOrder(*expectedArtifacts.toTypedArray()))
        }

        @Test
        fun `it should filter the accessible artifacts by the shared ID if specified`() {
            val email = "example@example.com"
            val user = User(
                id = 1, name = "User 1", email = email, password = "password", groups = mutableListOf(
                    Group(id = 1, name = "Group 1", members = mutableListOf())
                ), privateGroup = Group(2, "Group 2", members = mutableListOf())
            )
            val groupArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    2,
                    "Artifact 2",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    3,
                    "Artifact 3",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    4,
                    "Artifact 4",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                )
            )
            val ownerArtifacts = listOf(
                Artifact(
                    5,
                    "Artifact 7",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    6,
                    "Artifact 8",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    7,
                    "Artifact 9",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    8,
                    "Artifact 10",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                )
            )
            val sharedArtifacts = listOf(
                Artifact(
                    11,
                    "Artifact 11",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf())),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    12,
                    "Artifact 12",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf())),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    13,
                    "Artifact 13",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf())),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    14,
                    "Artifact 14",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf())),
                    sharedWith = mutableListOf(user)
                )
            )
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(artifactRepository.findByGroups_Id(anyLong())).thenReturn(groupArtifacts)
            Mockito.`when`(artifactRepository.findByOwners_Email(anyString())).thenReturn(ownerArtifacts)
            Mockito.`when`(artifactRepository.findBySharedWith_Email(anyString())).thenReturn(sharedArtifacts)

            val foundArtifacts = artifactService.findArtifactsByOwner(email, sharedID = user.id)
            val expectedArtifacts =
                (groupArtifacts + ownerArtifacts + sharedArtifacts).filter { it.sharedWith.firstOrNull()?.id == user.id }
            assertEquals(expectedArtifacts.size, foundArtifacts.size)
            assertThat(foundArtifacts, containsInAnyOrder(*expectedArtifacts.toTypedArray()))
        }

        @Test
        fun `it should not return duplicate artifacts`() {
            val email = "example@example.com"
            val user = User(
                id = 1, name = "User 1", email = email, password = "password", groups = mutableListOf(
                    Group(id = 1, name = "Group 1", members = mutableListOf())
                ), privateGroup = Group(2, "Group 2", members = mutableListOf())
            )
            val groupArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    2,
                    "Artifact 2",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    3,
                    "Artifact 3",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    4,
                    "Artifact 4",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                )
            )
            val ownerArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    6,
                    "Artifact 8",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    7,
                    "Artifact 9",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                ),
                Artifact(
                    8,
                    "Artifact 10",
                    "Description",
                    owners = mutableListOf(user),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf()))
                )
            )
            val sharedArtifacts = listOf(
                Artifact(
                    1,
                    "Artifact 1",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 1, name = "Group 1", members = mutableListOf()))
                ),
                Artifact(
                    12,
                    "Artifact 12",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf())),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    13,
                    "Artifact 13",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf())),
                    sharedWith = mutableListOf(user)
                ),
                Artifact(
                    14,
                    "Artifact 14",
                    "Description",
                    owners = mutableListOf(),
                    groups = mutableListOf(Group(id = 2, name = "Group 2", members = mutableListOf())),
                    sharedWith = mutableListOf(user)
                )
            )
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
            Mockito.`when`(artifactRepository.findByGroups_Id(anyLong())).thenReturn(groupArtifacts)
            Mockito.`when`(artifactRepository.findByOwners_Email(anyString())).thenReturn(ownerArtifacts)
            Mockito.`when`(artifactRepository.findBySharedWith_Email(anyString())).thenReturn(sharedArtifacts)

            val foundArtifacts = artifactService.findArtifactsByOwner(email)
            val expectedArtifacts = (groupArtifacts + ownerArtifacts + sharedArtifacts).toSet().toList()
            assertEquals(expectedArtifacts.size, foundArtifacts.size)
            assertThat(foundArtifacts, containsInAnyOrder(*expectedArtifacts.toTypedArray()))
        }
    }

    @Nested
    inner class UpdateArtifact {
        @Test
        fun `it should not allow users without permission to modify the artifact`() {
            TODO()
        }

        @Test
        fun `it should allow group owners to remove their group from the artifact`() {
            TODO()
        }

        @Test
        fun `it should not allow group owners to make changes except for their group`() {
            TODO()
        }

        @Test
        fun `it should allow artifact owners to make changes to the artifact`() {
            TODO()
        }
    }

    @Nested
    inner class DeleteArtifact {
        @Test
        fun `it should not allow user's who are not the artifact's owners to delete it`() {
            TODO()
        }

        @Test
        fun `it should allow the artifact's owners to delete it`() {
            TODO()
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
                groupIDs = listOf(),
                ownerIDs = listOf(),
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
                groupIDs = listOf(),
                ownerIDs = listOf(),
                sharedWith = listOf()
            )

            val updatedUser = userRepository.findByIdOrNull(user.id)!!
            assertThat(
                updatedUser,
                hasProperty("ownedArtifacts", contains(hasProperty("id", equalTo(createdArtifact.id))))
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
                groupIDs = listOf(),
                ownerIDs = owningUsers.map(User::id),
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
                        artifacts = mutableListOf()
                    )
                ),
                groupRepository.save(
                    Group(
                        name = "Group2",
                        members = mutableListOf(user),
                        artifacts = mutableListOf()
                    )
                ),
                groupRepository.save(Group(name = "Group3", members = mutableListOf(user), artifacts = mutableListOf()))
            )
            val createdArtifact = artifactService.createArtifact(
                user.email,
                "Artifact 1",
                "Description of artifact",
                groupIDs = groups.map(Group::id),
                ownerIDs = listOf(),
                sharedWith = listOf()
            )
            groupRepository.findAllById(groups.map(Group::id)).forEach {
                assertThat(
                    it,
                    hasProperty("artifacts", contains(hasProperty("id", equalTo(createdArtifact.id))))
                )
            }
        }
    }
}

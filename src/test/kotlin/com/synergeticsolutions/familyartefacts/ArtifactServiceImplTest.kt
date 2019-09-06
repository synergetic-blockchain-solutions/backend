package com.synergeticsolutions.familyartefacts

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.beans.HasPropertyWithValue.hasProperty
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
        }

        @Test
        fun `it should not create the artifact if one of the owning users' IDs are not in the database`() {
        }

        @Test
        fun `it should not create the artifact if one of the associated group IDs do not exist`() {
        }

        @Test
        fun `it should not create the artifact if one of the shared user's IDs do not exist`() {
        }

        @Test
        fun `it should include the creator as one of the artifact owners`() {
        }

        @Test
        fun `it should include the user's personal group as one of the artifact's associated groups`() {
        }

        @Test
        fun `it should make the specified owner IDs the owners of the artifact`() {
        }

        @Test
        fun `it should make the specified group IDs associated with the artifact`() {
        }

        @Test
        fun `it should share the artifact with the specified user IDs`() {
        }
    }

    @Nested
    inner class FindArtifactsByOwner() {
        @Test
        fun `it should find all the artifacts accessible by the user`() {
        }

        @Test
        fun `it should filter the accessible artifacts by the group ID if specified`() {
        }

        @Test
        fun `it should filter the accessible artifacts by the owner ID if specified`() {
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

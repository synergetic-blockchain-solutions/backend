package com.synergeticsolutions.familyartefacts

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito

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

class ArtifactServiceImplIntegrationTest {

    @Nested
    inner class CreateArtifact {
        @Test
        fun `it should insert the artifact in the database`() {
        }

        @Test
        fun `it should update the creating user's relationship with the artifact`() {
        }

        @Test
        fun `it should update the owning user's relationship with the artifact`() {
        }

        @Test
        fun `it should update the groups's relationship with the artifact`() {
        }
    }
}

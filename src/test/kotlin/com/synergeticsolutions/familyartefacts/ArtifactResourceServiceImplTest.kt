package com.synergeticsolutions.familyartefacts

import com.synergeticsolutions.familyartefacts.dtos.ArtifactResourceMetadata
import com.synergeticsolutions.familyartefacts.entities.Artifact
import com.synergeticsolutions.familyartefacts.entities.ArtifactResource
import com.synergeticsolutions.familyartefacts.entities.Group
import com.synergeticsolutions.familyartefacts.entities.User
import com.synergeticsolutions.familyartefacts.exceptions.ActionNotAllowedException
import com.synergeticsolutions.familyartefacts.exceptions.ArtifactNotFoundException
import com.synergeticsolutions.familyartefacts.exceptions.UserNotFoundException
import com.synergeticsolutions.familyartefacts.repositories.ArtifactRepository
import com.synergeticsolutions.familyartefacts.repositories.ArtifactResourceRepository
import com.synergeticsolutions.familyartefacts.repositories.UserRepository
import com.synergeticsolutions.familyartefacts.services.ArtifactResourceServiceImpl
import java.util.Optional
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasProperty
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.data.repository.findByIdOrNull

class ArtifactResourceServiceImplTest {

    private val artifactResourceRepository: ArtifactResourceRepository =
        Mockito.mock(ArtifactResourceRepository::class.java)
    private val artifactRepository: ArtifactRepository = Mockito.mock(
        ArtifactRepository::class.java)
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)

    val artifactResourceService =
        ArtifactResourceServiceImpl(
            artifactResourceRepository,
            artifactRepository,
            userRepository
        )

    @Nested
    inner class Create {
        @Test
        fun `it should not create the resource if the creator's email does not correspond to a user in the database`() {
            Mockito.`when`(userRepository.findByEmail(anyString())).thenReturn(null)

            assertThrows<UserNotFoundException> {
                artifactResourceService.create(
                    "example@example.com",
                    1,
                    ArtifactResourceMetadata(
                        id = 0,
                        name = "Resource name",
                        description = "Resource description",
                        artifactId = 0
                    ),
                    resource = "resource".toByteArray(),
                    contentType = "text/plain"
                )
            }
        }

        @Test
        fun `it should not create the resource if the artifact ID does not exist`() {
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(
                    User(
                        name = "name",
                        email = "email",
                        password = "password",
                        privateGroup = Group(
                            name = "name",
                            description = "description"
                        )
                    )
                )
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong())).then { Optional.empty<ArtifactResource>() }
            assertThrows<ArtifactNotFoundException> {
                artifactResourceService.create(
                    "example@example.com",
                    1,
                    ArtifactResourceMetadata(
                        id = 0,
                        name = "Resource name",
                        description = "Resource description",
                        artifactId = 0
                    ),
                    resource = "resource".toByteArray(),
                    contentType = "text/plain"
                )
            }
        }

        @Test
        fun `it should associate the resource with the specified artifact`() {
            var user = User(
                name = "name",
                email = "email",
                password = "password",
                privateGroup = Group(
                    name = "name",
                    description = "description"
                )
            )
            var artifact = Artifact(
                id = 1,
                name = "Artifact",
                description = "Description",
                groups = mutableListOf(),
                owners = mutableListOf(user)
            )
            user = user.copy(ownedArtifacts = mutableListOf(artifact))
            artifact = artifact.copy(owners = mutableListOf(user))
            val resource = ArtifactResource(
                name = "Name",
                description = "description",
                artifact = artifact,
                resource = "resource".toByteArray(),
                contentType = "text/plain"
            )
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(user)
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(artifact) }
            Mockito.`when`(artifactResourceRepository.save(any<ArtifactResource>())).then { it.arguments[0] }
            Mockito.`when`(artifactResourceRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(resource) }
            artifactResourceService.create(
                "example@example.com",
                artifact.id,
                ArtifactResourceMetadata(
                    id = 0,
                    name = resource.name,
                    description = resource.description,
                    artifactId = 0
                ),
                resource = resource.resource,
                contentType = "text/plain"
            )
            val artifactResourceArgumentCaptor = ArgumentCaptor.forClass(ArtifactResource::class.java)
            Mockito.verify(artifactResourceRepository).save(artifactResourceArgumentCaptor.capture())
            assertThat(artifactResourceArgumentCaptor.value, hasProperty("artifact", equalTo(artifact)))

            val artifactArgumentCaptor = ArgumentCaptor.forClass(Artifact::class.java)
            Mockito.verify(artifactRepository).save(artifactArgumentCaptor.capture())
            assertThat(artifactArgumentCaptor.value, hasProperty("resources", hasItem(resource)))
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `it should allow artifact owners to update the resource`() {
            var user = User(
                name = "name",
                email = "email",
                password = "password",
                privateGroup = Group(
                    name = "name",
                    description = "description"
                )
            )
            var artifact = Artifact(
                id = 1,
                name = "Artifact",
                description = "Description",
                groups = mutableListOf(),
                owners = mutableListOf(user)
            )
            user = user.copy(ownedArtifacts = mutableListOf(artifact))
            val resource = ArtifactResource(
                name = "Name",
                description = "description",
                artifact = artifact,
                contentType = "text/plain",
                resource = "resource".toByteArray()
            )
            artifact = artifact.copy(owners = mutableListOf(user), resources = mutableListOf(resource))
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(user)
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(artifact) }
            Mockito.`when`(artifactResourceRepository.save(any<ArtifactResource>())).then { it.arguments[0] }
            Mockito.`when`(artifactResourceRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(resource) }
            artifactResourceService.update(
                email = "example@example.com",
                artifactId = artifact.id,
                resourceId = 1,
                metadata = ArtifactResourceMetadata(
                    id = 0,
                    name = "Updated name",
                    description = "Updated description",
                    artifactId = 0
                ),
                resource = resource.resource,
                contentType = "text/plain"
            )

            val argumentCaptor = ArgumentCaptor.forClass(ArtifactResource::class.java)
            Mockito.verify(artifactResourceRepository).save(argumentCaptor.capture())
            assertThat(
                argumentCaptor.value, allOf(
                    hasProperty("name", `is`("Updated name")),
                    hasProperty("description", `is`("Updated description"))
                )
            )
        }

        @Test
        fun `it should not allow non artifact owners to update the resource`() {
            val user = User(
                name = "name",
                email = "useremail",
                password = "password",
                privateGroup = Group(
                    name = "name",
                    description = "description"
                )
            )
            var owner = User(
                name = "owner",
                email = "owneremail",
                password = "password",
                privateGroup = Group(
                    name = "name",
                    description = "description"
                )
            )
            val artifact = Artifact(
                id = 1,
                name = "Artifact",
                description = "Description",
                groups = mutableListOf(),
                owners = mutableListOf(owner)
            )
            owner = owner.copy(ownedArtifacts = mutableListOf(artifact))
            val resource = ArtifactResource(
                name = "Name",
                description = "description",
                artifact = artifact,
                contentType = "text/plain",
                resource = "resource".toByteArray()
            )
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .then {
                    when {
                        it.arguments[0] == user.email -> user
                        it.arguments[0] == owner.email -> owner
                        else -> throw NotImplementedError("${it.arguments[0]} not handled")
                    }
                }
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(artifact) }
            Mockito.`when`(artifactResourceRepository.save(any<ArtifactResource>())).then { it.arguments[0] }
            Mockito.`when`(artifactResourceRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(resource) }
            assertThrows<ActionNotAllowedException> {
                artifactResourceService.update(
                    email = user.email,
                    artifactId = artifact.id,
                    resourceId = 1,
                    metadata = ArtifactResourceMetadata(
                        id = 0,
                        name = "Resource name",
                        description = "Resource description",
                        artifactId = 0
                    ),
                    resource = resource.resource,
                    contentType = "text/plain"
                )
            }
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `it should allow artifact owners to delete the resource`() {
            var user = User(
                name = "name",
                email = "email",
                password = "password",
                privateGroup = Group(
                    name = "name",
                    description = "description"
                )
            )
            var artifact = Artifact(
                id = 1,
                name = "Artifact",
                description = "Description",
                groups = mutableListOf(),
                owners = mutableListOf(user)
            )
            user = user.copy(ownedArtifacts = mutableListOf(artifact))

            val resource = ArtifactResource(
                name = "Name",
                description = "description",
                artifact = artifact,
                contentType = "text/plain",
                resource = "resource".toByteArray()
            )
            artifact = artifact.copy(owners = mutableListOf(user), resources = mutableListOf(resource))

            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(user)
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(artifact) }
            Mockito.`when`(artifactResourceRepository.save(any<ArtifactResource>())).then { it.arguments[0] }
            Mockito.`when`(artifactResourceRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(resource) }
            artifactResourceService.delete(
                email = "example@example.com",
                artifactId = artifact.id,
                resourceId = 1
            )

            Mockito.verify(artifactResourceRepository).delete(any())
        }

        @Test
        fun `it should not allow non artifact owners to delete the resource`() {
            val user = User(
                name = "name",
                email = "useremail",
                password = "password",
                privateGroup = Group(
                    name = "name",
                    description = "description"
                )
            )
            var owner = User(
                name = "owner",
                email = "owneremail",
                password = "password",
                privateGroup = Group(
                    name = "name",
                    description = "description"
                )
            )
            val artifact = Artifact(
                id = 1,
                name = "Artifact",
                description = "Description",
                groups = mutableListOf(),
                owners = mutableListOf(owner)
            )
            owner = owner.copy(ownedArtifacts = mutableListOf(artifact))
            val resource = ArtifactResource(
                name = "Name",
                description = "description",
                artifact = artifact,
                contentType = "text/plain",
                resource = "resource".toByteArray()
            )
            Mockito.`when`(userRepository.findByEmail(anyString()))
                .then {
                    when {
                        it.arguments[0] == user.email -> user
                        it.arguments[0] == owner.email -> owner
                        else -> throw NotImplementedError("${it.arguments[0]} not handled")
                    }
                }
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(artifact) }
            Mockito.`when`(artifactResourceRepository.save(any<ArtifactResource>())).then { it.arguments[0] }
            Mockito.`when`(artifactResourceRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(resource) }
            assertThrows<ActionNotAllowedException> {
                artifactResourceService.delete(
                    email = user.email,
                    artifactId = artifact.id,
                    resourceId = 1
                )
            }
        }

        @Test
        fun `it should update the artifact in the artifact repository to remove the resource from it`() {
            var user = User(
                name = "name",
                email = "email",
                password = "password",
                privateGroup = Group(
                    name = "name",
                    description = "description"
                )
            )
            var artifact = Artifact(
                id = 1,
                name = "Artifact",
                description = "Description",
                groups = mutableListOf(),
                owners = mutableListOf(user)
            )
            user = user.copy(ownedArtifacts = mutableListOf(artifact))

            val resource = ArtifactResource(
                name = "Name",
                description = "description",
                artifact = artifact,
                contentType = "text/plain",
                resource = "resource".toByteArray()
            )
            artifact = artifact.copy(owners = mutableListOf(user), resources = mutableListOf(resource))

            Mockito.`when`(userRepository.findByEmail(anyString()))
                .thenReturn(user)
            Mockito.`when`(artifactRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(artifact) }
            Mockito.`when`(artifactRepository.save(any<Artifact>())).then { it.arguments[0] }
            Mockito.`when`(artifactResourceRepository.save(any<ArtifactResource>())).then { it.arguments[0] }
            Mockito.`when`(artifactResourceRepository.findByIdOrNull(anyLong()))
                .then { Optional.of(resource) }
            artifactResourceService.delete(
                email = "example@example.com",
                artifactId = artifact.id,
                resourceId = 1
            )

            val argumentCaptor = ArgumentCaptor.forClass(Artifact::class.java)
            Mockito.verify(artifactRepository).save(argumentCaptor.capture())
            assertThat(argumentCaptor.value, hasProperty("resources", not(hasItem(artifact))))
        }
    }
}

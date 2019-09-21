package com.synergeticsolutions.familyartefacts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class ArtifactResourceServiceImpl(
    @Autowired
    val artifactResourceRepository: ArtifactResourceRepository,
    @Autowired
    val artifactRepository: ArtifactRepository,
    @Autowired
    val userRepository: UserRepository
) : ArtifactResourceService {

    private fun isOwner(email: String, artifactId: Long = 0, resourceId: Long = 0): Boolean =
        if (artifactId > 0 && resourceId > 0) {
            val user = userRepository.findByEmail(email) ?: throw UserNotFoundException("No user with email $email found")
            val artifact = artifactRepository.findByIdOrNull(artifactId) ?: throw ArtifactNotFoundException("Not artifact with ID $artifactId found")
            val resource = artifactResourceRepository.findByIdOrNull(resourceId) ?: throw ArtifactResourceNotFoundException("No artifact resource with ID $resourceId found")
            artifact.owners.contains(user) && artifact.resources.contains(resource)
        } else if (artifactId > 0) {
            val user = userRepository.findByEmail(email) ?: throw UserNotFoundException("No user with email $email found")
            val artifact = artifactRepository.findByIdOrNull(artifactId) ?: throw ArtifactNotFoundException("Not artifact with ID $artifactId found")
            artifact.owners.contains(user)
        } else {
            throw IllegalArgumentException("If resourceId is specified, artifactId must also be specified")
        }

    override fun findMetadataById(email: String, artifactId: Long, resourceId: Long): ArtifactResourceMetadata {
        if (!isOwner(email, artifactId = artifactId, resourceId = resourceId)) {
            throw ActionNotAllowedException("User with email $email does not have access to artifact resource $resourceId")
        }
        val resource = artifactResourceRepository.findByIdOrNull(resourceId) ?: throw ArtifactResourceNotFoundException(
            "Could not find artifact resource $resourceId"
        )
        return ArtifactResourceMetadata(
            name = resource.name,
            description = resource.description,
            tags = resource.tags
        )
    }

    override fun findResourceById(email: String, artifactId: Long, resourceId: Long): Resource {
        if (!isOwner(email, artifactId = artifactId, resourceId = resourceId)) {
            throw ActionNotAllowedException("User with email $email does not have access to artifact resource $resourceId")
        }
        val resource = artifactResourceRepository.findByIdOrNull(resourceId)
            ?: throw ArtifactResourceNotFoundException("Could not find artifact resource $resourceId")
        return Resource(resource = resource.resource, contentType = resource.contentType)
    }

    override fun update(
        email: String,
        artifactId: Long,
        resourceId: Long,
        metadata: ArtifactResourceMetadata?,
        resource: ByteArray?,
        contentType: String?
    ): ArtifactResource {
        if (!isOwner(email, artifactId = artifactId, resourceId = resourceId)) {
            throw ActionNotAllowedException("User with email $email does not have access to artifact resource $resourceId")
        }

        var resourceEntity = artifactResourceRepository.findByIdOrNull(resourceId)
            ?: throw ArtifactResourceNotFoundException("Could not find artifact resource $resourceId")

        metadata?.let {
            resourceEntity = resourceEntity.copy(
                name = it.name,
                description = it.description,
                tags = (it.tags ?: listOf()).toMutableList()
            )
        }

        resource?.let { resourceEntity = resourceEntity.copy(resource = it) }
        contentType?.let { resourceEntity = resourceEntity.copy(contentType = it) }

        return artifactResourceRepository.save(resourceEntity)
    }

    override fun delete(email: String, artifactId: Long, resourceId: Long): ArtifactResourceMetadata {
        if (!isOwner(email, artifactId = artifactId, resourceId = resourceId)) {
            throw ActionNotAllowedException("User with email $email does not have access to artifact resource $resourceId")
        }
        val resource = artifactResourceRepository.findByIdOrNull(resourceId) ?: throw ArtifactResourceNotFoundException(
            "Could not find artifact resource $resourceId"
        )
        artifactResourceRepository.delete(resource)
        artifactRepository.save(resource.artifact.copy(resources = resource.artifact.resources.filter { it.id != resource.id }.toMutableList()))
        return ArtifactResourceMetadata(
            name = resource.name,
            description = resource.description,
            tags = resource.tags
        )
    }

    override fun create(
        email: String,
        artifactId: Long,
        metadata: ArtifactResourceMetadata,
        resource: ByteArray,
        contentType: String
    ): ArtifactResource {
        if (!isOwner(email, artifactId = artifactId)) {
            throw ActionNotAllowedException("User with email $email does not have access to artifact $artifactId")
        }
        val artifact = artifactRepository.findByIdOrNull(artifactId) ?: throw ArtifactNotFoundException("Could not find artifact with ID $artifactId")
        val artifactResource = artifactResourceRepository.save(
            ArtifactResource(
                name = metadata.name,
                description = metadata.description,
                resource = resource,
                artifact = artifact,
                contentType = contentType
            )
        )
        artifactRepository.save(artifact.copy(resources = (artifact.resources + artifactResource).toMutableList()))

        return checkNotNull(
            artifactResourceRepository.findByIdOrNull(artifactResource.id),
            { "Artifact resource ${artifactResource.id} must exist as we just created it" }
        )
    }
}

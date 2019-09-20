package com.synergeticsolutions.familyartefacts

interface ArtifactResourceService {
    fun create(
        email: String,
        artifactId: Long,
        metadata: ArtifactResourceMetadata,
        resource: Resource
    ): ArtifactResource

    fun findResourceById(email: String, artifactId: Long, resourceId: Long): ByteArray
    fun findMetadataById(email: String, artifactId: Long, resourceId: Long): ArtifactResourceMetadata
    fun update(
        email: String,
        artifactId: Long,
        resourceId: Long,
        metadata: ArtifactResourceMetadata? = null,
        resource: Resource? = null
    ): ArtifactResource

    fun delete(email: String, artifactId: Long, resourceId: Long): ArtifactResourceMetadata
    fun findContactTypeById(email: String, artifactId: Long, resourceId: Long): String
}

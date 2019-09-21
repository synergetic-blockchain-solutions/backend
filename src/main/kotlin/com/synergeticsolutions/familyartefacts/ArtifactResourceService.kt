package com.synergeticsolutions.familyartefacts

interface ArtifactResourceService {
    fun create(
        email: String,
        artifactId: Long,
        metadata: ArtifactResourceMetadata,
        resource: ByteArray,
        contentType: String
    ): ArtifactResource

    fun findResourceById(email: String, artifactId: Long, resourceId: Long): Resource
    fun findMetadataById(email: String, artifactId: Long, resourceId: Long): ArtifactResourceMetadata
    fun update(
        email: String,
        artifactId: Long,
        resourceId: Long,
        metadata: ArtifactResourceMetadata? = null,
        resource: ByteArray? = null,
        contentType: String? = null
    ): ArtifactResource

    fun delete(email: String, artifactId: Long, resourceId: Long): ArtifactResourceMetadata
}

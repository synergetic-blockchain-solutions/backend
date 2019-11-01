package com.synergeticsolutions.familyartefacts.dtos

/**
 * Metadata that can be associated with an [ArtifactResource]. This class is
 * mainly used for the 'metadata' part of the multipart/form-data requests.
 *
 * @param[name] Name of the resource. Maps to [ArtifactResource.name]
 * @param[description] Description of the resource. Maps to [ArtifactResource.description]
 * @param[tags] Tags associated with the resource. Maps to [ArtifactResource.tags]
 * @param[artifactId] ID of the artifact associated with the resource
 */
data class ArtifactResourceMetadata(
    val id: Long,
    val name: String,
    val description: String,
    val tags: List<String>? = listOf(),
    val artifactId: Long
)

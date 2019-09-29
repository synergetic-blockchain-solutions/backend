package com.synergeticsolutions.familyartefacts

/**
 * [ArtifactRequest] represents a request to create an artifact.
 *
 * @param [name] Name of the artifact
 * @param [description] Description of the artifact
 * @param [owners] User IDs of the users to be made owners of the artifact
 * @param [groups] Group IDs of the groups to be associated with the artifact
 * @param [sharedWith] User IDs of the users to share the artifact with
 */
data class ArtifactRequest(
    val name: String,
    val description: String,
    val owners: List<Long>?,
    val groups: List<Long>?,
    val sharedWith: List<Long>?,
    val resources: List<Long>? = listOf(),
    val tags: List<String>? = listOf()
)

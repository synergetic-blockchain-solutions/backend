package com.synergeticsolutions.familyartefacts

interface ArtifactService {
    fun findArtifactsByOwner(
        email: String,
        groupID: Long? = null,
        ownerID: Long? = null,
        sharedID: Long? = null
    ): List<Artifact>

    fun findArtifactById(email: String, id: Long): Artifact

    fun createArtifact(
        email: String,
        name: String,
        description: String,
        ownerIDs: List<Long> = listOf(),
        groupIDs: List<Long> = listOf(),
        sharedWith: List<Long> = listOf(),
        resources: List<Long> = listOf()
    ): Artifact

    fun updateArtifact(email: String, id: Long, update: ArtifactRequest): Artifact

    fun deleteArtifact(email: String, id: Long): Artifact
}

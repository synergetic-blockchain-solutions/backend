package com.synergeticsolutions.familyartefacts

interface ArtifactService {
    fun findArtifactsByOwner(
        email: String,
        groupID: Long? = null,
        ownerID: Long? = null,
        sharedID: Long? = null
    ): List<Artifact>
    fun createArtifact(
        email: String,
        name: String,
        description: String,
        ownerIDs: List<Long> = listOf(),
        groupIDs: List<Long> = listOf(),
        sharedWith: List<Long> = listOf()
    ): Artifact
}

package com.synergeticsolutions.familyartefacts

interface ArtifactService {
    fun findArtifactsByOwner(email: String, groupID: Long?, ownerID: Long?): List<Artifact>
    fun createArtifact(
        email: String,
        name: String,
        description: String,
        groupIDs: List<Long>,
        sharedWith: List<Long>
    ): Artifact
}

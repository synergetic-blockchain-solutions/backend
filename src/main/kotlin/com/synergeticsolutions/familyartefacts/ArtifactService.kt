package com.synergeticsolutions.familyartefacts

interface ArtifactService {
    fun findArtifactsByOwner(email: String, groupID: Long?, ownerID: Long?, sharedID: Long?): List<Artifact>
    fun createArtifact(
        email: String,
        name: String,
        description: String,
        ownerIDs: List<Long>,
        groupIDs: List<Long>,
        sharedWith: List<Long>
    ): Artifact
}

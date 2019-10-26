package com.synergeticsolutions.familyartefacts.services

import com.synergeticsolutions.familyartefacts.dtos.ArtifactRequest
import com.synergeticsolutions.familyartefacts.entities.Artifact
import java.util.Date

interface ArtifactService {
    fun findArtifactsByOwner(
        email: String,
        groupID: Long? = null,
        ownerID: Long? = null,
        sharedID: Long? = null,
        tag: String? = null,
        albumID: Long? = null,
        artifactName: String?
    ): List<Artifact>

    fun findArtifactById(email: String, id: Long): Artifact

    fun createArtifact(
        email: String,
        name: String,
        description: String,
        ownerIDs: List<Long> = listOf(),
        groupIDs: List<Long> = listOf(),
        sharedWith: List<Long> = listOf(),
        resourceIDs: List<Long> = listOf(),
        albumIDs: List<Long> = listOf(),
        tags: List<String> = listOf(),
        dateTaken: Date? = null
    ): Artifact

    fun updateArtifact(email: String, id: Long, update: ArtifactRequest): Artifact

    fun deleteArtifact(email: String, id: Long): Artifact
}

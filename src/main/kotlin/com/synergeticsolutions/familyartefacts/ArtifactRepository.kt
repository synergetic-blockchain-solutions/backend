package com.synergeticsolutions.familyartefacts

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArtifactRepository : JpaRepository<Artifact, Long> {
    fun findByOwners_Email(email: String): List<Artifact>
    fun findByGroups_Id(groupID: Long): List<Artifact>
    fun findBySharedWith_Email(email: String): List<Artifact>
}

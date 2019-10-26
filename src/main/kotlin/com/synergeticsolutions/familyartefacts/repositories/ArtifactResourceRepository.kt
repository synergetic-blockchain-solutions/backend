package com.synergeticsolutions.familyartefacts.repositories

import com.synergeticsolutions.familyartefacts.entities.ArtifactResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArtifactResourceRepository : JpaRepository<ArtifactResource, Long>

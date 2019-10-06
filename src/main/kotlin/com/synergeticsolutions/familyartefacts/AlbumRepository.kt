package com.synergeticsolutions.familyartefacts

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AlbumRepository : JpaRepository<Album, Long>

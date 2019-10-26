package com.synergeticsolutions.familyartefacts.repositories

import com.synergeticsolutions.familyartefacts.entities.Album
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AlbumRepository : JpaRepository<Album, Long> {
    fun findByOwners_Email(email: String): List<Album>
    fun findByGroups_Id(groupID: Long): List<Album>
    fun findBySharedWith_Email(email: String): List<Album>
}

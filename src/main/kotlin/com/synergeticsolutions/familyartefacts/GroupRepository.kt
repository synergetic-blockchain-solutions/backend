package com.synergeticsolutions.familyartefacts

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupRepository : JpaRepository<Group, Long> {
    fun findByAdmins_Email(email: String): List<Group>
    fun findByMembers_Email(email: String): List<Group>
}

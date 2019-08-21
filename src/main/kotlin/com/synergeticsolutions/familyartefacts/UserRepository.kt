package com.synergeticsolutions.familyartefacts

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    /**
     * Check if there is a user in the repository with [email].
     */
    fun existsByEmail(email: String): Boolean
}

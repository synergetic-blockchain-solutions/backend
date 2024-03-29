package com.synergeticsolutions.familyartefacts.services

import com.synergeticsolutions.familyartefacts.dtos.UserUpdateRequest
import com.synergeticsolutions.familyartefacts.entities.User

/**
 * Interface for a service that performs user related actions. This interface should be used rather than an actual
 * implementation so we allow Spring's dependency injection to do its magic and make it easier for us to test.
 */
interface UserService {
    fun createUser(name: String, email: String, password: String): User
    fun findById(email: String, id: Long): User
    fun findUsers(email: String, filterEmail: String? = null, filterName: String? = null): List<User>
    fun findByEmail(email: String): User
    fun update(
        email: String,
        id: Long,
        metadata: UserUpdateRequest? = null,
        profilePicture: ByteArray? = null,
        contentType: String? = null
    ): User
    fun delete(email: String, id: Long): User
}

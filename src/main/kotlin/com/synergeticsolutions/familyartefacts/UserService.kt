package com.synergeticsolutions.familyartefacts

/**
 * Interface for a service that performs user related actions. This interface should be used rather than an actual
 * implementation so we allow Spring's dependency injection to do its magic and make it easier for us to test.
 */
interface UserService {
    fun createUser(userRequest: UserRequest): User
    fun findById(email: String, id: Long): User
    fun findUsers(email: String, filterEmail: String? = null, filterName: String? = null): List<User>
    fun findByEmail(email: String): User
    fun update(email: String, id: Long, metadata: UserRequest? = null, profilePicture: ByteArray? = null): User
}

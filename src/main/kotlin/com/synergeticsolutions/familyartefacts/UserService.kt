package com.synergeticsolutions.familyartefacts

/**
 * Interface for a service that performs user related actions. This interface should be used rather than an actual
 * implementation so we allow Spring's dependency injection to do its magic and make it easier for us to test.
 */
interface UserService {
    fun createUser(name: String, email: String, password: String): User
    fun findById(email: String, id: Long): User
    fun findUsers(email: String, filterEmail: String?, filterName: String?): List<User>
}

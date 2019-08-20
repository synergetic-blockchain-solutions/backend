package com.synergeticsolutions.familyartefacts

interface UserService {
    fun createUser(name: String, email: String, password: String): User
}

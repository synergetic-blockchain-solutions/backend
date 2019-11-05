package com.synergeticsolutions.familyartefacts.services

import com.synergeticsolutions.familyartefacts.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * Service for getting user's details. This is to fulfill the requirements of Spring's authentication mechanism.
 */
@Service
class
UserDetailsServiceImpl : UserDetailsService {
    @Autowired
    lateinit var userRepository: UserRepository

    /**
     * Find the user in the database by [username] and convert them to a [org.springframework.security.core.userdetails.User]
     */
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username) ?: throw UsernameNotFoundException(username)
        return org.springframework.security.core.userdetails.User(user.email, user.password, listOf())
    }
}

package com.synergeticsolutions.familyartefacts

import javax.naming.AuthenticationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

class UserAlreadyExistsException(msg: String) : AuthenticationException(msg)

@Service
class UserServiceImpl(
    @Autowired
    val userRepository: UserRepository,
    @Autowired
    val passwordEncoder: PasswordEncoder
) : UserService {

    override fun createUser(name: String, email: String, password: String): User {
        if (userRepository.existsByEmail(email)) {
            throw UserAlreadyExistsException("User already exists with email $email")
        }
        val encPassword = passwordEncoder.encode(password)
        val user = User(name = name, email = email, password = encPassword)
        return userRepository.save(user)
    }
}

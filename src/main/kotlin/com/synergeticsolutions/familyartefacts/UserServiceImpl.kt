package com.synergeticsolutions.familyartefacts

import javax.naming.AuthenticationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

class UserAlreadyExistsException(msg: String) : AuthenticationException(msg)

/**
 * [UserServiceImpl] is an implementation of [UserService] that interacts with the [UserRepository] abstracting many
 * of the details of this interaction away.
 */
@Service
class UserServiceImpl(
    @Autowired
    val userRepository: UserRepository,
    @Autowired
    val groupRepository: GroupRepository,
    @Lazy
    @Autowired
    val passwordEncoder: PasswordEncoder
) : UserService {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * [createUser] creates a user with the provided [name], [email] and [password].
     *
     * Before the user is created, it is checked that they do not already exist in the database. If they do then
     * the [UserAlreadyExistsException] is thrown, resulting in a 5xx error being returned by the calling controller.
     * Additionally, the [password] is hashed using [passwordEncoder].
     *
     * @param [name] Name of the user being created.
     * @param [email] Email of the user being created.
     * @param [password] Password of the user being created.
     * @return [User] created in the [userRepository].
     * @throws [UserAlreadyExistsException] when trying to create a user with an [email] that already exists in the repository.
     */
    override fun createUser(name: String, email: String, password: String): User {
        if (userRepository.existsByEmail(email)) {
            throw UserAlreadyExistsException("User already exists with email $email")
        }
        logger.info("No user with email '$email' was found, creating user")
        val encPassword = passwordEncoder.encode(password)
        val group = Group(name = "$name's Personal Group", members = listOf())
        val user = User(name = name, email = email, password = encPassword, groups = listOf(group))
        group.members = listOf(user)
        val savedUser = userRepository.save(user)
        return savedUser
    }
}

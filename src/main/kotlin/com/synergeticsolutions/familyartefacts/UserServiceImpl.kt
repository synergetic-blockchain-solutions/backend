package com.synergeticsolutions.familyartefacts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import javax.naming.AuthenticationException

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

        // First we need to create a user with no groups so they're saved in the database. Then we create their personal
        // group with the user as a member of it so that group exists. We then update the existing user with the create
        // we just created.
        val user = userRepository.save(User(name = name, email = email, password = encPassword))
        val group = groupRepository.save(Group(name = "$name's Personal Group", members = mutableListOf(user)))
        user.groups.add(group)
        return userRepository.save(user)
    }
}

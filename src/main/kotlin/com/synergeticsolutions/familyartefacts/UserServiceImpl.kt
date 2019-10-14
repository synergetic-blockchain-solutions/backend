package com.synergeticsolutions.familyartefacts

import javax.naming.AuthenticationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
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

        val group = groupRepository.save(
            Group(
                name = "$name's Personal Group",
                description = "$name's Personal Group",
                members = mutableListOf(),
                admins = mutableListOf()
            )
        )
        val user = userRepository.save(
                User(
                        name = name,
                        email = email,
                        password = encPassword,
                        privateGroup = group,
                        groups = mutableListOf(group),
                        ownedGroups = mutableListOf(group),
                        ownedAlbums = mutableListOf(),
                        sharedAlbums = mutableListOf()
                )
        )
        group.members.add(user)
        group.admins.add(user)
        val updatedGroup = groupRepository.save(group)
        val updatedUser = userRepository.findByEmail(user.email)!!
        logger.debug("Created user: $updatedUser")
        logger.debug("Personal group: $updatedGroup")
        return updatedUser
    }

    override fun findById(email: String, id: Long): User {
        val user =
            userRepository.findByEmail(email) ?: throw UserNotFoundException("Could not find user with email $email")
        logger.info("Retrieving user $id for user ${user.id}")
        val foundUser =
            userRepository.findByIdOrNull(id) ?: throw UserNotFoundException("Could not find user with id $id")
        logger.info("Found user $foundUser")
        return foundUser
    }

    override fun findUsers(email: String, filterEmail: String?, filterName: String?): List<User> {
        val user =
            userRepository.findByEmail(email) ?: throw UserNotFoundException("Could not find user with email $email")
        logger.info("Finding users with email=$filterEmail and name=$filterName for user ${user.id}")
        var users = userRepository.findAll()
        if (filterEmail != null) {
            users = users.filter { it.email.startsWith(filterEmail) }
        }

        if (filterName != null) {
            users = users.filter { it.name.startsWith(filterName) }
        }

        logger.info("Found ${users.size} using filter email=$filterName and name=$filterName")
        return users
    }

    override fun findByEmail(email: String): User {
        return userRepository.findByEmail(email) ?: throw UserNotFoundException("Could not find user with email $email")
    }

    override fun update(email: String, id: Long, metadata: UserUpdateRequest?, profilePicture: ByteArray?): User {
        var user =
            userRepository.findByEmail(email) ?: throw UserNotFoundException("Could not find user with email $email")
        if (user.id != id) {
            logger.warn("User ${user.id} attempted to update user $id")
            throw ActionNotAllowedException("Users can only update themselves: User ${user.id} tried to update user $id")
        }

        metadata?.let {
            var password = user.password
            if (it.password != null) {
                password = passwordEncoder.encode(it.password)
            }
            user = user.copy(
                name = it.name,
                email = it.email,
                password = password
            )
        }

        profilePicture?.let {
            user = user.copy(
                image = it
            )
        }

        return userRepository.save(user)
    }

    override fun delete(email: String, id: Long): User {
        val user = userRepository.findByEmail(email) ?: throw UserNotFoundException("Could not find user with email $email")
        if (user.id != id) {
            logger.warn("User ${user.id} attempted to delete user $id")
            throw ActionNotAllowedException("Users can only delete themselves: user ${user.id} tried to delete user $id")
        }
        userRepository.delete(user)
        return user
    }
}

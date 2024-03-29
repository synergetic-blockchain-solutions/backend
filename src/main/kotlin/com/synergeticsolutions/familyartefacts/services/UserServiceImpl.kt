package com.synergeticsolutions.familyartefacts.services

import com.synergeticsolutions.familyartefacts.dtos.UserUpdateRequest
import com.synergeticsolutions.familyartefacts.entities.Group
import com.synergeticsolutions.familyartefacts.entities.User
import com.synergeticsolutions.familyartefacts.exceptions.ActionNotAllowedException
import com.synergeticsolutions.familyartefacts.exceptions.UserNotFoundException
import com.synergeticsolutions.familyartefacts.repositories.GroupRepository
import com.synergeticsolutions.familyartefacts.repositories.UserRepository
import javax.naming.AuthenticationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
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

    /**
     * Find a user by their ID.
     *
     * [email] is not use in this method but is included to maintain consistency with other methods.
     *
     * @param email Email of the requesting user
     * @param id ID of the user to be retrieved
     * @return The user entity with [id]
     */
    override fun findById(email: String, id: Long): User {
        val user =
            userRepository.findByEmail(email) ?: throw UserNotFoundException(
                "Could not find user with email $email"
            )
        logger.info("Retrieving user $id for user ${user.id}")
        val foundUser =
            userRepository.findByIdOrNull(id) ?: throw UserNotFoundException(
                "Could not find user with id $id"
            )
        logger.info("Found user $foundUser")
        return foundUser
    }

    /**
     * Find a users by their email or name.
     *
     * This function does not require a string match on the [filterEmail] or [filterName], it just checks that
     * they match the first part of the email or name, respectively.
     *
     * @return A collection of users matching the criteria
     */
    override fun findUsers(email: String, filterEmail: String?, filterName: String?): List<User> {
        val user =
            userRepository.findByEmail(email) ?: throw UserNotFoundException(
                "Could not find user with email $email"
            )
        logger.info("Finding users with email=$filterEmail and name=$filterName for user ${user.id}")
        var users = userRepository.findAll()
        if (filterEmail != null) {
            users = users.filter { it.email.startsWith(filterEmail, ignoreCase = true) }
        }

        if (filterName != null) {
            users = users.filter { it.name.startsWith(filterName, ignoreCase = true) }
        }

        logger.info("Found ${users.size} using filter email=$filterName and name=$filterName")
        return users
    }

    /**
     * Find a user by [email]
     *
     * @return A [User] entity with [email]
     */
    override fun findByEmail(email: String): User {
        return userRepository.findByEmail(email) ?: throw UserNotFoundException(
            "Could not find user with email $email"
        )
    }

    /**
     * Update a by [id]
     *
     * Users can only update themselves.
     *
     * @return Updated [User] entity
     */
    override fun update(
        email: String,
        id: Long,
        metadata: UserUpdateRequest?,
        profilePicture: ByteArray?,
        contentType: String?
    ): User {
        var user =
            userRepository.findByEmail(email) ?: throw UserNotFoundException(
                "Could not find user with email $email"
            )
        if (user.id != id) {
            logger.warn("User ${user.id} attempted to update user $id")
            throw ActionNotAllowedException("Users can only update themselves: User ${user.id} tried to update user $id")
        }

        metadata?.let {
            var password = user.password
            if (it.password != null) {
                password = passwordEncoder.encode(it.password)
            }

            if ((it.email != user.email) && userRepository.findByEmail(it.email) != null) {
                throw UserAlreadyExistsException("Cannot update user ${user.id} email to ${it.email} as there is already as user with that email")
            }

            user = user.copy(name = it.name, email = it.email, password = password)
        }

        profilePicture?.let { user = user.copy(image = it) }
        contentType?.let { user = user.copy(contentType = it) }

        return userRepository.save(user)
    }

    /**
     * Delete the user.
     *
     * Users can only delete themselves.
     */
    override fun delete(email: String, id: Long): User {
        val user = userRepository.findByEmail(email) ?: throw UserNotFoundException(
            "Could not find user with email $email"
        )
        if (user.id != id) {
            logger.warn("User ${user.id} attempted to delete user $id")
            throw ActionNotAllowedException("Users can only delete themselves: user ${user.id} tried to delete user $id")
        }
        userRepository.delete(user)
        return user
    }
}

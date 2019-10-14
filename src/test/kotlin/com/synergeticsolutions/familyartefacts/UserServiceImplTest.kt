package com.synergeticsolutions.familyartefacts

import java.util.Optional
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.junit.jupiter.SpringExtension

class UserServiceImplUnitTest {
    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val passwordEncoder: PasswordEncoder = Mockito.mock(PasswordEncoder::class.java)
    private val groupRepository: GroupRepository = Mockito.mock(GroupRepository::class.java)
    private val userService: UserService = UserServiceImpl(userRepository, groupRepository, passwordEncoder)

    @Test
    fun `it should encrypt the password before saving it in the user repository`() {
        val encodedPassword = "encodedSecret"
        val user = User(
                name = "name",
                email = "email",
                password = "secret",
                privateGroup = Group(
                        2,
                        "Group 2",
                        description = "description",
                        members = mutableListOf(),
                        admins = mutableListOf()
                )
        )
        Mockito.`when`(userRepository.save(any<User>())).thenReturn(user.copy(id = 1))
        Mockito.`when`(userRepository.findByEmail(anyString())).then { user }
        // Mockito.`when`(userRepository.findByIdOrNull(anyLong())).then { user }
        Mockito.`when`(groupRepository.save(any<Group>())).thenReturn(
                Group(
                        1,
                        "group",
                        description = "description",
                        members = mutableListOf(),
                        admins = mutableListOf()))
        Mockito.`when`(passwordEncoder.encode(anyString())).thenReturn(encodedPassword)
        val inOrder = Mockito.inOrder(passwordEncoder, userRepository)

        userService.createUser(user.name, user.email, user.password)

        // Ensure password is encrypted first
        inOrder.verify(passwordEncoder).encode("secret")
        // Ensure user is saved to the repository after the password has been encrypted
        inOrder.verify(userRepository)
                .save(argThat { it.javaClass == User::class.java && it.password == encodedPassword })
    }

    @Test
    fun `it should not allow the creation of users with emails that already exist`() {
        val user = User(
                name = "name",
                email = "email",
                password = "secret",
                privateGroup = Group(
                        2,
                        "Group 2",
                        description = "description",
                        members = mutableListOf(),
                        admins = mutableListOf()
                )
        )
        Mockito.`when`(userRepository.existsByEmail(user.email)).thenReturn(true)
        assertThrows(UserAlreadyExistsException::class.java) {
            userService.createUser(
                    user.name,
                    user.email,
                    user.password
            )
        }
    }

    @Nested
    inner class FindByEmail {
        @Test
        fun `it should find the user by the specified email`() {
            val email = "example@example.com"
            val user = User(
                name = "name",
                email = email,
                password = "secret",
                privateGroup = Group(
                    2,
                    "Group 2",
                    description = "description",
                    members = mutableListOf(),
                    admins = mutableListOf()
                )
            )
            Mockito.`when`(userRepository.findByEmail(anyString())).thenReturn(user)
            assertEquals(user, userService.findByEmail(email))
        }
    }

    @Nested
    inner class FindById {

        @Test
        fun `it should find the user by the specified ID`() {
            val email = "example@example.com"
            val user = User(
                id = 2,
                name = "name",
                email = "example2@example.com",
                password = "secret",
                privateGroup = Group(
                    2,
                    "Group 2",
                    description = "description",
                    members = mutableListOf(),
                    admins = mutableListOf()
                )
            )
            val user2 = User(
                name = "name",
                email = email,
                password = "secret",
                privateGroup = Group(
                    2,
                    "Group 2",
                    description = "description",
                    members = mutableListOf(),
                    admins = mutableListOf()
                )
            )
            Mockito.`when`(userRepository.findById(anyLong())).thenReturn(Optional.of(user))
            Mockito.`when`(userRepository.findByEmail(anyString())).thenReturn(user2)
            assertEquals(user, userService.findById(email, 2))
        }
    }

    @Nested
    inner class FindUsers {
        val email = "example1@example.com"
        private val user = User(7, "user", email, "password", privateGroup = Group(2, "Group 2", "description", mutableListOf(), mutableListOf()))
        private val users = listOf(
            User(1, "user1", "example1@example.com", "password", privateGroup = Group(2, "Group 2", "description", mutableListOf(), mutableListOf())),
            User(2, "user2", "example2@example.com", "password", privateGroup = Group(2, "Group 2", "description", mutableListOf(), mutableListOf())),
            User(3, "user2", "example3@example.com", "password", privateGroup = Group(2, "Group 2", "description", mutableListOf(), mutableListOf())),
            User(4, "user3", "example4@example.com", "password", privateGroup = Group(2, "Group 2", "description", mutableListOf(), mutableListOf())),
            User(5, "user4", "example5@example.com", "password", privateGroup = Group(2, "Group 2", "description", mutableListOf(), mutableListOf())),
            User(6, "user5", "example6@example.com", "password", privateGroup = Group(2, "Group 2", "description", mutableListOf(), mutableListOf()))
            )

        @BeforeEach
        fun setup() {
            Mockito.`when`(userRepository.findAll()).thenReturn(users)
            Mockito.`when`(userRepository.findByEmail(email)).thenReturn(user)
        }

        @Test
        fun `it should get all users if no filters are specified`() {
            val foundUsers = userService.findUsers(email)
            assertEquals(users, foundUsers)
        }

        @Test
        fun `it should filter users by the specified filter name`() {
            val foundUsers = userService.findUsers(email, filterName = "user2")
            assertEquals(users.filter { it.name == "user2" }, foundUsers)
        }

        @Test
        fun `it should filter users by the specified filter email`() {
            val foundUsers = userService.findUsers(email, filterEmail = email)
            assertEquals(users.filter { it.email == email }, foundUsers)
        }

        @Test
        fun `it should filter users by the specified filter name and filter email`() {
            val foundUsers = userService.findUsers(email, filterName = "user2", filterEmail = "example2@example.com")
            assertEquals(users.filter { it.name == "user2" && it.email == "example2@example.com" }, foundUsers)
        }
    }

    @Nested
    inner class UpdateUser {
        val user1 = User(1, "user1", "example1@example.com", "password", privateGroup = Group(2, "Group 2", "description", mutableListOf(), mutableListOf()))
        val user2 = User(2, "user2", "example2@example.com", "password", privateGroup = Group(2, "Group 2", "description", mutableListOf(), mutableListOf()))

        @BeforeEach
        fun beforeEach() {
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when {
                    it.arguments[0] == user1.email -> user1
                    it.arguments[0] == user2.email -> user2
                    else -> throw NotImplementedError("${it.arguments[0]} not handled")
                }
            }
            Mockito.`when`(userRepository.save(any<User>())).then { it.arguments[0] }
        }

        @Test
        fun `it should not allow users to update other users`() {
            assertThrows(ActionNotAllowedException::class.java) {
                userService.update(
                    user1.email,
                    user2.id,
                    metadata = UserUpdateRequest("test", "test", "test"),
                    contentType = null
                )
            }
        }

        @Test
        fun `it should only update the metadata if the image is null`() {
            val updatedUser = userService.update(
                user1.email,
                user1.id,
                metadata = UserUpdateRequest("test", user1.email, null),
                contentType = null
            )
            assertThat(updatedUser, allOf(
                hasProperty("id", `is`(user1.id)),
                hasProperty("name", `is`("test")),
                hasProperty("email", `is`(user1.email)),
                hasProperty("password", `is`(user1.password))
            ))
            Mockito.verify(userRepository).save(updatedUser)
        }

        @Test
        fun `it should only update the image if the image is null`() {
            val updatedUser = userService.update(
                user1.email,
                user1.id,
                profilePicture = "profilePicture".toByteArray(),
                contentType = null
            )
            assertThat(updatedUser, allOf(
                hasProperty("id", `is`(user1.id)),
                hasProperty("name", `is`(user1.name)),
                hasProperty("email", `is`(user1.email)),
                hasProperty("password", `is`(user1.password)),
                hasProperty("image", `is`("profilePicture".toByteArray()))
            ))
            Mockito.verify(userRepository).save(updatedUser)
        }

        @Test
        fun `it should update both the metadata and the image if they're both specified`() {
            val updatedUser = userService.update(
                user1.email, user1.id,
                metadata = UserUpdateRequest("test", user1.email, null),
                profilePicture = "profilePicture".toByteArray(),
                contentType = null
            )
            assertThat(updatedUser, allOf(
                hasProperty("id", `is`(user1.id)),
                hasProperty("name", `is`("test")),
                hasProperty("password", `is`(user1.password)),
                hasProperty("image", `is`("profilePicture".toByteArray()))
            ))
            Mockito.verify(userRepository).save(updatedUser)
        }
    }

    @Nested
    inner class DeleteUser {
        val user1 = User(1, "user1", "example1@example.com", "password", privateGroup = Group(2, "Group 2", "description", mutableListOf(), mutableListOf()))
        val user2 = User(2, "user2", "example2@example.com", "password", privateGroup = Group(2, "Group 2", "description", mutableListOf(), mutableListOf()))

        @BeforeEach
        fun beforeEach() {
            Mockito.`when`(userRepository.findByEmail(anyString())).then {
                when {
                    it.arguments[0] == user1.email -> user1
                    it.arguments[0] == user2.email -> user2
                    else -> throw NotImplementedError("${it.arguments[0]} not handled")
                }
            }
            Mockito.`when`(userRepository.delete(any<User>())).then { null }
        }

        @Test
        fun `it should allow users to delete themselves`() {
            userService.delete(user1.email, user1.id)
            Mockito.verify(userRepository).delete(user1)
        }

        @Test
        fun `it should not allow users to delete other users`() {
            assertThrows(ActionNotAllowedException::class.java) {
                userService.delete(user1.email, user2.id)
            }
        }
    }
}

@SpringBootTest
@ExtendWith(SpringExtension::class)
class UserServiceImplIntegrationTest {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var groupRepository: GroupRepository

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var testUtilsService: TestUtilsService

    @BeforeEach
    fun clearRepository() {
        testUtilsService.clearDatabase()
    }

    @Test
    fun `it should insert the user in the user repository`() {
        val createdUser = userService.createUser("name", "example@example.com", "password")
        val foundUser = userRepository.findByIdOrNull(createdUser.id)!!
        assertEquals(createdUser.id, foundUser.id)
    }

    @Test
    fun `it should create a private group where the user is member and admin`() {
        val createdUser = userService.createUser("name", "example2@example.com", "password")
        val foundUser = userRepository.findByIdOrNull(createdUser.id)!!
        val foundGroup = groupRepository.findByIdOrNull(createdUser.privateGroup.id)!!
        assertNotNull(createdUser.privateGroup)
        assertNotNull(foundUser.privateGroup)
        assertEquals(1, foundGroup.members.size)
        assertEquals(1, foundGroup.admins.size)
        assertEquals(createdUser.id, foundGroup.members.first().id)
        assertEquals(createdUser.id, foundGroup.admins.first().id)
    }
}

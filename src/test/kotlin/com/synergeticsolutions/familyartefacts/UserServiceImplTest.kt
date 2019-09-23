package com.synergeticsolutions.familyartefacts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
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

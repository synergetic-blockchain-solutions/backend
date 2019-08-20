package com.synergeticsolutions.familyartefacts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
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
    private val userService: UserService = UserServiceImpl(userRepository, passwordEncoder)

    @Test
    fun `it should encrypt the password before saving it in the user repository`() {
        val encodedPassword = "encodedSecret"
        val user = User(name = "name", email = "email", password = "secret")
        Mockito.`when`(userRepository.save(any<User>())).thenReturn(user.copy(id = 1))
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
        val user = User(name = "name", email = "email", password = "secret")
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
    lateinit var userService: UserService

    @Test
    fun `it should insert the user in the user repository`() {
        val createdUser = userService.createUser("name", "example@example.com", "password")
        val foundUser = userRepository.findByIdOrNull(createdUser.id)
        assertNotNull(foundUser)
        assertEquals(createdUser.email, foundUser!!.email)
    }
}

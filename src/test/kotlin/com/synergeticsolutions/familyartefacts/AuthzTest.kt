package com.synergeticsolutions.familyartefacts

import com.synergeticsolutions.familyartefacts.entities.User
import com.synergeticsolutions.familyartefacts.repositories.UserRepository
import com.synergeticsolutions.familyartefacts.security.TokenServiceImpl
import com.synergeticsolutions.familyartefacts.services.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "auth.jwtSecret=017af93e3bacee3729b3eb03765f83d7885d338ff53fd8d2bb6d30adec26f29fa640f5640bbfd16725c23731136b8ba7f6d9e7446ac32587ae056885231cb109",
    "auth.jwtLifetime=86400000",
    "auth.audience=test",
    "auth.issuer=test"
])
@AutoConfigureWebTestClient
class AuthzTest {
    @Autowired
    lateinit var client: WebTestClient
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var testUtilsService: TestUtilsService

    @Value("\${auth.jwtSecret}")
    lateinit var realJwtSecret: String
    @Value("\${auth.jwtLifetime}")
    val realJwtLifetime: Long? = null
    @Value("\${auth.audience}")
    lateinit var audience: String
    @Value("\${auth.issuer}")
    lateinit var issuer: String

    lateinit var user: User

    @BeforeEach
    fun beforeEach() {
        testUtilsService.clearDatabase()
        user = userService.createUser("name", "example@example.com", "password")
    }

    @Test
    fun `it should return 401 unauthorized for malformed tokens`() {
        client.get().uri("/group")
            .header(HttpHeaders.AUTHORIZATION, "Bearer malformed")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `it should return 401 unauthorized for expired tokens`() {
        val expiredTokenService = TokenServiceImpl(
            jwtSecret = realJwtSecret,
            jwtLifetime = -100,
            issuer = issuer,
            audience = audience,
            userRepository = userRepository
        )
        val expiredToken = expiredTokenService.createToken(user.email, listOf())
        client.get().uri("/group")
            .header(HttpHeaders.AUTHORIZATION, "Bearer malformed")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `it should return 401 unauthorized for incorrectly signed tokens`() {
        val incorrectlySignedTokenService = TokenServiceImpl(
            jwtSecret = "wrong_secret".repeat(42),
            jwtLifetime = realJwtLifetime!!,
            issuer = issuer,
            audience = audience,
            userRepository = userRepository
        )
        val incorrectlySignedToken = incorrectlySignedTokenService.createToken(user.email, listOf())

        client.get().uri("/group")
            .header(HttpHeaders.AUTHORIZATION, "Bearer malformed")
            .exchange()
            .expectStatus().isUnauthorized
    }
}

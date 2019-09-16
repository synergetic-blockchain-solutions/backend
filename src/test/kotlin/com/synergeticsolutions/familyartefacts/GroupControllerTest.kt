package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GroupControllerTest {
    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var groupRepository: GroupRepository

    @Autowired
    lateinit var userService: UserService

    @Autowired
    private lateinit var testUtils: TestUtilsService

    val email: String = "example@example.com"
    val password: String = "password"
    lateinit var token: String

    fun getToken(userEmail: String, userPassword: String): String {
        val resp = client.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(LoginRequest(email = userEmail, password = userPassword))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .returnResult()
                .responseBody!!
        return ObjectMapper().registerKotlinModule().readValue<LoginResponse>(resp).token
    }

    @BeforeEach
    fun beforeEach() {
        testUtils.clearDatabase()
        val user = userService.createUser("name", email, password)
        userRepository.save(user)
        token = getToken(email, password)
    }

    @Test
    fun `it should create group`() {
        val groupRequest = GroupRequest(
                name = "Group 1",
                description = "Group description",
                members = listOf(),
                admins = listOf()
        )
        val response = client.post()
                .uri("/group")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .syncBody(groupRequest)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .returnResult()
                .responseBody!!
        val returnedGroup = ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(response))
        assertTrue((returnedGroup["id"] as Int) > 0)
        assertEquals(returnedGroup["name"] as String, groupRequest.name)
        assertEquals(returnedGroup["description"] as String, groupRequest.description)

        val createdGroup = groupRepository.findByIdOrNull((returnedGroup["id"] as Int).toLong())!!
        assertEquals(mutableListOf(userRepository.findByEmail(email)!!.id), createdGroup.admins.map(User::id))
    }

    @Test
    fun `it should allow admins to delete the group`() {
        val groupRequest = GroupRequest(
                name = "Group 1",
                description = "Group description",
                members = listOf(),
                admins = listOf()
        )
        val groupResponse = client.post()
                .uri("/group")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .syncBody(groupRequest)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .returnResult()
                .responseBody!!
        val returnedGroup =
                ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(groupResponse))
        client.delete()
                .uri("/group/${returnedGroup["id"]}")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .returnResult()
                .responseBody!!
        assertFalse(groupRepository.existsById((returnedGroup["id"] as Int).toLong()))
        val user = userRepository.findByEmail(email)!!
        val groupId = (returnedGroup["id"] as Int).toLong()
        assertThat(user.ownedGroups, not(hasItems(hasProperty("id", `is`(groupId)))))
    }

    @Test
    fun `it should not allow members to delete the group`() {
        val groupRequest = GroupRequest(
                name = "Group 1",
                description = "Description",
                members = listOf(),
                admins = listOf()
        )

        val groupResponse = client.post()
                .uri("/group")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .syncBody(groupRequest)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .returnResult()
                .responseBody!!
        val returnedGroup =
                ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(groupResponse))

        userService.createUser("user 2", "exampl2@example.com", "password")
        val altToken = getToken("exampl2@example.com", "password")
        client.delete()
                .uri("/group/${returnedGroup["id"]}")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $altToken")
                .exchange()
                .expectStatus().isForbidden
    }
}

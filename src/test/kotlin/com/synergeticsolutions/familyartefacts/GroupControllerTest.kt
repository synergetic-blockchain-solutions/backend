package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasProperty
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
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
    lateinit var groupService: GroupService

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

    @Nested
    inner class GetGroup {
        @Test
        fun `it should get all the groups accessible by the user`() {
            val groups = listOf(
                groupService.createGroup(email = email, groupName = "Group1", description = "description1", memberIDs = listOf(), adminIDs = listOf()),
                groupService.createGroup(email = email, groupName = "Group2", description = "description2", memberIDs = listOf(), adminIDs = listOf()),
                groupService.createGroup(email = email, groupName = "Group3", description = "description3", memberIDs = listOf(), adminIDs = listOf())
            )
            client.get()
                    .uri("/group")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(hasSize<Group>(4))
                    // .jsonPath("$").value(containsInAnyOrder(groups.map { hasEntry("id", it.id.toInt()) }))
        }

        @Test
        fun `it should get all groups where the user is the admin`() {
            val user = userRepository.findByEmail(email)!!
            userService.createUser("name2", "example2@example.com", "password")
            val groups = listOf(
                    groupService.createGroup(email = email, groupName = "Group1", description = "description1", memberIDs = listOf(), adminIDs = listOf()),
                    groupService.createGroup(email = email, groupName = "Group2", description = "description2", memberIDs = listOf(), adminIDs = listOf())
            )
            client.get()
                    .uri("/group?admin=${user.id}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$").isArray
                    .jsonPath("$").value(hasSize<Group>(3))
        }
    }

    @Nested
    inner class CreateGroup {
        @Test
        fun `it should create the group`() {
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
    }

    @Nested
    inner class UpdateGroup {
        @Test
        fun `it should allow admins to update the group`() {
            val groupRequest = GroupRequest(
                    name = "Group 1",
                    description = "Description",
                    members = listOf(),
                    admins = listOf()
            )
            val createGroupResponse = client.post()
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
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createGroupResponse))
            @Suppress("UNCHECKED_CAST")
            val updateGroupRequest =
                    GroupRequest(
                            name = returnedGroup["name"] as String,
                            description = returnedGroup["description"] as String,
                            admins = (returnedGroup["admins"] as List<Map<String, Any>>).map { (it["id"] as Int).toLong() },
                            members = (returnedGroup["members"] as List<Map<String, Any>>).map{ (it["id"] as Int).toLong() }
                    )
            val updateGroupResponse = client.put()
                    .uri("/group/${returnedGroup["id"]}")
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .syncBody(updateGroupRequest)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .returnResult()
                    .responseBody!!
            val updatedGroupResponse =
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(updateGroupResponse)
            assertEquals((updatedGroupResponse["id"] as Int).toLong(), (returnedGroup["id"] as Int).toLong())
            assertEquals(updatedGroupResponse["name"] as String, groupRequest.name)
            assertEquals(updatedGroupResponse["description"] as String, groupRequest.description)

            val updatedGroup =
                    groupRepository.findByIdOrNull((updatedGroupResponse["id"] as Int).toLong())!!
            assertEquals(
                    mutableListOf(userRepository.findByEmail(email)!!.id),
                    updatedGroup.admins.map(User::id)
            )
            assertEquals(
                    mutableListOf(userRepository.findByEmail(email)!!.id),
                    updatedGroup.members.map(User::id)
            )
        }

        @Test
        fun `it should allow admins to add group image`() {
            val groupRequest = GroupRequest(
                    name = "Group 1",
                    description = "Description",
                    members = listOf(),
                    admins = listOf()
            )
            val createGroupResponse = client.post()
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
                    ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createGroupResponse))
            @Suppress("UNCHECKED_CAST")
            val updateGroupResponse = client.put()
                    .uri("/group/${returnedGroup["id"]}/image")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .contentType(MediaType.IMAGE_PNG)
                    .syncBody(ClassPathResource("test-image2.png"))
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()

            val updatedGroup =
                    groupRepository.findByIdOrNull((returnedGroup["id"] as Int).toLong())!!
            assertTrue(ClassPathResource("test-image2.png").file.readBytes().contentEquals(updatedGroup.image))
        }
    }

    @Nested
    inner class DeleteGroup {
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
}

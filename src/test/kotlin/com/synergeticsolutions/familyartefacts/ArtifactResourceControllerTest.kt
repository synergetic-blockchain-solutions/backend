package com.synergeticsolutions.familyartefacts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasProperty
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
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@Suppress("UNCHECKED_CAST")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ArtifactResourceControllerTest(
    @Autowired
    val client: WebTestClient,
    @Autowired
    val testUtils: TestUtilsService,

    @Autowired
    val userService: UserService,
    @Autowired
    val groupService: GroupService,
    @Autowired
    val artifactService: ArtifactService,
    @Autowired
    val artifactResourceService: ArtifactResourceService,

    @Autowired
    val artifactResourceRepository: ArtifactResourceRepository,
    @Autowired
    val artifactRepository: ArtifactRepository
) {
    val email = "example@example.com"
    val email2 = "example2@example.com"
    val password = "password"
    lateinit var user1: User
    lateinit var user2: User
    lateinit var user1Token: String
    lateinit var user2Token: String

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

    fun createResource(
        artifactOwner: User = user1,
        artifactOwnerToken: String = user1Token,
        resourceCreatorToken: String = user1Token
    ): Map<String, Any> {
        val artifactRequest = ArtifactRequest(
            name = "Artifact 1",
            description = "Description",
            owners = listOf(artifactOwner.id),
            groups = listOf(),
            sharedWith = listOf()
        )
        val createArtifactResponse = client.post()
            .uri("/artifact")
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $artifactOwnerToken")
            .syncBody(artifactRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .returnResult()
            .responseBody!!
        val returnedArtifact =
            ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createArtifactResponse))

        val metadata = ArtifactResourceMetadata(name = "Resource name", description = "Resource description")
        val multipartDataRequest = MultipartBodyBuilder()
        multipartDataRequest.part("metadata", metadata, MediaType.APPLICATION_JSON_UTF8)
        multipartDataRequest.part(
            "resource",
            ClassPathResource("test-image.jpg"),
            MediaType.IMAGE_JPEG
        )

        val createResourceResponse = client.post()
            .uri("/artifact/${returnedArtifact["id"]}/resource")
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $resourceCreatorToken")
            .body(BodyInserters.fromMultipartData(multipartDataRequest.build()))
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .returnResult()
            .responseBody!!
        return ObjectMapper().registerKotlinModule().readValue(String(createResourceResponse))
    }

    @BeforeEach
    fun beforeEach() {
        testUtils.clearDatabase()
        user1 = userService.createUser("name", email, password)
        user1Token = getToken(user1.email, password)
        user2 = userService.createUser("name2", email2, password)
        user2Token = getToken(user2.email, password)
    }

    @Nested
    inner class CreateArtifactResource {
        @Test
        fun `it should create the artifact resource and associate it with the artifact`() {
            val returnedResource = createResource()
            assertTrue(artifactResourceRepository.existsById((returnedResource["id"] as Int).toLong()))
            val resource = artifactResourceRepository.findByIdOrNull((returnedResource["id"] as Int).toLong())!!
            assertTrue(ClassPathResource("test-image.jpg").file.readBytes().contentEquals(resource.resource))
        }

        @Test
        fun `it should not allow non artifact owners to create a resource`() {
            val artifactRequest = ArtifactRequest(
                name = "Artifact 1",
                description = "Description",
                owners = listOf(user1.id),
                groups = listOf(),
                sharedWith = listOf()
            )
            val createArtifactResponse = client.post()
                .uri("/artifact")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user1Token")
                .syncBody(artifactRequest)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .returnResult()
                .responseBody!!
            val returnedArtifact =
                ObjectMapper().registerKotlinModule().readValue<Map<String, Any>>(String(createArtifactResponse))

            val metadata = ArtifactResourceMetadata(name = "Resource name", description = "Resource description")
            val multipartDataRequest = MultipartBodyBuilder()
            multipartDataRequest.part("metadata", metadata, MediaType.APPLICATION_JSON_UTF8)
            multipartDataRequest.part(
                "resource",
                ClassPathResource("test-image.jpg"),
                MediaType.IMAGE_JPEG
            )

            client.post()
                .uri("/artifact/${returnedArtifact["id"]}/resource")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user2Token")
                .body(BodyInserters.fromMultipartData(multipartDataRequest.build()))
                .exchange()
                .expectStatus().isForbidden
                .expectBody()
                .returnResult()
                .responseBody!!
        }
    }

    @Nested
    inner class GetArtifactResource {
        @Test
        fun `it should allow users the artifact is shared with access to access to the resources`() {
            val group = groupService.createGroup(user1.email, "Group 1", "Description", memberIDs = listOf(user2.id))
            val artifact = artifactService.createArtifact(user1.email, "Artifact name", "Artifact description", groupIDs = listOf(group.id))
            val resource = artifactResourceService.create(
                user1.email,
                artifactId = artifact.id,
                metadata = ArtifactResourceMetadata(name = "Resource name", description = "Resource description"),
                contentType = MediaType.IMAGE_PNG_VALUE,
                resource = ClassPathResource("test-image.jpg").file.readBytes()
            )

            client.get()
                .uri("/artifact/${artifact.id}/resource/${resource.id}/metadata")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user2Token")
                .exchange()
                .expectStatus().isOk
                .expectBody()
            client.get()
                .uri("/artifact/${artifact.id}/resource/${resource.id}/resource")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user2Token")
                .exchange()
                .expectStatus().isOk
                .expectBody()
        }

        @Test
        fun `it should allow users who are part of a group associated with the artifact access to the resources`() {
            val artifact = artifactService.createArtifact(user1.email, "Artifact name", "Artifact description", sharedWith = listOf(user2.id))
            val resource = artifactResourceService.create(
                user1.email,
                artifactId = artifact.id,
                metadata = ArtifactResourceMetadata(name = "Resource name", description = "Resource description"),
                contentType = MediaType.IMAGE_PNG_VALUE,
                resource = ClassPathResource("test-image.jpg").file.readBytes()
            )

            client.get()
                .uri("/artifact/${artifact.id}/resource/${resource.id}/metadata")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user2Token")
                .exchange()
                .expectStatus().isOk
                .expectBody()
            client.get()
                .uri("/artifact/${artifact.id}/resource/${resource.id}/resource")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user2Token")
                .exchange()
                .expectStatus().isOk
                .expectBody()
        }

        @Test
        fun `it should allow artifact owners access to the resources`() {
            val artifact = artifactService.createArtifact(user1.email, "Artifact name", "Artifact description")
            val resource = artifactResourceService.create(
                user1.email,
                artifactId = artifact.id,
                metadata = ArtifactResourceMetadata(name = "Resource name", description = "Resource description"),
                contentType = MediaType.IMAGE_PNG_VALUE,
                resource = ClassPathResource("test-image.jpg").file.readBytes()
            )

            client.get()
                .uri("/artifact/${artifact.id}/resource/${resource.id}/metadata")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user1Token")
                .exchange()
                .expectStatus().isOk
                .expectBody()
            client.get()
                .uri("/artifact/${artifact.id}/resource/${resource.id}/resource")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user1Token")
                .exchange()
                .expectStatus().isOk
                .expectBody()
        }

        @Test
        fun `it should not allow users without artifact access to access the resources`() {
            val artifact = artifactService.createArtifact(user1.email, "Artifact name", "Artifact description")
            val resource = artifactResourceService.create(
                user1.email,
                artifactId = artifact.id,
                metadata = ArtifactResourceMetadata(name = "Resource name", description = "Resource description"),
                contentType = MediaType.IMAGE_PNG_VALUE,
                resource = ClassPathResource("test-image.jpg").file.readBytes()
            )
            client.get()
                .uri("/artifact/${artifact.id}/resource/${resource.id}/metadata")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user2Token")
                .exchange()
                .expectStatus().isForbidden
                .expectBody()
            client.get()
                .uri("/artifact/${artifact.id}/resource/${resource.id}/resource")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user2Token")
                .exchange()
                .expectStatus().isForbidden
                .expectBody()
        }
    }

    @Nested
    inner class UpdateArtifactResource {
        @Test
        fun `it should allow artifact owners to update the resource`() {
            val returnedResource = createResource(
                artifactOwner = user1,
                artifactOwnerToken = user1Token,
                resourceCreatorToken = user1Token
            )
            val resourceId = (returnedResource["id"] as Int).toLong()
            val artifactId = (returnedResource["artifact"] as Int).toLong()

            val metadata =
                ArtifactResourceMetadata(name = "Updated resource name", description = "Updated resource description")
            val multipartDataRequest = MultipartBodyBuilder()
            multipartDataRequest.part("metadata", metadata, MediaType.APPLICATION_JSON_UTF8)
            multipartDataRequest.part(
                "resource",
                ClassPathResource("test-image2.png"),
                MediaType.IMAGE_PNG
            )

            client.put()
                .uri("/artifact/$artifactId/resource/$resourceId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user1Token")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartDataRequest.build()))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.name").value(equalTo(metadata.name))
                .jsonPath("$.description").value(equalTo(metadata.description))

            val resource = artifactResourceRepository.findByIdOrNull(resourceId)!!
            assertTrue(ClassPathResource("test-image2.png").file.readBytes().contentEquals(resource.resource))
        }

        @Test
        fun `it should not allow users who are not the artifact's owners to update the resource`() {
            val returnedResource = createResource(
                artifactOwner = user1,
                artifactOwnerToken = user1Token,
                resourceCreatorToken = user1Token
            )
            val resourceId = (returnedResource["id"] as Int).toLong()
            val artifactId = (returnedResource["artifact"] as Int).toLong()

            val metadata =
                ArtifactResourceMetadata(name = "Updated resource name", description = "Updated resource description")
            val multipartDataRequest = MultipartBodyBuilder()
            multipartDataRequest.part("metadata", metadata, MediaType.APPLICATION_JSON_UTF8)
            multipartDataRequest.part(
                "resource",
                ClassPathResource("test-image2.png"),
                MediaType.IMAGE_PNG
            )
            client.put()
                .uri("/artifact/$artifactId/resource/$resourceId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user2Token")
                .body(BodyInserters.fromMultipartData(multipartDataRequest.build()))
                .exchange()
                .expectStatus().isForbidden
                .expectBody()
        }

        @Test
        fun `it should allow updating the metadata only`() {
            val returnedResource = createResource(
                artifactOwner = user1,
                artifactOwnerToken = user1Token,
                resourceCreatorToken = user1Token
            )
            val resourceId = (returnedResource["id"] as Int).toLong()
            val artifactId = (returnedResource["artifact"] as Int).toLong()

            val metadata =
                ArtifactResourceMetadata(name = "Updated resource name", description = "Updated resource description")

            client.put()
                .uri("/artifact/$artifactId/resource/$resourceId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user1Token")
                .syncBody(metadata)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.name").value(equalTo(metadata.name))
                .jsonPath("$.description").value(equalTo(metadata.description))

            val resource = artifactResourceRepository.findByIdOrNull(resourceId)!!
            assertEquals(resource.name, metadata.name)
            assertEquals(resource.description, metadata.description)
        }

        @Test
        fun `it should allow updating the object only`() {
            val returnedResource = createResource(
                artifactOwner = user1,
                artifactOwnerToken = user1Token,
                resourceCreatorToken = user1Token
            )
            val resourceId = (returnedResource["id"] as Int).toLong()
            val artifactId = (returnedResource["artifact"] as Int).toLong()

            client.put()
                .uri("/artifact/$artifactId/resource/$resourceId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user1Token")
                .contentType(MediaType.IMAGE_PNG)
                .syncBody(ClassPathResource("test-image2.png"))
                .exchange()
                .expectStatus().isOk
                .expectBody()

            val resource = artifactResourceRepository.findByIdOrNull(resourceId)!!
            assertTrue(ClassPathResource("test-image2.png").file.readBytes().contentEquals(resource.resource))
        }
    }

    @Nested
    inner class DeleteArtifactResource {
        @Test
        fun `it should allow artifact owners to delete the resource`() {
            val artifact = artifactService.createArtifact(user1.email, "Artifact name", "Artifact description")
            val resource = artifactResourceService.create(
                user1.email,
                artifactId = artifact.id,
                metadata = ArtifactResourceMetadata(name = "Resource name", description = "Resource description"),
                contentType = MediaType.IMAGE_PNG_VALUE,
                resource = ClassPathResource("test-image.jpg").file.readBytes()
            )

            client.delete()
                .uri("/artifact/${artifact.id}/resource/${resource.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user1Token")
                .exchange()
                .expectStatus().isOk

            assertFalse(artifactResourceRepository.existsById(resource.id))
            assertThat(
                artifactRepository.findByIdOrNull(artifact.id)!!,
                hasProperty("resources", not(hasItem(hasProperty<ArtifactResource>("id", `is`(resource.id)))))
            )
        }

        @Test
        fun `it should not allow users who are not the artifact's owners to delete the resource`() {
            val artifact = artifactService.createArtifact(user1.email, "Artifact name", "Artifact description")
            val resource = artifactResourceService.create(
                user1.email,
                artifactId = artifact.id,
                metadata = ArtifactResourceMetadata(name = "Resource name", description = "Resource description"),
                contentType = MediaType.IMAGE_PNG_VALUE,
                resource = ClassPathResource("test-image.jpg").file.readBytes()
            )

            client.delete()
                .uri("/artifact/${artifact.id}/resource/${resource.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user2Token")
                .exchange()
                .expectStatus().isForbidden
        }
    }
}

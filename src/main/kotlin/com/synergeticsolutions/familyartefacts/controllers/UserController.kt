package com.synergeticsolutions.familyartefacts.controllers

import com.synergeticsolutions.familyartefacts.dtos.UserRequest
import com.synergeticsolutions.familyartefacts.dtos.UserUpdateRequest
import com.synergeticsolutions.familyartefacts.entities.User
import com.synergeticsolutions.familyartefacts.services.UserService
import java.security.Principal
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.Base64Utils
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for requests related to the [User] entity.
 */
@RestController
class UserController(
    @Autowired
    val userService: UserService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * POST /user
     * POST /register
     *
     * [createUser] is the POST endpoint for '/request'. Requests to this endpoint should conform to
     * [UserRequest]. If the body is not valid a 4xx response will be returned with a message describing
     * what is wrong with the request. If there is an issue creating the request a 5xx response will be returned
     * describing what when wrong. Successful requests will return 201 Created status code. The body will be the
     * created [User] object without the password field.
     *
     * @return The newly created [User] entity
     */
    @PostMapping(name = "createUser", path = ["/user", "/register"])
    fun createUser(@Valid @RequestBody request: UserRequest): ResponseEntity<User> {
        logger.info("Registering new request '${request.name}' with email '${request.email}")
        val user = userService.createUser(request.name, request.email, request.password)
        logger.info("User '${user.name}' was successfully created")
        logger.debug("$user")
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    /**
     * GET /user/me
     *
     * [getMe] gets the current user. A successful response will be the [User] entity of the authenticated user excluding the [User.password]
     * field.
     *
     * @return The currently authenticated [User] entity
     */
    @GetMapping(path = ["/user/me"])
    fun getMe(principal: Principal): User = userService.findByEmail(principal.name)

    /**
     * GET /user/me/image
     *
     * [getMyImage] gets the profile picture of the current user.
     *
     * The image is base64 encoded to make it easier for the frontend to handle, and the content
     * type is set to the image type of the picture.
     *
     * @return A [ByteArrayResource] of the profile picture for the current user.
     */
    @GetMapping(path = ["/user/me/image"])
    fun getMyImage(principal: Principal): ResponseEntity<ByteArrayResource> {
        val user = userService.findByEmail(principal.name)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(user.contentType ?: "image/png"))
            .body(ByteArrayResource(Base64Utils.encode(user.image)))
    }

    /**
     * GET /user/{id}
     *
     * [getUser] gets the text information associated with a user. A successful response will be the [User] entity
     * corresponding to [id] excluding the [User.password] field.
     *
     * @return The [User] entity for the user with ID [id]
     */
    @GetMapping(path = ["/user/{id}"])
    fun getUser(@PathVariable id: Long, principal: Principal): User = userService.findById(principal.name, id)

    /**
     * GET /user/{id}/image
     *
     * [getUserImage] gets the profile picture for user [id].
     */
    @GetMapping(path = ["/user/{id}/image"])
    fun getUserImage(@PathVariable id: Long, principal: Principal): ResponseEntity<ByteArrayResource> {
        val user = userService.findById(principal.name, id)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(user.contentType ?: "image/png"))
            .body(ByteArrayResource(Base64Utils.encode(user.image)))
    }
    /**
     * Get /user?email=[email]&name=[name]
     *
     * [getUserByEmailOrName] retrieves all users and filters them by [email] and [name] if specified. The [email]
     * and [name] need only match the first part of the user's email or name (i.e. [String.startsWith] is used)
     *
     * @return The [User] entities matched by the filters
     */
    @GetMapping(path = ["/user"])
    fun getUserByEmailOrName(
        @RequestParam(name = "email", required = false) email: String?,
        @RequestParam(name = "name", required = false) name: String?,
        principal: Principal
    ): List<User> = userService.findUsers(principal.name, filterEmail = email, filterName = name)

    /**
     * PUT /user/{id}
     *
     * [updateUser] updates the textual information associated with user [id].
     *
     * @return The updated [User] entity
     */
    @PutMapping(path = ["/user/{id}"])
    fun updateUser(@PathVariable("id") id: Long, @RequestBody update: UserUpdateRequest, principal: Principal): User =
        userService.update(principal.name, id, metadata = update, contentType = null)

    /**
     * PUT /user/{id}/image
     *
     * [updateImage] updates the image associated with user [id].
     */
    @PutMapping(path = ["/user/{id}/image"])
    fun updateImage(
        @RequestHeader(HttpHeaders.CONTENT_TYPE) contentType: String,
        @PathVariable("id") id: Long,
        @RequestBody profilePicture: ByteArray,
        principal: Principal
    ) =
        userService.update(principal.name, id, profilePicture = profilePicture, contentType = contentType)

    /**
     * Delete /user/{id}
     *
     * [deleteUser] deletes the user associated with [id].
     *
     * @return The deleted [User] entit
     */
    @DeleteMapping(path = ["/user/{id}"])
    fun deleteUser(@PathVariable("id") id: Long, principal: Principal): User = userService.delete(principal.name, id)
}

/**
 * RegistrationRequest represents a request sent to the registration endpoint.
 *
 * @param [name] Name of the user being registered. This should not be blank.
 * @param [email] Email of the user being registered. This will be used to uniquely identify them.
 * @param [password] Password the user will user in combination with their email to authenticate themselves. Has a
 *          minimum length of 6.
 */
data class RegistrationRequest(
    @field:NotBlank(message = "'name' must not be blank")
    val name: String,
    @field:Email(message = "'email' must be a well-formed email address")
    val email: String,
    @field:Length(min = 6, message = "'password' must have at least 6 characters")
    val password: String
)

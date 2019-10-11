package com.synergeticsolutions.familyartefacts

import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import javax.validation.Valid

@RestController
class UserController(
    @Autowired
    val userService: UserService
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * [createUser] is the POST endpoint for '/user'. Requests to this endpoint should conform to
     * [UserRequest]. If the body is not valid a 4xx response will be returned with a message describing
     * what is wrong with the request. If there is an issue creating the user a 5xx response will be returned
     * describing what when wrong. Successful requests will return 201 Created status code. The body will be the
     * created [User] object without the password field.
     */
    @PostMapping(name = "createUser", path = ["/user", "/register"])
    fun createUser(@Valid @RequestBody user: UserRequest): ResponseEntity<User> {
        logger.info("Registering new user '${user.name}' with email '${user.email}")
        val user = userService.createUser(user.name, user.email, user.password)
        logger.info("User '${user.name}' was successfully created")
        logger.debug("$user")
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    /**
     * GET /user/me
     *
     * [getMe] gets the current user. A successful response will be the [User] entity of the authenticated user excluding the [User.password]
     * field.
     */
    @GetMapping(path = ["/user/me"])
    fun getMe(principal: Principal): User = userService.findByEmail(principal.name)

    /**
     * GET /user/{id}
     *
     * [getUser] gets the text information associated with a user. A successful response will be the [User] entity
     * corresponding to [id] excluding the [User.password] field.
     */
    @GetMapping(path = ["/user/{id}"])
    fun getUser(@PathVariable id: Long, principal: Principal): User = userService.findById(principal.name, id)

    /**
     * Get /user?email=[email]&name=[name]
     *
     * [getUserByEmailOrName] retrieves all users and filters them by [email] and [name] if specified.
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
     * [updateUser] updates the textual informaton associated with user [id].
     */
    // @PutMapping(path = ["/user/{id}"])
    // fun updateUser(@PathVariable("id") id: Long, @RequestBody update: UserRequest, principal: Principal): User =
    //     userService.update(principal.name, id, update)

    /**
     * PUT /user/{id}/image
     *
     * [updateImage] updates the image associated with user [id].
     */
    @PutMapping(path = ["/user/{id}/image"])
    fun updateImage(@PathVariable("id") id: Long, @RequestBody image: ByteArray) {

    }
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

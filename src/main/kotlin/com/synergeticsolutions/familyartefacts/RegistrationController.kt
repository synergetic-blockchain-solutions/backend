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
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

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

@Controller
@RequestMapping(path = ["/register"])
class RegistrationController {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    // userService is a server for performing actions around validating and persisting user accounts.
    @Autowired
    lateinit var userService: UserService

    /**
     * [registerUser] is the POST endpoint for '/register'. Requests to this endpoint should conform to
     * [RegistrationRequest]. If the body is not valid a 4xx response will be returned with a message describing
     * what is wrong with the request. If there is an issue creating the user a 5xx response will be returned
     * describing what when wrong. Successful requests will return 201 Created status code. The body will be the
     * created [User] object without the password field.
     */
    @PostMapping
    fun registerUser(@Valid @RequestBody registration: RegistrationRequest): ResponseEntity<User> {
        logger.info("Registering new user '${registration.name}' with email '${registration.email}")
        val user = userService.createUser(registration.name, registration.email, registration.password)
        logger.info("User '${user.name}' was successfully created")
        logger.debug("User: id=${user.id} name=${user.name} email=${user.email} groups=${user.groups}")
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }
}

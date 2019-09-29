package com.synergeticsolutions.familyartefacts

import org.hibernate.validator.constraints.Length
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder
import java.net.URI
import javax.servlet.http.HttpServletResponse
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

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
     * Register a [User]
     *
     * @see [UserController.createUser]
     */
    @PostMapping
    fun registerUser(response: HttpServletResponse): ResponseEntity<Void> {
        logger.info("Redirecting user from /register to /user")
        val uri = URI(MvcUriComponentsBuilder.fromMappingName("createUser").build())
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(uri).build<Void>()
    }
}

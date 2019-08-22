package com.synergeticsolutions.familyartefacts

import org.hibernate.validator.constraints.Length
import org.hibernate.validator.constraints.ScriptAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

/**
 * RegistrationRequest represents a request sent to the registration endpoint.
 *
 * @param [name] Name of the user being registered. This should not be blank.
 * @param [email] Email of the user being registered. This will be used to uniquely identify them.
 * @param [password] Password the user will user in combination with their email to authenticate themselves. Has a
 *          minimum length of 6.
 * @param [confirmPassword] Confirmation of [password], they should be the same. Has a minimum length of 6.
 */
@ScriptAssert(
    lang = "javascript",
    script = "_this.confirmPassword.equals(_this.password)",
    message = "'password' and 'confirmPassword' are not matching"
)
data class RegistrationRequest(
    @field:NotBlank(message = "'name' must not be blank")
    val name: String,
    @field:Email(message = "'email' must be a well-formed email address")
    val email: String,
    @field:Length(min = 6, message = "'password' must have at least 6 characters")
    val password: String,
    @field:Length(min = 6, message = "'confirmPassword' must have at least 6 characters")
    val confirmPassword: String
)

@Controller
@RequestMapping(path = ["/register"])
class RegistrationController {

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
        val user = userService.createUser(registration.name, registration.email, registration.password)
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }
}

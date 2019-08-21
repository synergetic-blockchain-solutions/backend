package com.synergeticsolutions.familyartefacts

import org.hibernate.validator.constraints.Length
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

data class RegistrationRequest(
    @field:NotBlank
    val name: String,
    @field:Email
    val email: String,
    @field:Length(min = 6)
    val password: String,
    @field:Length(min = 6)
    val confirmPassword: String
)

@Controller
@RequestMapping(path = ["/register"])
class RegistrationController {

    @Autowired
    lateinit var userService: UserService

    @PostMapping
    fun registerUser(@Valid @RequestBody registration: RegistrationRequest): ResponseEntity<User> {
        val user = userService.createUser(registration.name, registration.email, registration.password)
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }
}

package com.synergeticsolutions.familyartefacts

import org.hibernate.validator.constraints.Length
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

/**
 * UserRequest represents a request sent to the registration endpoint.
 *
 * @param [name] Name of the user being registered. This should not be blank.
 * @param [email] Email of the user being registered. This will be used to uniquely identify them.
 * @param [password] Password the user will user in combination with their email to authenticate themselves. Has a
 *          minimum length of 6.
 */
data class UserRequest(
    @field:NotBlank(message = "'name' must not be blank")
    val name: String,
    @field:Email(message = "'email' must be a well-formed email address")
    val email: String,
    @field:Length(min = 6, message = "'password' must have at least 6 characters")
    val password: String
)

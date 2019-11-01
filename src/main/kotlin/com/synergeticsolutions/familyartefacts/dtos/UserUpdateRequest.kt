package com.synergeticsolutions.familyartefacts.dtos

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

/**
 * Models a request to update a user.
 *
 * The attributes are basically the same as [UserRequest], the difference is that the password is optional
 * because not all user update requests will want to update the password and the requester often will not
 * have access to the password.
 */
data class UserUpdateRequest(
    @field:NotBlank(message = "'name' must not be blank")
    val name: String,
    @field:Email(message = "'email' must be a well-formed email address")
    val email: String,
    val password: String? = null
)

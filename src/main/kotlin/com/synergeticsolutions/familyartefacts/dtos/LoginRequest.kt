package com.synergeticsolutions.familyartefacts.dtos

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

/**
 * Models a login request.
 *
 * @param email A valid email corresponding to an existing user
 * @param password The password to the user identified by [email]
 */
data class LoginRequest(
    @field:Email
    val email: String,
    @field:NotBlank
    val password: String
)

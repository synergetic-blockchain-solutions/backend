package com.synergeticsolutions.familyartefacts.dtos

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class UserUpdateRequest(
    @field:NotBlank(message = "'name' must not be blank")
    val name: String,
    @field:Email(message = "'email' must be a well-formed email address")
    val email: String,
    val password: String? = null
)

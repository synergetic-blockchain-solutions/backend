package com.synergeticsolutions.familyartefacts.dtos

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class LoginRequest(
    @field:Email
    val email: String,
    @field:NotBlank
    val password: String
)

package com.synergeticsolutions.familyartefacts

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class PasswordTooShort(min: Int) : RuntimeException("Password must be at least $min characters long")

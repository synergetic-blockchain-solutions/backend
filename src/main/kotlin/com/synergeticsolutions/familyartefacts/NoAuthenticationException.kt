package com.synergeticsolutions.familyartefacts

import javax.naming.AuthenticationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class NoAuthenticationException : AuthenticationException()

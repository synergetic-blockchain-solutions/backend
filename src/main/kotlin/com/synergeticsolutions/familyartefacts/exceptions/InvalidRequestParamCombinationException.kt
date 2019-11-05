package com.synergeticsolutions.familyartefacts.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Exception thrown when the HTTP request contains an incompatible combination of parameters.
 *
 * Some requests take many request parameters that cannot all be used together.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidRequestParamCombinationException(msg: String) : RuntimeException(msg)

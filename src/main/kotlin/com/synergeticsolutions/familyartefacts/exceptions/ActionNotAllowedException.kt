package com.synergeticsolutions.familyartefacts.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Exception thrown when the user attempts to perform an action they're not allowed to do. It maps to returning a 403
 * Forbidden HTTP status code.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
class ActionNotAllowedException(msg: String) : RuntimeException(msg)

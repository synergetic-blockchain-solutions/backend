package com.synergeticsolutions.familyartefacts

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.FORBIDDEN)
class ActionNotAllowedException(msg: String) : RuntimeException(msg)

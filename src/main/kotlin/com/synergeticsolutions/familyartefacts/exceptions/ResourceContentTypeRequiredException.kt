package com.synergeticsolutions.familyartefacts.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class ResourceContentTypeRequiredException : RuntimeException("'resource' part Content-Type is required to be provided")

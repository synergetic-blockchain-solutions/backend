package com.synergeticsolutions.familyartefacts

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class MetadataPartRequired : RuntimeException("Part 'metadata' is required in the request")

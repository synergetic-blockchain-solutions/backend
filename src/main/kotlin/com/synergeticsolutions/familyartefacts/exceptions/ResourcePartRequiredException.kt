package com.synergeticsolutions.familyartefacts.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Exception thrown when the resource part is not included in a multipart/form-data request.
 *
 * Some request bodies are more complex than just a binary of a JSON document and are a combination of both.
 * In this cases we use a multipart/form-data content type. By convention the binary part is in the "resource"
 * part and this exception is thrown if it does not exist.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
class ResourcePartRequiredException : RuntimeException("Part resource' is required in request")

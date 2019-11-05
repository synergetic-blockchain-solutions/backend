package com.synergeticsolutions.familyartefacts.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Exception thrown if the "resource" part of a multipart/form-data request does not have a content type.
 *
 * To correctly return an image to a user, we need to know the content type. Thus, if we are given an image with no
 * content type we throw this exception to notify the user that they must provide it.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
class ResourceContentTypeRequiredException : RuntimeException("'resource' part Content-Type is required to be provided")

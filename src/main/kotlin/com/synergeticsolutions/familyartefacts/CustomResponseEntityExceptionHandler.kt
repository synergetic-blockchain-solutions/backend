package com.synergeticsolutions.familyartefacts

import java.util.Date
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Custom handler for exceptions thrown in request handlers.
 */
@ControllerAdvice
class CustomResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    /**
     * Handle exceptions thrown due to request validation errors. This allows us to provide more context than the default
     * implementation, in that we return a list of the validation errors ("errors" field) rather than just a message
     * saying that validation has failed.
     */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {

        val body = mapOf(
            "timestamp" to Date(),
            "status" to status.value(),
            "message" to "Validation failed",
            "errors" to ex.bindingResult.allErrors.map { it.defaultMessage }
        )

        return ResponseEntity(body, headers, status)
    }
}

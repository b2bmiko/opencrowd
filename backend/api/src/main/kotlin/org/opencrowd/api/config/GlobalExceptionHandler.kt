package org.opencrowd.api.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {
        val details = ex.bindingResult.fieldErrors.map { fieldError ->
            FieldError(
                field = fieldError.field,
                message = fieldError.defaultMessage ?: "Invalid value"
            )
        }
        val error = ApiErrorResponse(
            code = "VALIDATION_ERROR",
            message = "Validation failed",
            details = details,
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            code = "FORBIDDEN",
            message = "Access denied",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(
        ex: NoSuchElementException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            code = "NOT_FOUND",
            message = ex.message ?: "Resource not found",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            code = "BAD_REQUEST",
            message = ex.message ?: "Invalid request",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericError(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            code = "INTERNAL_ERROR",
            message = "An unexpected error occurred",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val details: List<FieldError>? = null,
    val correlationId: String? = null,
    val timestamp: Instant
)

data class FieldError(
    val field: String,
    val message: String
)

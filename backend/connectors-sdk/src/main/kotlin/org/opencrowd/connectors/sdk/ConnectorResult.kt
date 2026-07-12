package org.opencrowd.connectors.sdk

/**
 * Represents the result of a connector operation.
 * Either a Success with data, or a Failure with error information.
 */
sealed class ConnectorResult<out T> {
    data class Success<T>(val data: T) : ConnectorResult<T>()
    data class Failure(
        val code: ErrorCode,
        val message: String,
        val retryable: Boolean = false,
        val details: Map<String, Any>? = null
    ) : ConnectorResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun <R> map(transform: (T) -> R): ConnectorResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw ConnectorException(code, message)
    }
}

enum class ErrorCode {
    CONNECTION_FAILED,
    AUTHENTICATION_FAILED,
    AUTHORIZATION_FAILED,
    RESOURCE_NOT_FOUND,
    OPERATION_NOT_SUPPORTED,
    RATE_LIMITED,
    TIMEOUT,
    VALIDATION_ERROR,
    CONFLICT,
    INTERNAL_ERROR,
    NOT_IMPLEMENTED
}

class ConnectorException(
    val code: ErrorCode,
    override val message: String
) : RuntimeException(message)

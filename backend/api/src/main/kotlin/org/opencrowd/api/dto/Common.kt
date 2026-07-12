package org.opencrowd.api.dto

import java.time.Instant

/**
 * Standard paginated response wrapper.
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)

/**
 * Standard API response wrapper for single-item responses.
 */
data class ApiResponse<T>(
    val data: T,
    val timestamp: Instant = Instant.now(),
)

/**
 * Standard list response (non-paginated).
 */
data class ListResponse<T>(
    val data: List<T>,
    val count: Int,
    val timestamp: Instant = Instant.now(),
)

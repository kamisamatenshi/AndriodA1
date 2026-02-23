package com.koi.thepiece.data.api.dto

import com.squareup.moshi.Json

/**
 * Generic API response wrapper used by multiple endpoints.
 *
 * @property success Indicates whether the request was processed successfully.
 * @property message Optional informational or error message from the backend.
 * @property price Optional price value (used by price-related endpoints).
 */
data class ApiResultDto(
    val success: Boolean? = null,
    val message: String? = null,
    val price: Int? = null
)

/**
 * Request body for updating user-owned card quantity.
 *
 * Maps to update_qty.php endpoint.
 *
 * @property token Session token for authentication.
 * @property id Card ID to update.
 * @property ownedQty New owned quantity for the specified card.
 */
data class UpdateQtyBody(
    @Json(name = "token") val token: String,
    @Json(name = "id") val id: Int,
    @Json(name = "owned_qty") val ownedQty: Int
)

/**
 * Request body for retrieving price information.
 *
 * Maps to get_price.php endpoint.
 *
 * @property yuyuUrl Marketplace URL used for server-side scraping.
 */
data class GetPriceBody(
    @Json(name = "url") val yuyuUrl: String?
)

/**
 * Direct price response model for GET-based pricing endpoint.
 *
 * @property success Indicates if scraping/parsing succeeded.
 * @property url Original marketplace URL.
 * @property raw Raw scraped price string (if provided).
 * @property price Parsed integer price value.
 * @property error Error message if scraping fails.
 */
data class PriceDto(
    val success: Boolean,
    val url: String? = null,
    val raw: String? = null,
    val price: Int? = null,
    val error: String? = null
)

/**
 * Response model for retrieving full card list.
 *
 * @property success Indicates whether retrieval succeeded.
 * @property cards List of CardDto objects returned from server.
 * @property error Optional error message.
 */
data class CardsResponseDto(
    val success: Boolean,
    val cards: List<CardDto>,
    val error: String? = null
)

/**
 * Response model for session validation checks.
 *
 * @property success Indicates API request success.
 * @property valid Whether the session token is valid.
 * @property reason Optional reason for invalid session.
 * @property user_id Associated user ID if valid.
 * @property expires_at Expiration timestamp of the session.
 * @property message Optional backend message.
 */
data class SessionCheckDto(
    val success: Boolean,
    val valid: Boolean,
    val reason: String? = null,
    val user_id: Int? = null,
    val expires_at: String? = null,
    val message: String? = null
)

/**
 * Request body for user login.
 *
 * @property email User email.
 * @property password User plaintext password (hashed server-side).
 */
data class LoginBody(
    val email: String,
    val password: String
)

/**
 * Response model for login endpoint.
 *
 * @property success Indicates login success.
 * @property message Optional message (error or info).
 * @property user_id Authenticated user ID.
 * @property token Session token for authenticated requests.
 * @property expires_at Expiration timestamp of session.
 */
data class LoginDto(
    val success: Boolean,
    val message: String? = null,
    val user_id: Int? = null,
    val token: String? = null,
    val expires_at: String? = null
)

/**
 * Request body for user registration.
 *
 * @property email User email address.
 * @property password Plaintext password (hashed server-side).
 */
data class RegisterBody(
    val email: String,
    val password: String
)

/**
 * Response model for registration endpoint.
 *
 * @property success Indicates whether registration succeeded.
 * @property message Optional backend message.
 * @property user_id Newly created user ID.
 */
data class RegisterDto(
    val success: Boolean,
    val message: String? = null,
    val user_id: Int? = null
)
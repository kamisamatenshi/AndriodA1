package com.koi.thepiece.data.api.dto

import com.squareup.moshi.Json

data class ApiResultDto(
    val success: Boolean? = null,
    val message: String? = null,
    val price: Int? = null
)

data class UpdateQtyBody(
    @Json(name = "token") val token: String,
    @Json(name = "id") val id: Int,
    @Json(name = "owned_qty") val ownedQty: Int
)

data class GetPriceBody (
    @Json(name = "url") val yuyuUrl: String?,
)

data class PriceDto(
    val success: Boolean,
    val url: String? = null,
    val raw: String? = null,
    val price: Int? = null,
    val error: String? = null
)

data class CardsResponseDto(
    val success: Boolean,
    val cards: List<CardDto>,
    val error: String? = null
)

data class SessionCheckDto(
    val success: Boolean,
    val valid: Boolean,
    val reason: String? = null,
    val user_id: Int? = null,
    val expires_at: String? = null,
    val message: String? = null
)
data class LoginBody(
    val email: String,
    val password: String
)

data class LoginDto(
    val success: Boolean,
    val message: String? = null,
    val user_id: Int? = null,
    val token: String? = null,
    val expires_at: String? = null
)

data class RegisterBody(
    val email: String,
    val password: String
)

data class RegisterDto(
    val success: Boolean,
    val message: String? = null,
    val user_id: Int? = null
)



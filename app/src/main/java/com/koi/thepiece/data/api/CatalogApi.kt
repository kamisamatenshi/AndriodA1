package com.koi.thepiece.data.api

import com.koi.thepiece.data.api.dto.ApiResultDto
import com.koi.thepiece.data.api.dto.CardDto
import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class UpdateQtyBody(
    @Json(name = "id") val id: Int,
    @Json(name = "owned_qty") val ownedQty: Int
)

interface CatalogApi {
    @GET("api/cards.php")
    suspend fun getCards(): List<CardDto>

    @POST("api/update_card.php")
    suspend fun updateQty(@Body body: UpdateQtyBody): ApiResultDto
}

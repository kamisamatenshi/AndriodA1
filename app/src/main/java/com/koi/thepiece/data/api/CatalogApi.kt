package com.koi.thepiece.data.api

import com.koi.thepiece.data.api.dto.ApiResultDto
import com.koi.thepiece.data.api.dto.CardDto
import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class UpdateQtyBody(
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


interface CatalogApi {
    @GET("api/cards.php")
    suspend fun getCards(): List<CardDto>

    @POST("api/update_card.php")
    suspend fun updateQty(@Body body: UpdateQtyBody): ApiResultDto

    @POST("api/get_price.php")
    suspend fun getPrice(@Body body: GetPriceBody): ApiResultDto

    @GET("api/get_price.php")
    suspend fun getPrice2(@Query("url") url: String): PriceDto
}

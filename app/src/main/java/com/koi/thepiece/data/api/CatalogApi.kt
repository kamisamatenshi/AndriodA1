package com.koi.thepiece.data.api

import com.koi.thepiece.data.api.dto.ApiResultDto
import com.koi.thepiece.data.api.dto.CardDto
import com.koi.thepiece.data.api.dto.CardsResponseDto
import com.koi.thepiece.data.api.dto.GetPriceBody
import com.koi.thepiece.data.api.dto.LoginBody
import com.koi.thepiece.data.api.dto.LoginDto
import com.koi.thepiece.data.api.dto.PriceDto
import com.koi.thepiece.data.api.dto.RegisterBody
import com.koi.thepiece.data.api.dto.RegisterDto
import com.koi.thepiece.data.api.dto.SessionCheckDto
import com.koi.thepiece.data.api.dto.UpdateQtyBody
import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query



interface CatalogApi {
    @GET("api/get_cards.php")
    suspend fun getCards(@Query("token") token: String): CardsResponseDto

    @POST("api/update_qty.php")
    suspend fun updateQty(@Body body: UpdateQtyBody): ApiResultDto

    @POST("api/get_price.php")
    suspend fun getPrice(@Body body: GetPriceBody): ApiResultDto

    @GET("api/get_price.php")
    suspend fun getPrice2(@Query("url") url: String): PriceDto
}

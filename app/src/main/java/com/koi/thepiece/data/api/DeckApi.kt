package com.koi.thepiece.data.api

import com.koi.thepiece.data.api.dto.DeckCreateRequestDto
import com.koi.thepiece.data.api.dto.DeckCreateResponseDto
import com.koi.thepiece.data.api.dto.DeckDeleteRequestDto
import com.koi.thepiece.data.api.dto.DeckDeleteResponseDto
import com.koi.thepiece.data.api.dto.DeckGetResponseDto
import com.koi.thepiece.data.api.dto.DeckImportResponseDto
import com.koi.thepiece.data.api.dto.DeckListResponseDto
import com.koi.thepiece.data.api.dto.DeckUpdateRequestDto
import com.koi.thepiece.data.api.dto.DeckUpdateResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DeckApi {

    @POST("api/deck_create.php")
    suspend fun createDeck(@Body req: DeckCreateRequestDto): DeckCreateResponseDto

    @POST("api/deck_update.php")
    suspend fun updateDeck(@Body req: DeckUpdateRequestDto): DeckUpdateResponseDto

    @POST("api/deck_delete.php")
    suspend fun deleteDeck(@Body req: DeckDeleteRequestDto): DeckDeleteResponseDto

    @GET("api/deck_list_owned.php")
    suspend fun getOwnedDeckSummaries(@Query("token") token: String): DeckListResponseDto

    @GET("api/deck_get.php")
    suspend fun getDeck(
        @Query("token") token: String,
        @Query("deck_id") deckId: Long
    ): DeckGetResponseDto

    @GET("api/deck_import_by_share.php")
    suspend fun importDeckByShareCode(
        @Query("token") token: String,
        @Query("share_code") shareCode: String
    ): DeckImportResponseDto
}
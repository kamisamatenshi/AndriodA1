package com.koi.thepiece.data.api

import com.koi.thepiece.data.api.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit API contract for deck-related backend operations.
 *
 * This interface defines all network calls associated with:
 * - Deck creation
 * - Deck updating
 * - Deck deletion
 * - Deck retrieval
 * - Deck listing
 * - Deck import via share code
 *
 * All endpoints require authentication via session token.
 */
interface DeckApi {

    /**
     * Creates a new deck for the authenticated user.
     *
     * Backend responsibilities:
     * - Validates session token
     * - Inserts deck into user_owned_decks
     * - Stores deck composition
     * - Generates share code if applicable
     *
     * @param req DeckCreateRequestDto containing deck name, leader, cards, and hash.
     * @return DeckCreateResponseDto containing success status and new deck ID.
     */
    @POST("api/deck_create.php")
    suspend fun createDeck(
        @Body req: DeckCreateRequestDto
    ): DeckCreateResponseDto

    /**
     * Updates an existing deck.
     *
     * Backend responsibilities:
     * - Validates session token
     * - Checks ownership of oldDeckId
     * - Updates deck composition and metadata
     *
     * @param req DeckUpdateRequestDto containing updated deck data.
     * @return DeckUpdateResponseDto indicating update result and potential new deck ID.
     */
    @POST("api/deck_update.php")
    suspend fun updateDeck(
        @Body req: DeckUpdateRequestDto
    ): DeckUpdateResponseDto

    /**
     * Deletes a deck owned by the authenticated user.
     *
     * Backend responsibilities:
     * - Validates session token
     * - Confirms ownership
     * - Removes deck from user_owned_decks
     *
     * @param req DeckDeleteRequestDto containing token and deck ID.
     * @return DeckDeleteResponseDto indicating deletion status.
     */
    @POST("api/deck_delete.php")
    suspend fun deleteDeck(
        @Body req: DeckDeleteRequestDto
    ): DeckDeleteResponseDto

    /**
     * Retrieves a list of decks owned by the authenticated user.
     *
     * Used for displaying deck summaries in the deck selection screen.
     *
     * @param token Session token for authentication.
     * @return DeckListResponseDto containing deck summary list.
     */
    @GET("api/deck_list_owned.php")
    suspend fun getOwnedDeckSummaries(
        @Query("token") token: String
    ): DeckListResponseDto

    /**
     * Retrieves full deck details by deck ID.
     *
     * Backend responsibilities:
     * - Validates session token
     * - Confirms ownership or access rights
     * - Returns full deck composition
     *
     * @param token Session token.
     * @param deckId Unique identifier of the requested deck.
     * @return DeckGetResponseDto containing full deck data.
     */
    @GET("api/deck_get.php")
    suspend fun getDeck(
        @Query("token") token: String,
        @Query("deck_id") deckId: Long
    ): DeckGetResponseDto

    /**
     * Imports a deck using a share code.
     *
     * Backend responsibilities:
     * - Validates session token
     * - Resolves share code to deck record
     * - Copies deck into user_owned_decks if allowed
     *
     * @param token Session token.
     * @param shareCode Unique share code of the deck.
     * @return DeckImportResponseDto containing import status and deck data.
     */
    @GET("api/deck_import_by_share.php")
    suspend fun importDeckByShareCode(
        @Query("token") token: String,
        @Query("share_code") shareCode: String
    ): DeckImportResponseDto
}
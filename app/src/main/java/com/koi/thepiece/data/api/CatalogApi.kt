package com.koi.thepiece.data.api

import com.koi.thepiece.data.api.dto.ApiResultDto
import com.koi.thepiece.data.api.dto.CardsResponseDto
import com.koi.thepiece.data.api.dto.GetPriceBody
import com.koi.thepiece.data.api.dto.PriceDto
import com.koi.thepiece.data.api.dto.UpdateQtyBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


/**
 * Retrofit API contract for catalogue-related backend endpoints.
 *
 * This interface defines network calls used by the Catalogue module, including:
 * - Fetching the full card list (server-authoritative data)
 * - Updating user-owned quantity for a given card
 * - Retrieving real-time marketplace pricing via the backend scraping endpoint
 */
interface CatalogApi {
    /**
     * Retrieves the full list of cards from the server.
     *
     * The backend may use the token to:
     * - Validate the session (authenticated request)
     * - Optionally personalize the response (e.g., include owned quantities)
     *
     * @param token Session token used for authentication / authorization.
     * @return CardsResponseDto containing the card records and metadata.
     */
    @GET("api/get_cards.php")
    suspend fun getCards(@Query("token") token: String): CardsResponseDto

    /**
     * Updates the owned quantity of a specific card for the authenticated user.
     *
     * Expected backend behavior:
     * - Validates the session token in the request body
     * - Performs an UPSERT into user_cards (or deletes row when qty == 0)
     *
     * @param body UpdateQtyBody containing token, card id, and new owned quantity.
     * @return ApiResultDto indicating success/failure and optional message payload.
     */
    @POST("api/update_qty.php")
    suspend fun updateQty(@Body body: UpdateQtyBody): ApiResultDto

    /**
     * Retrieves the latest price information via backend scraping.
     *
     * This uses a POST body (GetPriceBody) typically containing:
     * - token (optional depending on your backend design)
     * - yuyutei URL (or identifier) for the target card
     *
     * @param body GetPriceBody containing the card marketplace URL or lookup key.
     * @return ApiResultDto containing success/failure and pricing payload (if wrapped).
     */
    @POST("api/get_price.php")
    suspend fun getPrice(@Body body: GetPriceBody): ApiResultDto

    /**
     * Alternative GET-based pricing endpoint (query-string driven).
     *
     * This is useful for quick debugging/testing, but note:
     * - Query-string URLs may need encoding
     * - GET requests can expose sensitive parameters in logs
     *
     * @param url Full marketplace URL used by the backend to scrape pricing data.
     * @return PriceDto containing parsed price fields (direct DTO response).
     */
    @GET("api/get_price.php")
    suspend fun getPrice2(@Query("url") url: String): PriceDto
}

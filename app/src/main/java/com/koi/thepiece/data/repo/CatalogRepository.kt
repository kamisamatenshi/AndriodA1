package com.koi.thepiece.data.repo

import android.content.Context
import android.util.Log
import com.koi.thepiece.data.api.CatalogApi
import com.koi.thepiece.data.db.CardDao
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.data.model.toDomain
import com.koi.thepiece.data.model.toEntity
import com.koi.thepiece.core.image.ImagePreloader
import com.koi.thepiece.data.api.dto.GetPriceBody
import com.koi.thepiece.data.api.dto.UpdateQtyBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Represents completion progress for a selected card set.
 *
 * Completion is defined as owning at least one copy of each card in scope.
 *
 * @property owned Number of unique cards where ownedQty > 0.
 * @property total Total number of cards within the selected set scope.
 * @property percent Completion ratio (0.0 to 1.0).
 */
data class SetCompletion(val owned: Int, val total: Int, val percent: Float)

/**
 * Normalizes user-facing / backend-facing set strings into a consistent format.
 * Example: "op01" -> "OP01".
 */
private fun norm(s: String?) = s?.trim()?.uppercase()

/**
 * Checks whether a string looks like a set code.
 *
 * Supported examples:
 * - OP01, EB04, PRB02, ST29
 *
 * Used to disambiguate cards whose "obtainFrom" field may contain a set code
 * versus other acquisition sources.
 */
private fun looksLikeSetCode(s: String?): Boolean {
    val v = norm(s) ?: return false
    // matches OP01, EB04, PRB02, ST29 etc
    return Regex("""^(OP|EB|PRB|ST)\d{2}$""").matches(v)
}

/**
 * Repository responsible for catalogue data operations.
 *
 * Responsibilities:
 * - Read cards from local Room cache (reactive Flow)
 * - Synchronize cards from backend into Room
 * - Update owned quantities with optimistic local updates + server persistence
 * - Compute set completion progress (owned >= 1 per card)
 * - Retrieve real-time price via backend price endpoints
 *
 * This layer abstracts:
 * - Retrofit API calls (CatalogApi)
 * - Local database operations (CardDao)
 * so ViewModels do not directly manage network/database concerns.
 */
class CatalogRepository(
    private val appContext: Context,
    private val api: CatalogApi,
    private val price_api: CatalogApi,
    private val dao: CardDao
) {
    /**
     * Observes all locally cached cards as domain models.
     *
     * Uses Flow so the UI updates automatically when Room changes,
     * such as after synchronization or owned quantity updates.
     */
    fun observeCards(): Flow<List<Card>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /**
     * Retrieves owned quantity for a specific card from local cache.
     *
     * @param cardId Local card ID.
     * @return Owned quantity, defaults to 0 if missing.
     */
    suspend fun getStockQty(cardId: Int): Int = withContext(Dispatchers.IO) {
        dao.getOwnedQtyById(cardId) ?: 0
    }

    /**
     * Retrieves a map of cardId -> ownedQty from local cache.
     *
     * Used for efficient ownership checks (e.g., UI badges, completion calculations).
     */
    suspend fun getOwnedQtyMap(): Map<Int, Int> = withContext(Dispatchers.IO) {
        dao.getAllOwnedQty().associate { it.id to it.ownedQty }
    }

    /**
     * Observes collection completion for a given set.
     *
     * Completion definition:
     * - A card is counted as owned if ownedQty > 0.
     * - Scope is determined by selected set code.
     *
     * Filtering rules:
     * - If target is null or "ALL": include all cards.
     * - If "obtainFrom" matches selected set: include.
     * - Else, include if "cardSet" matches selected set and obtainFrom is not itself a set code
     *   (prevents double-counting or mismatched attribution).
     *
     * @param cardSet Selected set code (e.g., "OP01") or "ALL".
     * @return Flow emitting SetCompletion updates whenever card data changes.
     */
    fun observeSetCompletion(cardSet: String): Flow<SetCompletion> {
        val target = norm(cardSet)

        return observeCards().map { cards ->
            val setCards = if (target == null || target == "ALL") {
                cards
            } else {
                cards.filter { c ->
                    val cs = norm(c.cardSet)
                    val ob = norm(c.obtainFrom)

                    val obtainIsSet = looksLikeSetCode(ob)


                    if (ob == target) return@filter true


                    if (cs == target && !obtainIsSet) return@filter true


                    false
                }
            }

            val total = setCards.size
            val owned = setCards.count { it.ownedQty > 0 }
            val percent = if (total == 0) 0f else owned.toFloat() / total

            SetCompletion(owned, total, percent)
        }
    }

    /**
     * Synchronizes card data from the backend into the local Room cache.
     *
     * Flow:
     * 1. Fetch server cards via token-authenticated endpoint
     * 2. Convert DTO -> Entity with defensive null handling
     * 3. Upsert into Room
     * 4. Optionally preload images for first page (performance optimization)
     *
     * @param token Session token required by backend.
     * @param preloadFirstPageImages Whether to preload the first batch of image URLs.
     * @return Result<Unit> indicating success or failure.
     */
    suspend fun refreshCards(
        token: String,
        preloadFirstPageImages: Boolean = true
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val now = System.currentTimeMillis()

                val res = api.getCards(token)
                if (res.success != true) {
                    throw IllegalStateException(res.error ?: "Failed to fetch cards")
                }

                val remoteCards = res.cards ?: emptyList()
                val entities = remoteCards.mapNotNull { it.toEntity(now) }

                dao.upsertAll(entities)

                if (preloadFirstPageImages) {
                    val urls = entities.map { it.imageUrl }
                    ImagePreloader.preload(appContext, urls, limit = 30)
                }
            }
        }
    }

    /**
     * Updates a user's owned quantity for a card.
     *
     * Strategy: optimistic local update, then server update.
     * - Local cache update ensures immediate UI responsiveness.
     * - Server call persists the change to user_cards table.
     *
     * Note: if the server update fails, the local cache remains updated.
     * A production approach may add rollback or a retry queue.
     *
     * @param token Session token for authenticated update.
     * @param cardId Card ID to update.
     * @param newQty New quantity value (expected >= 0).
     * @return Result<Unit> indicating server update success/failure.
     */
    suspend fun updateOwnedQty(token:String,cardId: Int, newQty: Int ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            // Optimistic local update first (fast UI)
            val now = System.currentTimeMillis()
            dao.updateOwnedQty(cardId, newQty, now)

            // Then push to server
            runCatching {
                val res = api.updateQty(
                    UpdateQtyBody(
                        token = token,
                        id = cardId,
                        ownedQty = newQty
                    )
                )
                if (res.success != true) {
                    throw IllegalStateException(res.message ?: "Server update failed")
                }
            }
        }
    }

    /**
     * Retrieves card price via POST-based pricing endpoint.
     *
     * Backend performs scraping using the provided marketplace URL and returns a parsed price.
     *
     * @param cardUrl Marketplace URL (e.g., Yuyutei page).
     * @return Result<Int> containing parsed integer price.
     */
    suspend fun GetPrice(cardUrl: String?):Result<Int>{
        return withContext(Dispatchers.IO) {
            runCatching {
                val res = price_api.getPrice(GetPriceBody(cardUrl))
                if (res.success != true) {
                    throw IllegalStateException(res.message ?: "Server update failed")
                }
                res.price ?: throw IllegalStateException("Price missing from response")
            }

        }

    }

    /**
     * Retrieves card price via GET-based pricing endpoint.
     *
     * Useful for debugging or alternative backend contract.
     * Logs raw response for troubleshooting.
     *
     * @param cardUrl Marketplace URL passed as a query parameter (must be properly encoded).
     * @return Result<Int> containing parsed integer price.
     */
    suspend fun getPrice2(cardUrl: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val res = price_api.getPrice2(cardUrl)
                Log.d("PRICE_RAW", "Response = $res")
                if (res.success != true) throw IllegalStateException(res.error ?: "Price fetch failed")
                res.price ?: throw IllegalStateException("Missing price")
            }
        }
    }
}

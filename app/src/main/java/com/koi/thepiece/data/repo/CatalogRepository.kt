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

data class SetCompletion(val owned: Int, val total: Int, val percent: Float)

private fun norm(s: String?) = s?.trim()?.uppercase()

private fun looksLikeSetCode(s: String?): Boolean {
    val v = norm(s) ?: return false
    // matches OP01, EB04, PRB02, ST29 etc
    return Regex("""^(OP|EB|PRB|ST)\d{2}$""").matches(v)
}
class CatalogRepository(
    private val appContext: Context,
    private val api: CatalogApi,
    private val dao: CardDao
) {
    fun observeCards(): Flow<List<Card>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

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

    suspend fun GetPrice(cardUrl: String?):Result<Int>{
        return withContext(Dispatchers.IO) {
            runCatching {
                val res = api.getPrice(GetPriceBody(cardUrl))
                if (res.success != true) {
                    throw IllegalStateException(res.message ?: "Server update failed")
                }
                res.price ?: throw IllegalStateException("Price missing from response")
            }

        }

    }

    suspend fun getPrice2(cardUrl: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val res = api.getPrice2(cardUrl)
                Log.d("PRICE_RAW", "Response = $res")
                if (res.success != true) throw IllegalStateException(res.error ?: "Price fetch failed")
                res.price ?: throw IllegalStateException("Missing price")
            }
        }
    }
}

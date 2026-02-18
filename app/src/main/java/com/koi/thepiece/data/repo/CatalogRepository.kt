package com.koi.thepiece.data.repo

import android.content.Context
import com.koi.thepiece.data.api.CatalogApi
import com.koi.thepiece.data.api.UpdateQtyBody
import com.koi.thepiece.data.db.CardDao
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.data.model.toDomain
import com.koi.thepiece.data.model.toEntity
import com.koi.thepiece.core.image.ImagePreloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CatalogRepository(
    private val appContext: Context,
    private val api: CatalogApi,
    private val dao: CardDao
) {
    fun observeCards(): Flow<List<Card>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun refreshCards(preloadFirstPageImages: Boolean = true): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val now = System.currentTimeMillis()
                val remote = api.getCards()
                val entities = remote.mapNotNull { it.toEntity(now) }
                dao.upsertAll(entities)

                if (preloadFirstPageImages) {
                    val urls = entities.map { it.imageUrl }
                    ImagePreloader.preload(appContext, urls, limit = 30)
                }
            }
        }
    }

    suspend fun updateOwnedQty(cardId: Int, newQty: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            // Optimistic local update first (fast UI)
            val now = System.currentTimeMillis()
            dao.updateOwnedQty(cardId, newQty, now)

            // Then push to server
            runCatching {
                val res = api.updateQty(UpdateQtyBody(id = cardId, ownedQty = newQty))
                if (res.success != true) {
                    throw IllegalStateException(res.message ?: "Server update failed")
                }
            }
        }
    }
}

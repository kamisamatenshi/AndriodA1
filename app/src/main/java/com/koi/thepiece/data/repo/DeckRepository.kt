package com.koi.thepiece.data.repo

import com.koi.thepiece.data.api.DeckApi
import com.koi.thepiece.data.api.dto.DeckCardReqDto
import com.koi.thepiece.data.api.dto.DeckCreateRequestDto
import com.koi.thepiece.data.api.dto.DeckDeleteRequestDto
import com.koi.thepiece.data.api.dto.DeckUpdateRequestDto
import com.koi.thepiece.data.db.DeckCardEntity
import com.koi.thepiece.data.db.DeckDao
import com.koi.thepiece.data.db.DeckEntity
import com.koi.thepiece.data.db.DeckWithCards
import com.koi.thepiece.ui.screens.deckbuilderscreen.QtyClass
import java.security.MessageDigest
import android.util.Log
import com.koi.thepiece.data.api.dto.DeckImportResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class DeckRepository(private val deckDao: DeckDao ,private val api: DeckApi) {

    private val _shareCodeMap = MutableStateFlow<Map<Long, String>>(emptyMap())
    val shareCodeMap = _shareCodeMap.asStateFlow()

    suspend fun getDeck(deckId: Long): DeckWithCards? = deckDao.getDeck(deckId)

    fun observeAllDecks(): kotlinx.coroutines.flow.Flow<List<DeckWithCards>> {
        return deckDao.observeAllDecks()
    }


    suspend fun syncOwnedDecksFromServer(token: String) {
        // 1) get owned deck IDs
        val listResp = api.getOwnedDeckSummaries(token)
        if (!listResp.success || listResp.decks == null) {
            throw IllegalStateException("deck_list_owned failed: ${listResp.error ?: "unknown"}")
        }


        val shareMap: Map<Long, String> = listResp.decks
            .mapNotNull { s -> s.shareCode?.let { code -> s.deckId to code } }
            .toMap()

        _shareCodeMap.value = shareMap

        val serverIds = listResp.decks.map { it.deckId }

        // 2) fetch each deck detail and upsert into Room
        val now = System.currentTimeMillis()


        for (serverDeckId in serverIds) {
            val deckResp = api.getDeck(token, serverDeckId)
            if (!deckResp.success || deckResp.deck == null) {
                // if server says not_owned/not_found, skip it
                continue
            }

            val d = deckResp.deck
            val pairs = d.cards.map { it.cardId to it.qty }

            deckDao.upsertDeckByServerId(
                serverDeckId = d.deckId,
                name = d.name,
                leaderCardId = d.leaderCardId,
                updatedAtEpochMs = now,
                cards = pairs,
                shareCode = shareMap[d.deckId]
            )
        }

        // 3) remove local decks that are no longer owned
        if (serverIds.isEmpty()) {
            deckDao.deleteAllServerDecks() // add DAO below
        } else {
            deckDao.deleteDecksNotInServer(serverIds)
        }
    }
    fun computeDeckHash(
        name: String,
        leaderCardId: Int,
        deckMap: Map<Int, QtyClass> // cardId -> QtyClass(requiredQty, stockQty)
    ): String {
        val normalizedName = name
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()

        val cardsPart = deckMap.entries
            .sortedBy { it.key } // sort by cardId
            .joinToString(separator = ",") { (cardId, qtyClass) ->
                "$cardId:${qtyClass.requiredQty}"
            }

        val canonical = "v1|name=$normalizedName|leader=$leaderCardId|cards=$cardsPart"

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") { "%02x".format(it) } // 64-char hex
    }

    suspend fun saveNewDeckServerFirst(
        token: String,
        name: String,
        leaderCardId: Int,
        deckMap: Map<Int, QtyClass>
    ): Long {
        val now = System.currentTimeMillis()

        // 1) Convert deckMap -> request cards
        val cardsReq = deckMap.entries
            .sortedBy { it.key }
            .map { (cardId, qtyClass) ->
                DeckCardReqDto(cardId = cardId, qty = qtyClass.requiredQty)
            }

        // 2) Compute hash (name changes => new deck, as you wanted)
        val deckHash = computeDeckHash(name, leaderCardId, deckMap)

        // 3) Call server FIRST
        val resp = api.createDeck(
            DeckCreateRequestDto(
                token = token,
                name = name,
                leaderCardId = leaderCardId,
                cards = cardsReq,
                deckHash = deckHash
            )
        )

        if (!resp.success || resp.deckId == null) {
            throw IllegalStateException("Server createDeck failed: ${resp.error ?: "unknown"}")
        }

        val serverDeckId = resp.deckId

        // 4) Mirror into Room SECOND (single transaction ideally)
        val localDeckId = deckDao.insertDeck(
            DeckEntity(
                name = name,
                leaderCardId = leaderCardId,
                serverDeckId = serverDeckId,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )

        val deckCards = deckMap.map { (cardId, qtyClass) ->
            DeckCardEntity(deckId = localDeckId, cardId = cardId, qty = qtyClass.requiredQty)
        }
        deckDao.insertDeckCards(deckCards)

        return localDeckId
    }

    suspend fun overwriteExistingDeckServerFirst(
        token: String,
        deckId: Long,                      // local deckId (old deck)
        name: String,
        leaderCardId: Int,
        deckMap: Map<Int, QtyClass>
    ): Long? {
        val now = System.currentTimeMillis()

        // 1) Need serverDeckId to authorize update on server
        val oldServerDeckId = deckDao.getServerDeckId(deckId)
            ?: throw IllegalStateException("No serverDeckId for local deckId=$deckId (not synced?)")

        Log.d("DeckUpdate", "localDeckId=$deckId oldServerDeckId=$oldServerDeckId")

        // 2) Build request cards
        val cardsReq = deckMap.entries
            .sortedBy { it.key }
            .map { (cardId, qtyClass) ->
                DeckCardReqDto(cardId = cardId, qty = qtyClass.requiredQty)
            }

        // 3) Hash
        val deckHash = computeDeckHash(name, leaderCardId, deckMap)

        // 4) Call server
        val req = DeckUpdateRequestDto(
            token = token,
            oldDeckId = oldServerDeckId,
            name = name,
            leaderCardId = leaderCardId,
            cards = cardsReq,
            deckHash = deckHash
        )

        val resp = api.updateDeck(req)
        if (!resp.success || resp.newDeckId == null) {
            throw IllegalStateException("Server updateDeck failed: ${resp.error ?: "unknown"}")
        }

        // Server returns whether it actually changed
        val changed = resp.changed == true
        val newServerDeckId = resp.newDeckId

        if (!changed) {
            // Same hash => do nothing locally
            Log.d("DeckUpdate", "No changes detected (hash same). Keeping existing deck.")
            return null
        }

        // 5) Create NEW local deck (keep old one)
        val newLocalDeckId = deckDao.insertDeck(
            DeckEntity(
                name = name,
                leaderCardId = leaderCardId,
                serverDeckId = newServerDeckId,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )

        val newDeckCards = deckMap.map { (cardId, qtyClass) ->
            DeckCardEntity(deckId = newLocalDeckId, cardId = cardId, qty = qtyClass.requiredQty)
        }
        deckDao.insertDeckCards(newDeckCards)

        return newLocalDeckId
    }
    suspend fun deleteDeckServerFirst(
        token: String,
        deckId: Long // local deckId
    ) {
        // 1) Get serverDeckId for API
        val serverDeckId = deckDao.getServerDeckId(deckId)
            ?: throw IllegalStateException("No serverDeckId for local deckId=$deckId (not synced?)")

        // 2) Call server delete (remove ownership)
        val resp = api.deleteDeck(
            DeckDeleteRequestDto(
                token = token,
                deckId = serverDeckId
            )
        )

        if (!resp.success) {
            throw IllegalStateException("Server deleteDeck failed: ${resp.error ?: "unknown"}")
        }

        // Optional: if removed == false, means server says you didn't own it anymore.
        // You can still delete locally to match "no longer visible".
        // if (resp.removed == false) { ... }

        // 3) Delete locally
        deckDao.clearDeckCards(deckId)
        deckDao.deleteDeck(deckId)
    }

    suspend fun importDeckByShareCode(token: String, shareCode: String): DeckImportResponseDto {
        val resp = api.importDeckByShareCode(token, shareCode.trim())
        if (!resp.success) throw IllegalStateException(resp.error ?: "import_failed")
        return resp
    }
}
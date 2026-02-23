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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository responsible for deck persistence and server synchronization.
 *
 * Responsibilities:
 * - Provide local deck read/observe APIs backed by Room (DeckDao)
 * - Synchronize owned decks from server into local Room cache
 * - Perform server-first mutations (create/update/delete) to ensure server is authoritative
 * - Maintain a local shareCode map for quick UI display (serverDeckId -> shareCode)
 *
 * Note:
 * This repository does not implement deck-building or rule validation logic.
 * It only handles storage, retrieval, and server synchronization of decks.
 */
class DeckRepository(private val deckDao: DeckDao ,private val api: DeckApi) {

    /**
     * In-memory map of server deck IDs to share codes.
     *
     * Purpose:
     * - Deck list UI often needs to show share code without fetching each deck again.
     * - Updated whenever syncOwnedDecksFromServer() is called.
     */
    private val _shareCodeMap = MutableStateFlow<Map<Long, String>>(emptyMap())

    /** Public read-only StateFlow for share code display usage in UI. */
    val shareCodeMap = _shareCodeMap.asStateFlow()

    /**
     * Retrieves a deck (local Room) by local deck ID, including its card composition.
     *
     * @param deckId Local Room deckId.
     * @return DeckWithCards if found, otherwise null.
     */
    suspend fun getDeck(deckId: Long): DeckWithCards? = deckDao.getDeck(deckId)

    /**
     * Observes all local decks (Room) reactively for UI updates.
     *
     * @return Flow of decks ordered by updatedAtEpochMs (as defined in DAO query).
     */
    fun observeAllDecks(): kotlinx.coroutines.flow.Flow<List<DeckWithCards>> {
        return deckDao.observeAllDecks()
    }

    /**
     * Synchronizes owned decks from the server into local Room storage.
     *
     * Workflow:
     * 1) Fetch owned deck summaries from server (IDs + share codes)
     * 2) For each server deck ID, fetch full deck detail and upsert into Room
     * 3) Delete local server-synced decks that are no longer owned
     *
     * This ensures local cache reflects server-authoritative ownership.
     *
     * @param token Session token for authentication.
     * @throws IllegalStateException if deck list endpoint fails.
     */
    suspend fun syncOwnedDecksFromServer(token: String) {
        // 1) Fetch owned deck summaries (server IDs)
        val listResp = api.getOwnedDeckSummaries(token)
        if (!listResp.success || listResp.decks == null) {
            throw IllegalStateException("deck_list_owned failed: ${listResp.error ?: "unknown"}")
        }

        // Build share-code map for quick UI access: serverDeckId -> shareCode
        val shareMap: Map<Long, String> = listResp.decks
            .mapNotNull { s -> s.shareCode?.let { code -> s.deckId to code } }
            .toMap()

        _shareCodeMap.value = shareMap

        val serverIds = listResp.decks.map { it.deckId }

        // 2) Fetch each deck detail and upsert into Room
        val now = System.currentTimeMillis()


        for (serverDeckId in serverIds) {
            val deckResp = api.getDeck(token, serverDeckId)
            if (!deckResp.success || deckResp.deck == null) {
                // If server returns not_owned/not_found, skip this deck safely
                continue
            }

            val d = deckResp.deck
            val pairs = d.cards.map { it.cardId to it.qty }

            // Upsert by server ID so local mapping remains stable across sync cycles
            deckDao.upsertDeckByServerId(
                serverDeckId = d.deckId,
                name = d.name,
                leaderCardId = d.leaderCardId,
                updatedAtEpochMs = now,
                cards = pairs,
                shareCode = shareMap[d.deckId]
            )
        }

        // 3) Cleanup: remove local decks that are no longer present on the server
        if (serverIds.isEmpty()) {
            deckDao.deleteAllServerDecks() // add DAO below
        } else {
            deckDao.deleteDecksNotInServer(serverIds)
        }
    }

    /**
     * Computes a stable hash of a deck based on canonicalized content.
     *
     * Purpose:
     * - Detect identical decks across saves/updates
     * - Allow server-side duplication checks (same composition)
     *
     * Canonical format:
     * - Name is normalized (trim, single spaces, lowercase)
     * - Card entries are sorted by cardId
     * - Only requiredQty is included (stockQty is not part of composition)
     *
     * @param name Deck name.
     * @param leaderCardId Leader card ID.
     * @param deckMap Map of cardId -> QtyClass(requiredQty, stockQty).
     * @return SHA-256 hash as 64-character hex string.
     */
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

    /**
     * Creates a new deck using a server-first strategy.
     *
     * Rationale:
     * - Server is treated as the authoritative source of ownership and IDs.
     * - Local Room cache mirrors the server state after successful creation.
     *
     * Workflow:
     * 1) Convert deckMap to request DTO list
     * 2) Compute deck hash
     * 3) Call create endpoint (server)
     * 4) Insert resulting deck locally with serverDeckId mapping
     *
     * @return Newly created local deckId (Room primary key).
     * @throws IllegalStateException if server creation fails.
     */
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

        // 2) Compute hash
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

    /**
     * Updates an existing deck using a server-first strategy.
     *
     * Behavior:
     * - Server is updated first using the serverDeckId linked to the local deck.
     * - If server reports "no changes" (same hash), local storage is not modified.
     * - If changed, a new local deck entry is created (old deck preserved).
     *
     * @param deckId Local deckId to update from.
     * @return New local deckId if a new deck is created, or null if no change.
     * @throws IllegalStateException if local deck has no serverDeckId or server update fails.
     */
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

    /**
     * Deletes a deck using a server-first strategy.
     *
     * Workflow:
     * 1) Resolve serverDeckId from local deckId
     * 2) Call server delete endpoint (ownership removal)
     * 3) Remove local deck and associated deck_cards
     *
     * @throws IllegalStateException if deck is not linked to server or server call fails.
     */
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

        // Remove locally to match server-owned list visibility
        deckDao.clearDeckCards(deckId)
        deckDao.deleteDeck(deckId)
    }

    /**
     * Imports a deck using a share code.
     *
     * Server responsibilities:
     * - Validate session token
     * - Resolve share code to a deck record
     * - Add deck to user's owned list if allowed
     *
     * This call returns the server response. Local caching can be done by caller,
     * or via syncOwnedDecksFromServer() after import.
     *
     * @param token Session token.
     * @param shareCode User-entered share code (trimmed).
     * @return DeckImportResponseDto containing status and optional imported deck data.
     * @throws IllegalStateException if import fails.
     */
    suspend fun importDeckByShareCode(token: String, shareCode: String): DeckImportResponseDto {
        val resp = api.importDeckByShareCode(token, shareCode.trim())
        if (!resp.success) throw IllegalStateException(resp.error ?: "import_failed")
        return resp
    }
}
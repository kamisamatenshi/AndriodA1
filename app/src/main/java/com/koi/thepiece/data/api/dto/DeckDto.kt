package com.koi.thepiece.data.api.dto

import com.squareup.moshi.Json

data class DeckCardReqDto(
    @Json(name = "cardId") val cardId: Int,
    @Json(name = "qty") val qty: Int
)

data class DeckCreateRequestDto(
    val token: String,
    val name: String,
    @Json(name = "leader_card_id") val leaderCardId: Int,
    val cards: List<DeckCardReqDto>,
    @Json(name = "deck_hash") val deckHash: String
)

data class DeckUpdateRequestDto(
    val token: String,
    @Json(name = "old_deck_id") val oldDeckId: Long,
    val name: String,
    @Json(name = "leader_card_id") val leaderCardId: Int,
    val cards: List<DeckCardReqDto>,
    @Json(name = "deck_hash") val deckHash: String
)

data class DeckDeleteRequestDto(
    val token: String,
    @Json(name = "deck_id") val deckId: Long
)

data class DeckSummaryDto(
    @Json(name = "deck_id") val deckId: Long,
    val name: String,
    @Json(name = "leader_card_id") val leaderCardId: Int,
    @Json(name = "share_code") val shareCode: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

data class DeckListResponseDto(
    val success: Boolean,
    val decks: List<DeckSummaryDto>?,
    val error: String?
)

data class DeckDto(
    @Json(name = "deck_id") val deckId: Long,
    val name: String,
    @Json(name = "leader_card_id") val leaderCardId: Int,
    val cards: List<DeckCardReqDto>,
    @Json(name = "deck_hash") val deckHash: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "share_code") val shareCode: String?
)

data class DeckGetResponseDto(
    val success: Boolean,
    val deck: DeckDto?,
    val error: String?
)
data class DeckCreateResponseDto(
    val success: Boolean,
    @Json(name = "deck_id") val deckId: Long?,
    val error: String?
)

data class DeckUpdateResponseDto(
    val success: Boolean,
    val changed: Boolean?,
    @Json(name = "new_deck_id") val newDeckId: Long?,
    val error: String?
)

data class DeckDeleteResponseDto(
    val success: Boolean,
    val removed: Boolean?,
    val error: String?
)

data class DeckImportResponseDto(
    val success: Boolean,
    @Json(name = "already_owned") val alreadyOwned: Boolean?,
    val added: Boolean?,
    val deck: DeckDto?,
    val error: String?
)


package com.koi.thepiece.data.api.dto

import com.squareup.moshi.Json

/**
 * Represents a single card entry inside a deck request.
 *
 * Used when creating or updating a deck.
 *
 * @property cardId Database ID of the card.
 * @property qty Quantity of this card in the deck.
 */
data class DeckCardReqDto(
    @Json(name = "cardId") val cardId: Int,
    @Json(name = "qty") val qty: Int
)

/**
 * Request body for creating a new deck.
 *
 * @property token Session token for authentication.
 * @property name Deck name.
 * @property leaderCardId Leader card ID for the deck.
 * @property cards List of cards included in the deck.
 * @property deckHash Hash representation of deck composition (used for duplicate detection).
 */
data class DeckCreateRequestDto(
    val token: String,
    val name: String,
    @Json(name = "leader_card_id") val leaderCardId: Int,
    val cards: List<DeckCardReqDto>,
    @Json(name = "deck_hash") val deckHash: String
)

/**
 * Request body for updating an existing deck.
 *
 * @property token Session token for authentication.
 * @property oldDeckId Existing deck identifier.
 * @property name Updated deck name.
 * @property leaderCardId Updated leader card ID.
 * @property cards Updated card list.
 * @property deckHash Updated deck composition hash.
 */
data class DeckUpdateRequestDto(
    val token: String,
    @Json(name = "old_deck_id") val oldDeckId: Long,
    val name: String,
    @Json(name = "leader_card_id") val leaderCardId: Int,
    val cards: List<DeckCardReqDto>,
    @Json(name = "deck_hash") val deckHash: String
)

/**
 * Request body for deleting a deck.
 *
 * @property token Session token for authentication.
 * @property deckId ID of the deck to remove.
 */
data class DeckDeleteRequestDto(
    val token: String,
    @Json(name = "deck_id") val deckId: Long
)

/**
 * Summary representation of a deck (used in deck listing).
 *
 * @property deckId Unique deck identifier.
 * @property name Deck name.
 * @property leaderCardId Leader card ID.
 * @property shareCode Optional share code for deck distribution.
 * @property updatedAt Last updated timestamp.
 */
data class DeckSummaryDto(
    @Json(name = "deck_id") val deckId: Long,
    val name: String,
    @Json(name = "leader_card_id") val leaderCardId: Int,
    @Json(name = "share_code") val shareCode: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

/**
 * Response model for retrieving a list of user decks.
 *
 * @property success Indicates API request success.
 * @property decks List of deck summaries.
 * @property error Optional error message.
 */
data class DeckListResponseDto(
    val success: Boolean,
    val decks: List<DeckSummaryDto>?,
    val error: String?
)

/**
 * Full deck representation including card composition.
 *
 * @property deckId Unique deck identifier.
 * @property name Deck name.
 * @property leaderCardId Leader card ID.
 * @property cards List of cards within the deck.
 * @property deckHash Hash of deck composition (used for validation or duplication checks).
 * @property updatedAt Last updated timestamp.
 * @property shareCode Optional share code.
 */
data class DeckDto(
    @Json(name = "deck_id") val deckId: Long,
    val name: String,
    @Json(name = "leader_card_id") val leaderCardId: Int,
    val cards: List<DeckCardReqDto>,
    @Json(name = "deck_hash") val deckHash: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "share_code") val shareCode: String?
)

/**
 * Response model for retrieving a specific deck.
 *
 * @property success Indicates request success.
 * @property deck Full deck data.
 * @property error Optional error message.
 */
data class DeckGetResponseDto(
    val success: Boolean,
    val deck: DeckDto?,
    val error: String?
)

/**
 * Response model for deck creation.
 *
 * @property success Indicates whether deck was successfully created.
 * @property deckId Newly generated deck ID.
 * @property error Optional error message.
 */
data class DeckCreateResponseDto(
    val success: Boolean,
    @Json(name = "deck_id") val deckId: Long?,
    val error: String?
)

/**
 * Response model for deck update.
 *
 * @property success Indicates request success.
 * @property changed Whether deck content was modified.
 * @property newDeckId New deck ID if update resulted in duplication logic.
 * @property error Optional error message.
 */
data class DeckUpdateResponseDto(
    val success: Boolean,
    val changed: Boolean?,
    @Json(name = "new_deck_id") val newDeckId: Long?,
    val error: String?
)

/**
 * Response model for deck deletion.
 *
 * @property success Indicates request success.
 * @property removed Whether deck was successfully removed.
 * @property error Optional error message.
 */
data class DeckDeleteResponseDto(
    val success: Boolean,
    val removed: Boolean?,
    val error: String?
)

/**
 * Response model for deck import via share code.
 *
 * @property success Indicates request success.
 * @property alreadyOwned Whether user already owns this deck.
 * @property added Whether deck was newly added.
 * @property deck Imported deck data.
 * @property error Optional error message.
 */
data class DeckImportResponseDto(
    val success: Boolean,
    @Json(name = "already_owned") val alreadyOwned: Boolean?,
    val added: Boolean?,
    val deck: DeckDto?,
    val error: String?
)
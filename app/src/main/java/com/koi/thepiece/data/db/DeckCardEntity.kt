package com.koi.thepiece.data.db

import androidx.room.Entity
import androidx.room.Index

/**
 * Junction entity representing the many-to-many relationship
 * between decks and cards.
 *
 * Each row represents a specific card within a specific deck,
 * along with the quantity of that card.
 *
 * Composite primary key (deckId + cardId) ensures:
 * - A card cannot appear more than once in the same deck.
 * - Quantity updates replace the existing row instead of duplicating it.
 *
 * Indexes improve query performance when:
 * - Retrieving all cards in a deck
 * - Finding which decks contain a specific card
 */
@Entity(
    tableName = "deck_cards",
    primaryKeys = ["deckId", "cardId"],
    indices = [
        Index("deckId"),  // Optimizes deck-to-cards lookup
        Index("cardId")   // Optimizes reverse lookup (optional queries)
    ]
)
data class DeckCardEntity(

    /** ID of the deck this card belongs to. */
    val deckId: Long,

    /** ID of the card stored in the deck. */
    val cardId: Int,

    /** Quantity of this card within the deck. */
    val qty: Int
)
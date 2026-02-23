package com.koi.thepiece.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a locally stored deck.
 *
 * This entity stores deck metadata and maintains linkage
 * to both local and server-side identifiers.
 *
 * The actual card composition of the deck is stored separately
 * in the `deck_cards` junction table (DeckCardEntity).
 */
@Entity(tableName = "decks")
data class DeckEntity(

    /**
     * Local primary key for the deck.
     *
     * Auto-generated to uniquely identify the deck
     * within the local Room database.
     */
    @PrimaryKey(autoGenerate = true)
    val deckId: Long = 0L,

    /** User-defined deck name. */
    val name: String,

    /**
     * ID of the leader card associated with this deck.
     *
     * Stored separately to allow quick access without
     * scanning the deck_cards table.
     */
    val leaderCardId: Int,

    /**
     * Server-side deck identifier.
     *
     * Null if the deck has not been synchronized with the server.
     * Used for local–remote mapping during synchronization.
     */
    val serverDeckId: Long? = null,

    /**
     * Timestamp of deck creation (epoch milliseconds).
     */
    val createdAtEpochMs: Long,

    /**
     * Timestamp of last modification (epoch milliseconds).
     *
     * Used for:
     * - Sorting decks
     * - Synchronization comparison
     */
    val updatedAtEpochMs: Long
)
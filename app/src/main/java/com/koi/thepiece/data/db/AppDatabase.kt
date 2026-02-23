package com.koi.thepiece.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Main Room database definition for the application.
 *
 * This database serves as the local persistence layer and is used to:
 * - Cache card data retrieved from the backend
 * - Store locally saved decks (if applicable)
 * - Maintain deck-to-card relationships
 *
 * The database acts as a performance optimization layer,
 * reducing repeated network requests while maintaining
 * synchronization with the server-authoritative data.
 */
@Database(
    entities = [
        CardEntity::class,
        DeckEntity::class,
        DeckCardEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to card-related database operations.
     */
    abstract fun cardDao(): CardDao

    /**
     * Provides access to deck-related database operations.
     */
    abstract fun deckDao(): DeckDao
}
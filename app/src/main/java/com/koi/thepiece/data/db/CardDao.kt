package com.koi.thepiece.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for card-related database operations.
 *
 * This DAO manages:
 * - Local caching of card metadata
 * - Owned quantity updates
 * - Synchronization support between server and local storage
 *
 * The local database acts as a performance optimization layer
 * while the backend remains the authoritative data source.
 */
@Dao
interface CardDao {

    /**
     * Observes all cards stored locally.
     *
     * Returns a Flow to support reactive UI updates
     * whenever card data changes.
     */
    @Query("SELECT * FROM cards ORDER BY code ASC, name ASC")
    fun observeAll(): Flow<List<CardEntity>>

    /**
     * Retrieves all cards once without observation.
     *
     * Used for one-time operations such as data synchronization.
     */
    @Query("SELECT * FROM cards ORDER BY code ASC, name ASC")
    suspend fun getAllOnce(): List<CardEntity>

    /**
     * Inserts or updates a list of card entities.
     *
     * Uses REPLACE strategy to ensure:
     * - Updated server data overwrites outdated local records.
     * - No duplicate primary keys exist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CardEntity>)

    /**
     * Updates the owned quantity of a specific card locally.
     *
     * @param id Card ID.
     * @param newQty Updated owned quantity.
     * @param now Timestamp used to track last modification time.
     */
    @Query("UPDATE cards SET ownedQty = :newQty, updatedAtEpochMs = :now WHERE id = :id")
    suspend fun updateOwnedQty(id: Int, newQty: Int, now: Long)

    /**
     * Clears all locally cached card records.
     *
     * Typically used before full re-synchronization with server data.
     */
    @Query("DELETE FROM cards")
    suspend fun clear()

    /**
     * Retrieves the owned quantity of a specific card by ID.
     *
     * Returns null if the card does not exist locally.
     */
    @Query("SELECT ownedQty FROM cards WHERE id = :id LIMIT 1")
    suspend fun getOwnedQtyById(id: Int): Int?

    /**
     * Retrieves all card IDs with their corresponding owned quantities.
     *
     * Used for:
     * - Completion percentage calculation
     * - Inventory reconciliation
     */
    @Query("SELECT id, ownedQty FROM cards")
    suspend fun getAllOwnedQty(): List<OwnedQtyRow>
}
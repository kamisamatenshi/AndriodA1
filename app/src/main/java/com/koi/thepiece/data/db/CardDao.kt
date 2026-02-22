package com.koi.thepiece.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY code ASC, name ASC")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY code ASC, name ASC")
    suspend fun getAllOnce(): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CardEntity>)

    @Query("UPDATE cards SET ownedQty = :newQty, updatedAtEpochMs = :now WHERE id = :id")
    suspend fun updateOwnedQty(id: Int, newQty: Int, now: Long)

    @Query("DELETE FROM cards")
    suspend fun clear()

    @Query("SELECT ownedQty FROM cards WHERE id = :id LIMIT 1")
    suspend fun getOwnedQtyById(id: Int): Int?

    @Query("SELECT id, ownedQty FROM cards")
    suspend fun getAllOwnedQty(): List<OwnedQtyRow>
}

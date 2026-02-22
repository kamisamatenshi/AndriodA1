package com.koi.thepiece.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val deckId: Long = 0L,
    val name: String,
    val leaderCardId: Int,              // <- associate leader here
    val serverDeckId: Long? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
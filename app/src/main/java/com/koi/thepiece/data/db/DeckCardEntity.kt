package com.koi.thepiece.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "deck_cards",
    primaryKeys = ["deckId", "cardId"],
    indices = [Index("deckId"), Index("cardId")]
)
data class DeckCardEntity(
    val deckId: Long,
    val cardId: Int,
    val qty: Int
)
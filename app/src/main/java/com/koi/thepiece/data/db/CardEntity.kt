package com.koi.thepiece.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cards",
    indices = [
        Index(value = ["code"]),
        Index(value = ["name"]),
        Index(value = ["color"]),
        Index(value = ["type"]),
        Index(value = ["cardSet"])
    ]
)
data class CardEntity(
    @PrimaryKey val id: Int,

    val code: String?,
    val name: String,

    val color: String,
    val type: String,
    val cardSet: String?,
    val rarity: String?,

    val ownedQty: Int,

    val imageUrl: String,
    val yuyuUrl: String?,
    val obtainFrom: String?,
    val traits: String?,
    val cost: String?,
    val skillJp: String?,
    val skillEn: String?,
    val price:Int?,
    val updatedAtEpochMs: Long
)

data class OwnedQtyRow(
    val id: Int,
    val ownedQty: Int
)
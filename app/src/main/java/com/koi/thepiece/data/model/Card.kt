package com.koi.thepiece.data.model

data class Card(
    val id: Int,
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
    val skillEn: String?
)

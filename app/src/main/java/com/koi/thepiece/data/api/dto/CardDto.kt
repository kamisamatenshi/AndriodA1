package com.koi.thepiece.data.api.dto

import com.squareup.moshi.Json

data class CardDto(
    val id: Int?,

    val code: String?,
    val name: String?,

    val color: String?,
    val type: String?,

    @Json(name = "card_set")
    val cardSet: String?,

    val rarity: String?,

    @Json(name = "owned_qty")
    val ownedQty: Int?,

    @Json(name = "image")
    val imageUrl: String?,

    @Json(name = "yuyu_url")
    val yuyuUrl: String?,

    @Json(name = "obtain_from")
    val obtainFrom: String?,

    val traits: String?,
    val cost: String?,

    @Json(name = "skill_jp")
    val skillJp: String?,

    @Json(name = "skill_en")
    val skillEn: String?
)

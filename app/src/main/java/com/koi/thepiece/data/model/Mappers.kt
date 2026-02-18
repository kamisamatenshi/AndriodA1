package com.koi.thepiece.data.model

import com.koi.thepiece.data.api.dto.CardDto
import com.koi.thepiece.data.db.CardEntity

fun CardEntity.toDomain(): Card = Card(
    id = id,
    code = code,
    name = name,
    color = color,
    type = type,
    cardSet = cardSet,
    rarity = rarity,
    ownedQty = ownedQty,
    imageUrl = imageUrl,
    yuyuUrl = yuyuUrl,
    obtainFrom = obtainFrom,
    traits = traits,
    cost = cost,
    skillJp = skillJp,
    skillEn = skillEn
)

fun CardDto.toEntity(now: Long): CardEntity? {
    val safeId = id ?: return null
    val safeName = name ?: return null
    val safeColor = color ?: "unknown"
    val safeType = type ?: "unknown"
    val safeImage = imageUrl ?: return null

    return CardEntity(
        id = safeId,
        code = code,
        name = safeName,
        color = safeColor,
        type = safeType,
        cardSet = cardSet,
        rarity = deriveRarityKey(rarity, safeName),
        ownedQty = ownedQty ?: 0,
        imageUrl = safeImage,
        yuyuUrl = yuyuUrl,
        obtainFrom = obtainFrom,
        traits = traits,
        cost = cost,
        skillJp = skillJp,
        skillEn = skillEn,
        updatedAtEpochMs = now
    )
}


private fun deriveRarityKey(dtoRarity: String?, name: String?): String {

    val api = dtoRarity?.trim()?.lowercase()
    if (!api.isNullOrBlank()) return api

    val n = name?.lowercase().orEmpty()


    Regex("""\b(p-sec|p-sr|p-r|p-l)\b""").find(n)?.value?.let { return it }


    Regex("""\b(sec|sr|r|l|sp|uc|c)\b""").find(n)?.value?.let { return it }


    return "c"
}

package com.koi.thepiece.data.api.dto

import com.squareup.moshi.Json

/**
 * Data Transfer Object representing a single card record retrieved from the backend.
 *
 * This model maps directly to the `cards` table (and related user-owned data)
 * returned by the get_cards.php endpoint.
 *
 * Some fields use @Json annotations to match backend JSON keys.
 */
data class CardDto(

    /** Unique database identifier for the card. */
    val id: Int?,

    /** Official card code (e.g., OP01-001). */
    val code: String?,

    /** Official card name. */
    val name: String?,

    /** Card color classification (e.g., Red, Blue, Green). */
    val color: String?,

    /** Card type (e.g., Leader, Character, Event, Stage). */
    val type: String?,

    /**
     * Card set identifier (e.g., OP01, OP02).
     * Mapped from backend JSON field "card_set".
     */
    @Json(name = "card_set")
    val cardSet: String?,

    /** Rarity classification (e.g., Common, Rare, Super Rare). */
    val rarity: String?,

    /**
     * Number of copies owned by the authenticated user.
     * Retrieved from `user_cards` table via server-side join.
     */
    @Json(name = "owned_qty")
    val ownedQty: Int?,

    /**
     * URL to the card image.
     * Used by Coil ImageLoader for asynchronous rendering.
     */
    @Json(name = "image")
    val imageUrl: String?,

    /**
     * Yuyutei marketplace URL for price retrieval.
     * Used by get_price.php for server-side scraping.
     */
    @Json(name = "yuyu_url")
    val yuyuUrl: String?,

    /**
     * Source of acquisition (e.g., booster pack name).
     * Mapped from backend JSON field "obtain_from".
     */
    @Json(name = "obtain_from")
    val obtainFrom: String?,

    /** Card trait information (e.g., Straw Hat Crew). */
    val traits: String?,

    /** Card cost value (stored as String to match backend representation). */
    val cost: String?,

    /**
     * Japanese skill description text.
     * Mapped from backend JSON field "skill_jp".
     */
    @Json(name = "skill_jp")
    val skillJp: String?,

    /**
     * English skill description text.
     * Mapped from backend JSON field "skill_en".
     */
    @Json(name = "skill_en")
    val skillEn: String?,

    /**
     * Real-time price value (optional).
     * May be null if not requested or not available.
     */
    val price: Int?
)
package com.koi.thepiece.data.model

/**
 * Domain model representing a card within the application layer.
 *
 * This model is used by the UI and business logic layers.
 * It is independent from:
 * - CardDto (network layer representation)
 * - CardEntity (local database representation)
 *
 * Separation ensures clean architecture and prevents
 * direct coupling between UI and data sources.
 */
data class Card(

    /** Unique identifier of the card. */
    val id: Int,

    /** Official card code (e.g., OP01-001). */
    val code: String?,

    /** Official card name. */
    val name: String,

    /** Card color classification. */
    val color: String,

    /** Card type (Leader, Character, Event, etc.). */
    val type: String,

    /** Card set identifier (e.g., OP01, OP02). */
    val cardSet: String?,

    /** Rarity classification. */
    val rarity: String?,

    /**
     * Number of copies owned by the user.
     * Derived from server data or local cache.
     */
    val ownedQty: Int,

    /** Image URL used for UI rendering. */
    val imageUrl: String,

    /** Marketplace URL used for price retrieval. */
    val yuyuUrl: String?,

    /** Source of acquisition (e.g., booster pack). */
    val obtainFrom: String?,

    /** Card traits (e.g., Straw Hat Crew). */
    val traits: String?,

    /** Card cost value. */
    val cost: String?,

    /** Japanese skill description. */
    val skillJp: String?,

    /** English skill description. */
    val skillEn: String?,

    /**
     * Latest retrieved price value.
     * May be null if price has not been fetched.
     */
    val price: Int?
)
package com.koi.thepiece.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a locally cached card record.
 *
 * This entity mirrors the backend `cards` table structure
 * and additionally stores user-owned quantity information
 * for local caching and UI rendering.
 *
 * Indexed fields improve query performance for:
 * - Search operations
 * - Filtering by set, color, or type
 */
@Entity(
    tableName = "cards",
    indices = [
        Index(value = ["code"]),      // Optimizes lookup by card code
        Index(value = ["name"]),      // Optimizes search by name
        Index(value = ["color"]),     // Optimizes filtering by color
        Index(value = ["type"]),      // Optimizes filtering by type
        Index(value = ["cardSet"])    // Optimizes filtering by set
    ]
)
data class CardEntity(

    /** Unique card identifier (primary key). */
    @PrimaryKey val id: Int,

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

    /** Card rarity classification. */
    val rarity: String?,

    /**
     * Number of copies owned by the authenticated user.
     * Updated through synchronization with backend user_cards table.
     */
    val ownedQty: Int,

    /** Remote image URL for card artwork. */
    val imageUrl: String,

    /** Marketplace URL used for price scraping. */
    val yuyuUrl: String?,

    /** Source of acquisition (e.g., booster pack). */
    val obtainFrom: String?,

    /** Card trait information. */
    val traits: String?,

    /** Card cost value. */
    val cost: String?,

    /** Japanese skill description. */
    val skillJp: String?,

    /** English skill description. */
    val skillEn: String?,

    /** Latest known price value (optional cache). */
    val price: Int?,

    /**
     * Timestamp of last update in epoch milliseconds.
     * Used to track synchronization recency.
     */
    val updatedAtEpochMs: Long
)

/**
 * Lightweight projection model used for retrieving
 * card ownership data efficiently.
 *
 * Used for:
 * - Completion percentage calculations
 * - Inventory reconciliation
 */
data class OwnedQtyRow(
    val id: Int,
    val ownedQty: Int
)
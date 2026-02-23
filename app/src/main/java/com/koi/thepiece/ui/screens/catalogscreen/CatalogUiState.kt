package com.koi.thepiece.ui.screens.catalogscreen

import com.koi.thepiece.data.model.Card

/**
 * Immutable UI state model for the Catalogue screen.
 *
 * This data class represents the single source of truth for the
 * catalogue UI layer. It is typically exposed as a StateFlow
 * from CatalogViewModel and collected inside the Composable.
 *
 * Design principles:
 * - Immutable state (copy() used for updates)
 * - Centralized representation of UI configuration
 * - Clear separation between data, filters, paging and modal state
 */
data class CatalogUiState(

    /**
     * Indicates whether catalogue data is currently loading.
     * Used to control header loading indicator.
     */
    val loading: Boolean = true,

    /**
     * Holds an error message when network or refresh fails.
     * Null indicates no error.
     */
    val error: String? = null,

    /**
     * Full list of cards retrieved (usually from Room cache
     * synchronized with server).
     *
     * Filtering, paging, and search operations are derived
     * from this master list.
     */
    val allCards: List<Card> = emptyList(),

    // -------------------------
    // Filters
    // -------------------------

    /**
     * Active color filter.
     * "all" disables filtering.
     */
    val color: String = "all",

    /**
     * Active card type filter.
     * Example values: Leader, Event, Stage, Don.
     */
    val cardType: String = "all",

    /**
     * Active set filter.
     * Example values: OP01, EB04, ST29, or "all".
     */
    val setFilter: String = "all",

    /**
     * Active rarity filter.
     * Example values: SR, SEC, SP, C, or "all".
     */
    val rarityFilter: String = "all",

    // -------------------------
    // Search
    // -------------------------

    /**
     * Current user-entered search query.
     * Applied to code, name, traits, etc.
     */
    val searchQuery: String = "",

    /**
     * Optional translated / normalized search query (e.g., JP variant).
     * Used to support multilingual search logic.
     */
    val searchQueryJa: String = "",

    // -------------------------
    // Paging
    // -------------------------

    /**
     * Number of cards per page.
     * Used for pagedCards() calculation.
     */
    val pageSize: Int = 20,

    /**
     * Current page index (1-based).
     */
    val page: Int = 1,

    // -------------------------
    // Modal / Selection
    // -------------------------

    /**
     * Currently selected card (optional reference).
     * May be redundant if selectedID is authoritative.
     */
    val selected: Card? = null,

    /**
     * ID of the selected card used to display preview dialog.
     * Used to derive selectedCard from allCards.
     */
    val selectedID: Int? = null,

    // -------------------------
    // Pricing
    // -------------------------

    /**
     * Optional current price field.
     * Not strictly required if pricing is handled via separate map/state,
     * but retained for flexibility.
     */
    val price: Int = 0
)
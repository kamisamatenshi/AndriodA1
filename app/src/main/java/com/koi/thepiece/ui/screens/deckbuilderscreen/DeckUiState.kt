package com.koi.thepiece.ui.screens.deckbuilderscreen

import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.DeckLegalityResult

/**
 * Represents quantity information for a card inside a deck.
 *
 * requiredQty:
 * - Number of copies currently placed in the deck.
 *
 * stockQty:
 * - Number of copies owned by the user (from collection).
 * - Used to visually indicate over-limit or insufficient stock.
 */
data class QtyClass(
    val requiredQty: Int,
    val stockQty: Int
)

/**
 * Immutable UI state model for the Deck Builder screen.
 *
 * This state model was derived from CatalogUiState,
 * reusing catalogue filtering, search, and paging structure,
 * and extending it with deck-specific data such as:
 * - selected leader
 * - deck composition
 * - deck persistence metadata
 * - legality validation
 *
 * Exposed as StateFlow from DeckViewModel and collected in the Composable.
 *
 * Design principles:
 * - Immutable state (updated via copy())
 * - Single source of truth for the entire screen
 * - Clear separation between catalogue browsing and deck composition
 */
data class DeckUiState(

    // -------------------------
    // Loading / Error
    // -------------------------

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

    // -------------------------
    // Catalogue Source (Shared with CatalogUiState)
    // -------------------------

    /**
     * Full list of cards retrieved (usually from Room cache
     * synchronized with server).
     *
     * Filtering, paging, and search operations are derived
     * from this master list.
     */
    val allCards: List<Card> = emptyList(),

    // -------------------------
    // Filters (Shared with CatalogUiState)
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
    // Search (Shared with CatalogUiState)
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
    // Paging (Shared with CatalogUiState)
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
    // Modal / Selection (Shared with CatalogUiState)
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
    val price: Int = 0,

    // ============================================================
    // Deck-Specific Extensions (Not present in CatalogUiState)
    // ============================================================

    /**
     * Currently selected Leader card.
     * Required for deck legality validation.
     */
    val selectedLeader: Card? = null,

    /**
     * Deck composition map.
     * Key: cardId
     * Value: QtyClass (requiredQty + stockQty)
     */
    val deck: Map<Int, QtyClass> = emptyMap(),

    /**
     * Server-assigned deck ID.
     * Null indicates a new, unsaved deck.
     */
    val deckId: Long? = null,

    /** Display name of the deck. */
    val deckName: String = "",

    /**
     * Optional editing reference.
     * Used to differentiate between new deck creation and editing flow.
     */
    val editingDeckId: Long? = null,

    /**
     * Result of deck legality validation.
     * Produced by DeckLegality.check().
     */
    val legality: DeckLegalityResult? = null,
)
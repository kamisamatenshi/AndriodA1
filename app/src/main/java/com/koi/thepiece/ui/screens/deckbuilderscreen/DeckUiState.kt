package com.koi.thepiece.ui.screens.deckbuilderscreen

import com.koi.thepiece.data.model.Card

data class QtyClass(
    val requiredQty: Int,
    val stockQty: Int
)
data class DeckUiState(
    val loading: Boolean = true,
    val error: String? = null,

    val allCards: List<Card> = emptyList(),

    // filters
    val color: String = "all",
    val cardType: String = "all",
    val setFilter: String = "all",
    val rarityFilter: String = "all",

    // search
    val searchQuery: String = "",

    // search translate
    val searchQueryJa: String = "",

    // paging
    val pageSize: Int = 20,
    val page: Int = 1,

    // modal
    val selected: Card? = null,
    val selectedID: Int? = null,

    val price: Int =0,

    val selectedLeader: Card? = null,
    val deck: Map<Int, QtyClass> = emptyMap(), // cardId -> qty

    val deckId: Long? = null,
    val deckName: String = "",
    val editingDeckId: Long? = null,
    val legality: DeckLegalityResult? = null,



)

package com.koi.thepiece.ui.screens.deckbuilderscreen

import com.koi.thepiece.data.model.Card

data class DeckUiState(
    val loading: Boolean = true,
    val error: String? = null,

    val allCards: List<Card> = emptyList(),

    // filters
    val colorOrType: String = "all",
    val setFilter: String = "all",
    val rarityFilter: String = "all",

    // search
    val searchQuery: String = "",

    // paging
    val pageSize: Int = 30,
    val page: Int = 1,

    // modal
    val selected: Card? = null,
    val selectedLeader: Card? = null,
    val deck: Map<Int, Int> = emptyMap()
    // key = card.id, value = quantity

)

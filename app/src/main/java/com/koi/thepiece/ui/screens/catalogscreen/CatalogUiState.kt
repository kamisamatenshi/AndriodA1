package com.koi.thepiece.ui.screens.catalogscreen

import com.koi.thepiece.data.model.Card

data class CatalogUiState(
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

    // paging
    val pageSize: Int = 30,
    val page: Int = 1,

    // modal
    val selected: Card? = null
)

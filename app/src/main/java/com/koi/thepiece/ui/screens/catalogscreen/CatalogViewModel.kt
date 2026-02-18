package com.koi.thepiece.ui.screens.catalogscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koi.thepiece.AppGraph
import com.koi.thepiece.data.model.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class CatalogViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppGraph.provideCatalogRepository(app)

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state

    init {
        // Observe local DB (offline cache)
        viewModelScope.launch {
            repo.observeCards().collect { cards ->
                _state.update { it.copy(allCards = cards, loading = false, error = null) }
                // keep page in range after updates
                clampPage()
            }
        }

        // Refresh in background (network -> Room upsert)
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = repo.refreshCards(preloadFirstPageImages = true)
            result.exceptionOrNull()?.let { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Failed to refresh") }
            }
            _state.update { it.copy(loading = false) }
        }
    }




    // -------- Filters / Search --------
    fun setColorOrType(value: String) {
        _state.update { it.copy(colorOrType = value, page = 1) }
    }

    fun setSetFilter(value: String) {
        _state.update { it.copy(setFilter = value, page = 1) }
    }

    fun setRarityFilter(value: String) {
        _state.update { it.copy(rarityFilter = value, page = 1) }
    }

    fun setSearchQuery(value: String) {
        _state.update { it.copy(searchQuery = value, page = 1) }
    }



    // -------- Modal --------
    fun openCard(card: Card) {
        _state.update { it.copy(selected = card) }
    }

    fun closeModal() {
        _state.update { it.copy(selected = null) }
    }

    // -------- Qty updates --------
    fun incrementQty(card: Card) {
        updateQty(card, card.ownedQty + 1)
    }

    fun decrementQty(card: Card) {
        updateQty(card, max(0, card.ownedQty - 1))
    }


    private fun updateQty(card: Card, newQty: Int) {
        viewModelScope.launch {
            val result = repo.updateOwnedQty(card.id, newQty)
            result.exceptionOrNull()?.let { e ->
                _state.update { it.copy(error = e.message ?: "Failed to update qty") }
            }
        }
    }

    // -------- Derived lists (filter + paging) --------
    fun filteredCards(s: CatalogUiState): List<Card> {
        val q = s.searchQuery.trim().lowercase()

        return s.allCards.asSequence()
            .filter { c ->
                val matchColorOrType =
                    s.colorOrType == "all" ||
                            c.color.equals(s.colorOrType, ignoreCase = true) ||
                            c.type.equals(s.colorOrType, ignoreCase = true)

                val matchSet =
                    s.setFilter == "all" ||
                            (c.cardSet ?: "").equals(s.setFilter, ignoreCase = true)

                val selectedRarity = s.rarityFilter.trim().lowercase()
                val cardRarity = (c.rarity ?: "").trim().lowercase()

                val matchRarity =
                    selectedRarity == "all" ||cardRarity== getRareKey( selectedRarity)

                val matchSearch =
                    q.isEmpty() ||
                            (c.code ?: "").lowercase().contains(q) ||
                            c.name.lowercase().contains(q) ||
                            (c.traits ?: "").lowercase().contains(q)

                matchColorOrType && matchSet && matchRarity && matchSearch
            }
            .toList()
    }

    fun totalPages(s: CatalogUiState): Int {
        val total = filteredCards(s).size
        return max(1, ceil(total / s.pageSize.toDouble()).toInt())
    }

    fun pagedCards(s: CatalogUiState): List<Card> {
        val list = filteredCards(s)
        val start = (s.page - 1) * s.pageSize
        val end = min(start + s.pageSize, list.size)
        if (start >= list.size) return emptyList()
        return list.subList(start, end)
    }

    fun nextPage() {
        _state.update { s ->
            val tp = totalPages(s)
            s.copy(page = min(tp, s.page + 1))
        }
    }

    fun prevPage() {
        _state.update { s -> s.copy(page = max(1, s.page - 1)) }
    }

    private fun clampPage() {
        _state.update { s ->
            val tp = totalPages(s)
            s.copy(page = s.page.coerceIn(1, tp))
        }
    }

    private fun getRareKey(display: String) : String
    {
        return RARITY_OPTIONS
            .firstOrNull { it.second.equals(display.trim(), ignoreCase = true) }
            ?.first
            ?: "all"
    }

    private val RARITY_OPTIONS = listOf(
        "all" to "All",
        "p-sec" to "A-SEC",
        "sec" to "SEC",
        "p-sr" to "A-SR",
        "sr" to "SR",
        "p-r" to "A-R",
        "r" to "R",
        "p-l" to "A-L",
        "l" to "L",
        "sp" to "SP",
        "uc" to "UC",
        "c" to "C",
    )

}



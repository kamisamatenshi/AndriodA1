package com.koi.thepiece.ui.screens.catalogscreen

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.ui.screens.catalogscreen.components.CardPreviewDialog
import com.koi.thepiece.ui.screens.catalogscreen.components.CardTileGrid
import com.koi.thepiece.ui.screens.catalogscreen.components.CardTileList
import com.koi.thepiece.ui.screens.catalogscreen.components.CatalogFooter
import com.koi.thepiece.ui.screens.catalogscreen.components.CatalogHeaderBlock
import com.koi.thepiece.ui.screens.catalogscreen.components.FilterBottomSheet
import com.koi.thepiece.ui.screens.catalogscreen.components.PagingRow
import com.koi.thepiece.ui.screens.catalogscreen.components.SearchBarRow

/**
 * Catalogue view layout modes.
 * - GRID: dense browsing experience for large card lists
 * - LIST: readability-focused layout for scanning names and quick edits
 */
private enum class CatalogViewMode { GRID, LIST }

/**
 * Main catalogue screen.
 *
 * Responsibilities:
 * - Provides browsing experience for card database with pagination
 * - Supports grid/list view switching
 * - Provides filter and search UI (set, color, rarity, type, query)
 * - Displays set completion progress derived from owned quantities
 * - Displays total net worth (JPY base, optional SGD view) computed from owned quantities and prices
 * - Opens card detail dialog with zoom, skill display (JP/EN), metadata, price and quantity editing
 *
 * Architecture:
 * - Uses CatalogViewModel as the single source of truth for catalogue UI state
 * - Reads state as a StateFlow (vm.state) and renders UI reactively
 * - Delegates all state mutations to ViewModel methods (e.g., setSetFilter, incrementQty)
 *
 * External dependencies:
 * - ImageLoader is injected for consistent caching and rendering of card images
 * - AudioManager is injected for UI feedback sounds (e.g., back button click)
 *
 * @param onBack Callback for navigation pop behavior.
 * @param imageLoader Shared Coil ImageLoader instance.
 * @param audio Audio manager used for click / UI feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onBack: () -> Unit,
    imageLoader: ImageLoader,
    audio: AudioManager
)
 {
     // ViewModel provisioning via factory (requires Application + Context)
    val context = LocalContext.current
    val app = LocalContext.current.applicationContext as Application
    val vm: CatalogViewModel = viewModel(factory = CatalogViewModelFactory(app,context))

     /**
      * Screen UI state:
      * Contains full card list, active filters, paging, loading/error state, and current selection.
      */
    val s by vm.state.collectAsState()

     /**
      * Initial set selection behavior:
      * When entering the catalogue, if no set is selected (all), default to OP01.
      * This ensures the completion indicator and paging are meaningful by default.
      */
    LaunchedEffect(Unit) {
        if (s.setFilter == "all") vm.setSetFilter("OP01")
    }

    //Local UI state for view mode and filter sheet visibility
    var viewMode by rememberSaveable { mutableStateOf(CatalogViewMode.GRID) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

     /**
      * Derived view data:
      * - cards: paged + filtered cards for the current view
      * - totalPages: total pages after applying filters/search
      *
      * remember(...) prevents re-running list computations unless inputs change.
      */
    val cards = remember(s.allCards, s.color , s.cardType , s.setFilter, s.rarityFilter, s.searchQuery, s.page, s.pageSize) {
        vm.pagedCards(s)
    }
    val totalPages = remember(s.allCards, s.color, s.cardType, s.setFilter, s.rarityFilter, s.searchQuery, s.pageSize) {
        vm.totalPages(s)
    }

     /**
      * Net worth display state:
      * totalNetWorth is treated as JPY source-of-truth and can be displayed as SGD via toggle.
      */
     val totalNetWorth by vm.totalNetWorth.collectAsState(initial = 0)
     val isSgd by vm.isSgd.collectAsState(initial = false)

     /**
      * Filter bottom sheet overlay.
      * Appears when showFilters is true and updates ViewModel filter state through callbacks.
      */
    if (showFilters) {
        FilterBottomSheet(
            currentSet = s.setFilter,
            currentColor = s.color,
            currentRarity = s.rarityFilter,
            currentType = s.cardType,
            onSetChange = vm::setSetFilter,
            onColorChange = vm::setColor,
            onCardTypeChange = vm::setCardType,
            onRarityChange = vm::setRarityFilter,
            onClear = {
                vm.setSetFilter("all")
                vm.setColor("all")
                vm.setRarityFilter("all")
                vm.setCardType("all")
            },
            onDismiss = { showFilters = false }
        )
    }

     /**
      * Scaffold structure:
      * - TopAppBar: navigation back, view mode toggle, filter action
      * - Bottom bar: net worth and currency toggle
      * - Content: header (completion), search, paging, and grid/list of cards
      */
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Catalog",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        audio.playClick()
                        onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }

                },
                actions = {
                    IconButton(onClick = {
                        viewMode = if (viewMode == CatalogViewMode.GRID) CatalogViewMode.LIST else CatalogViewMode.GRID
                    }) {
                        Icon(
                            imageVector = if (viewMode == CatalogViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Change view"
                        )
                    }
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                    }
                }
            )
        },
        /**
         * Bottom bar: total net worth display and currency toggle.
         * The "safe check" is redundant because totalNetWorth is non-null and vm is always non-null,
         * but kept as a defensive UI guard.
         */
        bottomBar = { if (totalNetWorth != null && vm != null) { // safe check
            CatalogFooter(
                totalNetWorth = totalNetWorth.toDouble(),
                isSgd = isSgd,
                onToggleCurrency = { vm.toggleCurrency() }
            )
        }
        }
    ) { padding ->


        /**
         * Content column:
         * - Header: set completion progress
         * - Search bar with suggestions
         * - Pagination controls
         * - Card results displayed in grid or list mode
         * - Card preview dialog when a card is selected
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Completion header based on selected set
            CatalogHeaderBlock(
                setCode = s.setFilter,
                loading = s.loading,
                error = s.error,
                viewModel = vm
            )

            // Collect suggestions from ViewModel
            val suggestions by vm.suggestions.collectAsState()

            SearchBarRow(
                query = s.searchQuery,                     // Current search text
                suggestions = suggestions,                 // Suggestions list
                onQueryChange = vm::setSearchQuery,        // Called when user types
                onSuggestionClick = vm::selectSuggestion   // Called when suggestion clicked
            )

            // Page navigation controls
            PagingRow(
                page = s.page,
                totalPages = totalPages,
                onPrev = vm::prevPage,
                onNext = vm::nextPage
            )

            Spacer(Modifier.height(6.dp))

            // Main results view
            when (viewMode) {
                CatalogViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(cards, key = { it.id }) { card ->
                            CardTileGrid(
                                card = card,
                                imageLoader = imageLoader,
                                onClick = { vm.openCard(card) },
                                onPlus = { vm.incrementQty(card) },
                                onMinus = { vm.decrementQty(card) },
                                vm
                            )
                        }
                    }

                }

                CatalogViewMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(cards, key = { it.id }) { card ->
                            CardTileList(
                                card = card,
                                imageLoader = imageLoader,
                                onClick = { vm.openCard(card) },
                                onPlus = { vm.incrementQty(card) },
                                onMinus = { vm.decrementQty(card) }
                            )
                        }
                    }
                }
            }
        }

        /**
         * Card preview dialog:
         * The selected card is identified by selectedID stored in UI state.
         * When present, a dialog is shown allowing detailed view and quantity edits.
         */
        val selectedCard = s.allCards.find { it.id == s.selectedID }
        if (selectedCard != null) {
            CardPreviewDialog(
                card = selectedCard,
                imageLoader = imageLoader,
                onDismiss = vm::closeModal,
                onPlus = { vm.incrementQty(selectedCard) },
                onMinus = { vm.decrementQty(selectedCard) },
                vm
            )
        }
    }
}

package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.catalogscreen.components.PagingRow
import com.koi.thepiece.ui.screens.catalogscreen.components.SearchBarRow
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel

/**
 * Leader selection view layout modes.
 * - GRID: dense browsing experience for quickly scanning leader art
 * - LIST: readability-focused layout for scanning leader names/details
 */
private enum class LeaderViewMode { GRID, LIST }

/**
 * Leader selection screen (deck creation entry).
 *
 * This screen intentionally follows the structural UI pattern defined in CatalogScreen
 * to maintain a consistent browsing experience across modules.
 *
 * UI Alignment with CatalogScreen:
 * - Identical TopAppBar layout (back button + grid/list toggle + filter action)
 * - Identical filter model (set, color, rarity, type)
 * - Identical SearchBarRow + suggestion pipeline
 * - Identical PagingRow pagination logic
 * - Identical Grid/List rendering structure (LazyVerticalGrid / LazyColumn)
 *
 * Purpose of Reuse:
 * - Reduce cognitive load by preserving browsing mechanics across screens
 * - Maintain UI consistency between Catalog, Leader selection, and Deck builder
 * - Allow structural improvements in CatalogScreen to propagate here
 *
 * Responsibilities:
 * - Restricts browsing to Leader cards only
 * - Allows leader preview and confirmation
 * - Passes selected leader into deck creation flow
 *
 * Architecture:
 * - Uses DeckViewModel as the single source of truth
 * - Observes vm.state via StateFlow
 * - Delegates all state mutations to ViewModel methods
 *
 * Side Effects:
 * - Forces ViewModel into leader-pick mode (vm.setLeaderPickMode())
 * - Enforces cardType = "Leader" on entry
 *
 * @param vm DeckViewModel controlling filters, paging, and selection state.
 * @param onBack Callback for navigation pop behavior.
 * @param audio Audio manager used for click / UI feedback.
 * @param imageLoader Shared Coil ImageLoader instance.
 * @param onGoCreateNewDeck Callback invoked when leader is confirmed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderDeckBuildScreen(
    vm: DeckViewModel,
    onBack: () -> Unit,
    audio: AudioManager,
    imageLoader: ImageLoader,
    onGoCreateNewDeck: (Card) -> Unit
) {
    /**
     * Screen UI state:
     * Contains full card list, active filters, paging, loading/error state,
     * and current selected card for leader preview.
     */
    val s by vm.state.collectAsState()

    /**
     * Screen mode:
     * Leader picking requires different behavior semantics compared to normal catalog browsing,
     * e.g., selecting a card should open the leader preview and proceed to create deck.
     */
    vm.setLeaderPickMode()

    /**
     * Initial behavior on entering screen:
     * Ensure this browsing experience is constrained to Leader cards.
     */
    LaunchedEffect(Unit) {
        vm.setCardType("Leader")
    }

    // Local UI state for view mode and filter sheet visibility
    var viewMode by rememberSaveable { mutableStateOf(LeaderViewMode.GRID) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    /**
     * Derived view data:
     * - cards: paged + filtered leader results for current view
     * - totalPages: total pages after applying filters/search
     *
     * remember(...) prevents re-running list computations unless inputs change.
     */
    val cards = remember(
        s.allCards, s.color, s.cardType, s.setFilter, s.rarityFilter, s.searchQuery, s.page, s.pageSize
    ) {
        vm.pagedCards(s)
    }

    val totalPages = remember(
        s.allCards, s.color, s.cardType, s.setFilter, s.rarityFilter, s.searchQuery, s.pageSize
    ) {
        vm.totalPages(s)
    }

    /**
     * Filter bottom sheet overlay:
     * LeaderFilterBottomSheet mirrors the catalog filter sheet structure,
     * but typically constrains type/rarity options for leader selection flow.
     */
    if (showFilters) {
        LeaderFilterBottomSheet(
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
                vm.setCardType("Leader") // keep this screen leader-only
            },
            onDismiss = { showFilters = false }
        )
    }

    /**
     * Scaffold structure:
     * - TopAppBar: back navigation, view mode toggle, filter action
     * - Content: header + search + paging + grid/list of leaders
     * - Modal: leader preview dialog for selection confirmation
     */
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Leader",
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
                        viewMode =
                            if (viewMode == LeaderViewMode.GRID) LeaderViewMode.LIST else LeaderViewMode.GRID
                    }) {
                        Icon(
                            imageVector = if (viewMode == LeaderViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Change view"
                        )
                    }

                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { padding ->

        /**
         * Content column:
         * - Header: leader browsing header block (set/loading/error)
         * - Search bar with suggestions
         * - Pagination controls
         * - Grid/List of leader cards
         * - Leader preview dialog when a leader is selected
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header based on selected set
            LeaderHeaderBlock(
                setCode = s.setFilter,
                loading = s.loading,
                error = s.error
            )

            // Collect suggestions from ViewModel
            val suggestions by vm.suggestions.collectAsState()

            SearchBarRow(
                query = s.searchQuery,                    // Current search text
                suggestions = suggestions,                // Suggestions list
                onQueryChange = vm::setSearchQuery,       // Called when user types
                onSuggestionClick = vm::selectSuggestion  // Called when suggestion clicked
            )

            // Page navigation controls
            PagingRow(
                page = s.page,
                totalPages = totalPages,
                onPrev = vm::prevPage,
                onNext = vm::nextPage
            )

            Spacer(Modifier.height(6.dp))

            // Main results view (leader browsing)
            when (viewMode) {
                LeaderViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(cards, key = { it.id }) { card ->
                            LeaderTileGrid(
                                card = card,
                                imageLoader = imageLoader,
                                onClick = { vm.openCard(card) }
                            )
                        }
                    }
                }

                LeaderViewMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(cards, key = { it.id }) { card ->
                            LeaderTileList(
                                card = card,
                                imageLoader = imageLoader,
                                onClick = { vm.openCard(card) }
                            )
                        }
                    }
                }
            }
        }

        /**
         * Leader preview dialog:
         * Shown when a leader is selected from the grid/list.
         * From this dialog, user confirms and proceeds to deck creation.
         */
        if (s.selected != null) {
            LeaderPreviewDialog(
                card = s.selected!!,
                imageLoader = imageLoader,
                onDismiss = vm::closeModal,
                viewModel = vm,
                onGoCreateNewDeck = onGoCreateNewDeck
            )
        }
    }
}
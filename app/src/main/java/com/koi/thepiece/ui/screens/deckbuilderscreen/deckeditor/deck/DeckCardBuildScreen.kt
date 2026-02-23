package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.ui.screens.catalogscreen.components.PagingRow
import com.koi.thepiece.ui.screens.catalogscreen.components.SearchBarRow
import com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.deck.deckdetails.CardsSection
import com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.deck.deckdetails.LeaderSection
import com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.DeckLegality
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel
import kotlin.collections.get

/**
 * Deck view layout modes.
 * - GRID: dense browsing experience for large card lists
 * - LIST: readability-focused layout for scanning names and quick edits
 */
enum class DeckViewMode { GRID, LIST }

/**
 * Deck Builder main screen.
 *
 * This screen extends the CatalogScreen browsing architecture
 * and layers deck composition functionality beneath it.
 *
 * Structural Similarities to CatalogScreen:
 * - Identical TopAppBar layout:
 *   - Back navigation with audio feedback
 *   - Grid/List toggle for browsing
 *   - Filter bottom sheet trigger
 * - Identical filter model:
 *   - setFilter / color / rarityFilter / cardType
 * - Identical search implementation:
 *   - SearchBarRow with ViewModel-driven suggestions
 * - Identical paging logic:
 *   - PagingRow using ViewModel page/pageSize
 * - Identical grid/list rendering structure:
 *   - LazyVerticalGrid (5 columns)
 *   - LazyColumn
 *
 * Architectural Alignment:
 * - ViewModel is the single source of truth (DeckViewModel)
 * - UI observes vm.state as StateFlow
 * - All mutations delegated to ViewModel methods
 * - Derived lists computed via vm.pagedCards(s)
 *
 * Key Differences (Deck Builder Layer):
 * - Vertical split layout:
 *   - Top half = card browsing (add source)
 *   - Bottom half = deck composition state
 * - Leader + Cards tab system
 * - Deck legality validation via DeckLegality.check()
 * - Deck persistence via vm.saveDeck()
 * - Price aggregation based on deck contents
 * - Separate view mode toggle for deck list/grid
 *
 * Side Effects:
 * - Forces card pick mode on entry (vm.setCardPickMode())
 * - Resets filters to "all" on first composition
 * - Fetches prices for cards currently inside deck
 *
 * @param vm DeckViewModel controlling deck state and mutations.
 * @param onBack Navigation callback.
 * @param audio AudioManager for UI click feedback.
 * @param imageLoader Shared Coil ImageLoader for image consistency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckCardBuildScreen(
    vm: DeckViewModel,
    onBack: () -> Unit,
    audio: AudioManager,
    imageLoader: ImageLoader
) {

    /**
     * Screen UI state:
     * Contains full card list, active filters, paging, selected leader, deck map,
     * loading/error state, and current selected card for preview modal.
     */
    val s by vm.state.collectAsState()
    val leader = s.selectedLeader

    /**
     * Price cache state:
     * prices is keyed by yuyuUrl (or the URL key used in vm.fetchPrice2)
     * and stores fetched card prices for aggregation in the deck summary.
     */
    val prices by vm.prices.collectAsState()

    // Local UI state for dialogs
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var deckName by rememberSaveable { mutableStateOf("") }
    var showValidateDialog by rememberSaveable { mutableStateOf(false) }

    /**
     * Screen mode:
     * Deck builder requires a specialized "card pick mode" so taps behave as
     * "select + add to deck" flows rather than catalogue ownership flows.
     */
    vm.setCardPickMode()

    /**
     * Deck price fetching:
     * Whenever deck contents or card database changes, request prices for each card in deck.
     * ViewModel should dedupe/cache by URL to avoid redundant network calls.
     */
    LaunchedEffect(s.deck, s.allCards) {
        s.deck.keys.forEach { cardId ->
            val card = s.allCards.firstOrNull { it.id == cardId }
            vm.fetchPrice2(card?.yuyuUrl)
        }
    }

    /**
     * Initial behavior on entering screen:
     * - Close any open modal (defensive)
     * - Reset filters so deck builder always starts from a neutral browsing state ("all")
     */
    LaunchedEffect(Unit) {
        vm.closeModal()
        vm.setSetFilter("all")
        vm.setColor("all")
        vm.setRarityFilter("all")
        vm.setCardType("all")
    }

    // Local UI state for view modes and filter sheet visibility
    var catalogViewMode by rememberSaveable { mutableStateOf(DeckViewMode.GRID) }
    var deckViewMode by rememberSaveable { mutableStateOf(DeckViewMode.LIST) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    /**
     * Derived view data (catalog browsing):
     * - cards: filtered + paged results based on ViewModel state
     * - totalPages: computed after applying active filters/search
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
     * Same filter structure as CatalogScreen, but uses DeckViewModel mutations.
     */
    if (showFilters) {
        DeckFilterBottomSheet(
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
     * - TopAppBar: back navigation, catalog view mode toggle, filter action
     * - Content: vertically split into:
     *   (Top) Card browsing (catalog-like)
     *   (Bottom) Deck composition + tabs + actions
     */
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Deck",
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
                        catalogViewMode =
                            if (catalogViewMode == DeckViewMode.GRID) DeckViewMode.LIST else DeckViewMode.GRID
                    }) {
                        Icon(
                            imageVector = if (catalogViewMode == DeckViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
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
         * Root content:
         * Two major regions stacked vertically with equal weight:
         * - Top: browsing results (add source)
         * - Bottom: current deck state (leader + cards)
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            /**
             * Top Region — Catalog browsing:
             * - Header, search, paging
             * - Grid/List of cards (tap opens preview modal)
             */
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                DeckHeaderBlock(
                    setCode = s.setFilter,
                    loading = s.loading,
                    error = s.error
                )

                // Collect suggestions from ViewModel
                val suggestions by vm.suggestions.collectAsState()

                SearchBarRow(
                    query = s.searchQuery,                     // Current search text
                    suggestions = suggestions,                 // Suggestions list
                    onQueryChange = vm::setSearchQuery,        // Called when user types
                    onSuggestionClick = vm::selectSuggestion   // Called when suggestion clicked
                )

                PagingRow(
                    page = s.page,
                    totalPages = totalPages,
                    onPrev = vm::prevPage,
                    onNext = vm::nextPage
                )

                Spacer(Modifier.height(6.dp))

                // Main results view (catalog side)
                when (catalogViewMode) {
                    DeckViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(cards, key = { it.id }) { card ->
                                DeckTileGrid(
                                    card = card,
                                    imageLoader = imageLoader,
                                    onClick = { vm.openCard(card) }
                                )
                            }
                        }
                    }

                    DeckViewMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(cards, key = { it.id }) { card ->
                                DeckTileList(
                                    card = card,
                                    imageLoader = imageLoader,
                                    onClick = { vm.openCard(card) }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            /**
             * Bottom Region — Deck composition:
             * - Summary row (total price + validate/save + view toggle)
             * - Tabs: Leader / Cards
             * - LeaderSection: set/select leader card
             * - CardsSection: shows deck composition in grid/list
             */
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                /**
                 * Deck count + tabs:
                 * totalCards is derived from requiredQty in deck map.
                 */
                val totalCards = s.deck.values.sumOf { it.requiredQty }
                val tabs = listOf("Leader", "Cards ($totalCards/50)")

                /**
                 * Deck price aggregation:
                 * totalYen multiplies fetched yen price by requiredQty.
                 * missingCount is used to show "(…)" when some cards haven't fetched prices yet.
                 */
                val totalYen = s.deck.entries.sumOf { (cardId, qty) ->
                    val card = s.allCards.firstOrNull { it.id == cardId }
                    val yen = prices[card?.yuyuUrl] ?: 0
                    yen * qty.requiredQty
                }

                val missingCount = s.deck.keys.count { cardId ->
                    val card = s.allCards.firstOrNull { it.id == cardId }
                    val url = card?.yuyuUrl
                    url != null && !prices.containsKey(url)
                }

                val priceText = if (missingCount > 0) {
                    "¥${"%,d".format(totalYen)} (…)"
                } else {
                    "¥${"%,d".format(totalYen)}"
                }

                /**
                 * Deck action row:
                 * - Price summary
                 * - Validate (requires leader)
                 * - Save/Update (requires leader + non-empty deck)
                 * - Deck view mode toggle independent of catalog view mode
                 */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showValidateDialog = true },
                            enabled = (s.selectedLeader != null),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) { Text("Validate", maxLines = 1) }

                        val isEditing = (s.deckId != null)

                        OutlinedButton(
                            onClick = {
                                showSaveDialog = true
                                deckName = s.deckName
                            },
                            enabled = (s.selectedLeader != null && s.deck.isNotEmpty()),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) { Text(if (isEditing) "Update Deck" else "Save Deck", maxLines = 1) }

                        IconButton(onClick = {
                            deckViewMode =
                                if (deckViewMode == DeckViewMode.GRID) DeckViewMode.LIST else DeckViewMode.GRID
                        }) {
                            Icon(
                                imageVector = if (deckViewMode == DeckViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = "Change view"
                            )
                        }
                    }
                }

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                // Content rendered OUTSIDE TabRow
                when (selectedTab) {
                    0 -> LeaderSection(
                        state = s,
                        vm = vm,
                        imageLoader = imageLoader,
                        card = leader
                    )
                    1 -> CardsSection(
                        state = s,
                        vm = vm,
                        viewMode = deckViewMode,
                        imageLoader = imageLoader
                    )
                }

                /**
                 * Save deck dialog:
                 * Prompts for deck name then calls vm.saveDeck(deckName).
                 */
                if (showSaveDialog) {
                    AlertDialog(
                        onDismissRequest = { showSaveDialog = false },
                        title = { Text("Save Deck") },
                        text = {
                            Column {
                                Text("Enter a name for this deck.")
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = deckName,
                                    onValueChange = { deckName = it },
                                    singleLine = true,
                                    label = { Text("Deck name") }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    vm.saveDeck(deckName)
                                    showSaveDialog = false
                                    deckName = ""
                                },
                                enabled = deckName.trim().isNotEmpty()
                            ) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                /**
                 * Validate deck dialog:
                 * Runs DeckLegality.check() and shows errors/warnings.
                 */
                if (showValidateDialog) {

                    val result = DeckLegality.check(
                        leader = s.selectedLeader,
                        deckMap = s.deck,
                        allCards = s.allCards,
                        requireExactly50 = true
                    )

                    AlertDialog(
                        onDismissRequest = { showValidateDialog = false },
                        title = {
                            Text(
                                if (result.isLegal) "✅ Deck is Legal" else "❌ Deck Not Legal"
                            )
                        },
                        text = {
                            Column {
                                if (result.isLegal) {
                                    Text("All official rules satisfied.")
                                } else {
                                    result.errors.forEach { error ->
                                        Text("• ${error.message}")
                                    }
                                }

                                if (result.warnings.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    result.warnings.forEach { warning ->
                                        Text("• ${warning.message}")
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showValidateDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }

            /**
             * Card preview dialog (deck builder):
             * Shown when a card is selected from the browsing list.
             * The `normal` flag is derived from card type and gates quantity controls.
             */
            if (s.selected != null) {

                val isNormal =
                    s.selected!!.type.equals("Event", true) ||
                            s.selected!!.type.equals("Stage", true) ||
                            s.selected!!.type.equals("Normal", true)

                DeckPreviewDialog(
                    card = s.selected!!,
                    imageLoader = imageLoader,
                    onDismiss = vm::closeModal,
                    viewModel = vm,
                    normal = isNormal,
                    onAddToDeck = vm::addToDeck
                )
            }
        }
    }
}
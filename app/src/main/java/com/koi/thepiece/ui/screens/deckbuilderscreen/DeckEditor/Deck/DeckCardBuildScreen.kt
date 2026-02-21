package com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.Deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.Deck.DeckDetails.CardsSection
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.Deck.DeckDetails.LeaderSection
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckLegality
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel
import kotlin.collections.get

enum class DeckViewMode { GRID, LIST }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckCardBuildScreen(
    vm: DeckViewModel,
    onBack: () -> Unit,
    audio: AudioManager,
    imageLoader: ImageLoader
) {
    val s by vm.state.collectAsState()
    val leader = s.selectedLeader
    val prices by vm.prices.collectAsState()
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var deckName by rememberSaveable { mutableStateOf("") }
    var showValidateDialog by rememberSaveable { mutableStateOf(false) }

    vm.setCardPickMode()

    LaunchedEffect(s.deck, s.allCards) {
        s.deck.keys.forEach { cardId ->
            val card = s.allCards.firstOrNull { it.id == cardId }
            vm.fetchPrice2(card?.yuyuUrl)
        }
    }

    LaunchedEffect(Unit) {
        vm.closeModal()

        vm.setSetFilter("all")
        vm.setColor("all")
        vm.setRarityFilter("all")
        vm.setCardType("all")
    }

    var catalogViewMode by rememberSaveable { mutableStateOf(DeckViewMode.GRID) }
    var deckViewMode by rememberSaveable { mutableStateOf(DeckViewMode.LIST) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val cards = remember(s.allCards, s.color , s.cardType , s.setFilter, s.rarityFilter, s.searchQuery, s.page, s.pageSize) {
        vm.pagedCards(s)
    }
    val totalPages = remember(s.allCards, s.color, s.cardType, s.setFilter, s.rarityFilter, s.searchQuery, s.pageSize) {
        vm.totalPages(s)
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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

                SearchBarRow(
                    query = s.searchQuery,
                    onQueryChange = vm::setSearchQuery
                )

                PagingRow(
                    page = s.page,
                    totalPages = totalPages,
                    onPrev = vm::prevPage,
                    onNext = vm::nextPage
                )

                Spacer(Modifier.height(6.dp))

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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                val tabs = listOf("Leader", "Cards")

                val totalCards = s.deck.values.sum()

                val totalYen = s.deck.entries.sumOf { (cardId, qty) ->
                    val card = s.allCards.firstOrNull { it.id == cardId }
                    val yen = prices[card?.yuyuUrl] ?: 0
                    yen * qty
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "($totalCards/50)",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // push price+button to the right
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    OutlinedButton(
                        onClick = { showValidateDialog = true },
                        enabled = (s.selectedLeader != null)
                    ) {
                        Text("Validate")
                    }

                    val isEditing = (s.deckId != null)

                    OutlinedButton(
                        onClick = {
                            if (isEditing) {
                                vm.saveDeck(s.deckName)   // update in-place
                            } else {
                                showSaveDialog = true     // ask name once
                                deckName = s.deckName
                            }
                        },
                        enabled = (s.selectedLeader != null && s.deck.isNotEmpty())
                    ) {
                        Text(if (isEditing) "Update Deck" else "Save Deck")
                    }

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
                    1 -> CardsSection(state = s, vm = vm, viewMode = deckViewMode, imageLoader = imageLoader)
                }

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
                                if (result.isLegal)
                                    "✅ Deck is Legal"
                                else
                                    "❌ Deck Not Legal"
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
        }

        if (s.selected != null) {
            DeckPreviewDialog(
                card = s.selected!!,
                imageLoader = imageLoader,
                onDismiss = vm::closeModal,
                viewModel = vm,
                onAddToDeck = vm::addToDeck
            )
        }
    }
}
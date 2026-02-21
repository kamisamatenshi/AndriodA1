package com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.CreateDeck

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
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koi.thepiece.audio.AudioManager
import coil.ImageLoader
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.catalogscreen.components.PagingRow
import com.koi.thepiece.ui.screens.catalogscreen.components.SearchBarRow
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel

private enum class LeaderViewMode { GRID, LIST }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderDeckBuildScreen(
    vm: DeckViewModel,
    onBack: () -> Unit,
    audio: AudioManager,
    imageLoader: ImageLoader,
    onGoCreateNewDeck: (Card) -> Unit
) {
    val s by vm.state.collectAsState()
    vm.setLeaderPickMode()

    LaunchedEffect(Unit) {
        vm.setCardType("Leader")
    }

    var viewMode by rememberSaveable { mutableStateOf(LeaderViewMode.GRID) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val cards = remember(s.allCards, s.color , s.cardType , s.setFilter, s.rarityFilter, s.searchQuery, s.page, s.pageSize) {
        vm.pagedCards(s)
    }
    val totalPages = remember(s.allCards, s.color, s.cardType, s.setFilter, s.rarityFilter, s.searchQuery, s.pageSize) {
        vm.totalPages(s)
    }

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
                vm.setCardType("Leader")
            },
            onDismiss = { showFilters = false }
        )
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LeaderHeaderBlock(
                setCode = s.setFilter,
                loading = s.loading,
                error = s.error
            )

            SearchBarRow(
                query = s.searchQuery,
                suggestions = emptyList(),
                onQueryChange = vm::setSearchQuery,
                onSuggestionClick = { }
            )

            PagingRow(
                page = s.page,
                totalPages = totalPages,
                onPrev = vm::prevPage,
                onNext = vm::nextPage
            )

            Spacer(Modifier.height(6.dp))

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

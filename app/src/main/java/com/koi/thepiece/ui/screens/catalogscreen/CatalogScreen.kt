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


private enum class CatalogViewMode { GRID, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onBack: () -> Unit,
    imageLoader: ImageLoader,
    audio: AudioManager
)
 {
    val context = LocalContext.current
    val app = LocalContext.current.applicationContext as Application
    val vm: CatalogViewModel = viewModel(factory = CatalogViewModelFactory(app,context))
    val s by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        if (s.setFilter == "all") vm.setSetFilter("OP01")
    }

    var viewMode by rememberSaveable { mutableStateOf(CatalogViewMode.GRID) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val cards = remember(s.allCards, s.color , s.cardType , s.setFilter, s.rarityFilter, s.searchQuery, s.page, s.pageSize) {
        vm.pagedCards(s)
    }
    val totalPages = remember(s.allCards, s.color, s.cardType, s.setFilter, s.rarityFilter, s.searchQuery, s.pageSize) {
        vm.totalPages(s)
    }

     val totalNetWorth by vm.totalNetWorth.collectAsState(initial = 0)
     val isSgd by vm.isSgd.collectAsState(initial = false)

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
        bottomBar = { if (totalNetWorth != null && vm != null) { // safe check
            CatalogFooter(
                totalNetWorth = totalNetWorth.toDouble(),
                isSgd = isSgd,
                onToggleCurrency = { vm.toggleCurrency() }
            )
        }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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

            PagingRow(
                page = s.page,
                totalPages = totalPages,
                onPrev = vm::prevPage,
                onNext = vm::nextPage
            )

            Spacer(Modifier.height(6.dp))

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

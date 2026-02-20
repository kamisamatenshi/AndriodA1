package com.koi.thepiece.ui.screens.deckbuilderscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.catalogscreen.components.PagingRow
import com.koi.thepiece.ui.screens.catalogscreen.components.SearchBarRow
enum class DeckViewMode { GRID, LIST }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckBuilderLeaderDeckScreen(
    vm: DeckViewModel,
    onBack: () -> Unit,
    audio: AudioManager,
    imageLoader: ImageLoader
) {
    val s by vm.state.collectAsState()
    val leader = s.selectedLeader

    LaunchedEffect(Unit) {
        vm.setColorOrType("Red")
        vm.closeModal()
    }

    var viewMode by rememberSaveable { mutableStateOf(DeckViewMode.GRID) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val cards = remember(
        s.allCards,
        s.colorOrType,
        s.setFilter,
        s.rarityFilter,
        s.searchQuery,
        s.page,
        s.pageSize
    ) {
        vm.pagedCards(s)
    }
    val totalPages = remember(
        s.allCards,
        s.colorOrType,
        s.setFilter,
        s.rarityFilter,
        s.searchQuery,
        s.pageSize
    ) {
        vm.totalPages(s)
    }

    if (showFilters) {
        DeckFilterBottomSheet(
            currentSet = s.setFilter,
            currentColorOrType = s.colorOrType,
            currentRarity = s.rarityFilter,
            onSetChange = vm::setSetFilter,
            onColorOrTypeChange = vm::setColorOrType,
            onRarityChange = vm::setRarityFilter,
            onClear = {
                vm.setSetFilter("OP01")
                vm.setColorOrType("all")
                vm.setRarityFilter("all")
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
                        viewMode =
                            if (viewMode == DeckViewMode.GRID) DeckViewMode.LIST else DeckViewMode.GRID
                    }) {
                        Icon(
                            imageVector = if (viewMode == DeckViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
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

            // 🔼 TOP 50% — Card Catalog
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

                when (viewMode) {
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

                Text(
                    text = "(${s.deck.values.sum()}/50)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp)
                )
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
                    1 -> CardsSection(state = s, vm = vm, viewMode = viewMode, imageLoader = imageLoader)
                }
            }
        }

        if (s.selected != null) {
            DeckPreviewDialog(
                card = s.selected!!,
                imageLoader = imageLoader,
                onDismiss = vm::closeModal,
                onAddToDeck = vm::addToDeck
            )
        }
    }
}

@Composable
fun CardsSection(
    state: DeckUiState,
    vm: DeckViewModel,
    viewMode: DeckViewMode,
    imageLoader: ImageLoader
) {
    val deckEntries = state.deck.entries.toList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(deckEntries) { (cardId, qty) ->
            val card = state.allCards.firstOrNull { it.id == cardId }
            if (card != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${card.code}  x$qty", modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.removeFromDeck(card) }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Remove")
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderSection(
    state: DeckUiState,
    vm: DeckViewModel,
    imageLoader: ImageLoader,
    card: Card?
) {
    if (card == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No leader selected")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "Selected Leader",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(card.imageUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .scale(Scale.FIT)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = card.code ?: "Leader",
                modifier = Modifier
                    .width(72.dp)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(card.code ?: "-", style = MaterialTheme.typography.titleSmall)
                Text(card.name, style = MaterialTheme.typography.bodySmall)
                Text("Color: ${card.color}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun DeckPreviewDialog(
    card: Card,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
    onAddToDeck: (Card) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text(card.code ?: "Card") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(card.imageUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .scale(Scale.FIT)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = card.code ?: "Card",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .transformable(state)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )

                Spacer(Modifier.height(10.dp))
                Text("Code: ${card.code ?: "-"}")
                Text("Color: ${card.color}   Type: ${card.type}")
                if (!card.cardSet.isNullOrBlank()) Text("Set: ${card.cardSet}")
                if (!card.rarity.isNullOrBlank()) Text("Rarity: ${card.rarity}")
                if (!card.traits.isNullOrBlank()) Text("Traits: ${card.traits}")
                if (!card.obtainFrom.isNullOrBlank()) Text("Obtain: ${card.obtainFrom}")

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAddToDeck(card) }) {
                        Text("Add to deck")
                    }
                }
            }
        }
    )
}

@Composable
fun DeckTileList(
    card: Card,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(card.imageUrl)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .scale(Scale.FIT)
                .build(),
            imageLoader = imageLoader,
            contentDescription = card.code ?: "Card",
            modifier = Modifier
                .width(56.dp)
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(card.code ?: "-", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun DeckTileGrid(
    card: Card,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(card.imageUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .scale(Scale.FIT)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = card.code ?: "Card",
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = card.code ?: "-",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckFilterBottomSheet(
    currentSet: String,
    currentColorOrType: String,
    currentRarity: String,
    onSetChange: (String) -> Unit,
    onColorOrTypeChange: (String) -> Unit,
    onRarityChange: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        val filters = buildList {
            add("all")
            addAll((1..14).map { "OP" + it.toString().padStart(2, '0') })
            addAll((1..4).map { "EB" + it.toString().padStart(2, '0') })
        }

        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Filters", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // Set filter (NO "all")
            FilterSectionTitle("Set")
            FlowRowChips(
                options = filters,
                selected = currentSet,
                onSelect = onSetChange
            )

            Spacer(Modifier.height(12.dp))

            // Rarity
            FilterSectionTitle("Rarity")
            FlowRowChips(
                options = listOf("All", "A-L", "L"),
                selected = currentRarity,
                onSelect = onRarityChange
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = onClear) { Text("Clear") }
                Button(onClick = onDismiss) { Text("Done") }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { opt ->
            val isSelected = opt.equals(selected, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(opt) },
                label = { Text(opt) }
            )
        }
    }
}


@Composable
fun DeckHeaderBlock(
    setCode: String,
    loading: Boolean,
    error: String?
) {
    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(onClick = {}, enabled = false, label = { Text(setCode) })
            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text("Set: $setCode", style = MaterialTheme.typography.titleMedium)
                when {
                    loading -> Text("Loading…", style = MaterialTheme.typography.bodySmall)
                    error != null -> Text("Error: $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    else -> Text("Ready", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}


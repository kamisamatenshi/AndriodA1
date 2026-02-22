package com.koi.thepiece.ui.screens.deckbuilderscreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.koi.thepiece.AppGraph
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.ui.components.SfxButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    vm: DeckListViewModel,
    deckVm: DeckViewModel,
    onBack: () -> Unit,
    audio: AudioManager,
    onGoCreateNewDeck: () -> Unit,
    onOpenDeck: (deckId: Long) -> Unit,
    imageLoader: ImageLoader
) {
    val decks by vm.decksUi.collectAsState()
    var deckToDelete by remember { mutableStateOf<Long?>(null) }

    var showBtn1 by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(120); showBtn1 = true }
    fun fadeSpec() = fadeIn(animationSpec = tween(durationMillis = 250))
    val snackbarHostState = remember { SnackbarHostState() }
    // ✅ Import dialog state
    var showImportDialog by remember { mutableStateOf(false) }
    var shareCodeInput by remember { mutableStateOf("") }
    var backLocked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        vm.snackbar.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    LaunchedEffect(backLocked) {
        if (backLocked) {
            kotlinx.coroutines.delay(350) // debounce window
            backLocked = false
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Deck List") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (backLocked) return@IconButton
                            backLocked = true
                            audio.playClick()
                            onBack()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(12.dp)
        ) {
            AnimatedVisibility(visible = showBtn1, enter = fadeSpec()) {

                // ✅ Row with 2 buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SfxButton(
                        onClick = {
                            deckVm.startNewDeck()
                            onGoCreateNewDeck()
                        },
                        audio = audio,
                        modifier = Modifier.weight(1f)
                    ) { Text("Create New Deck") }

                    SfxButton(
                        onClick = {
                            audio.playClick()
                            shareCodeInput = ""
                            showImportDialog = true
                        },
                        audio = audio,
                        modifier = Modifier.weight(1f)
                    ) { Text("Import Deck") }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (decks.isEmpty()) {
                Text("No saved decks yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(decks, key = { it.deckId }) { d ->
                        DeckRow(
                            d = d,
                            imageLoader = imageLoader,
                            onClick = { onOpenDeck(d.deckId) },
                            onDelete = { deckToDelete = d.deckId }
                        )
                    }
                }
            }
        }
    }

    // ✅ Delete dialog (unchanged)
    if (deckToDelete != null) {
        AlertDialog(
            onDismissRequest = { deckToDelete = null },
            title = { Text("Delete Deck?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteDeck(deckToDelete!!)
                    deckToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deckToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // ✅ Import dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Deck") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a ShareCode to import a deck into your deck list.")
                    OutlinedTextField(
                        value = shareCodeInput,
                        onValueChange = { shareCodeInput = it },
                        singleLine = true,
                        label = { Text("ShareCode") },
                        placeholder = { Text("e.g. 4gHRCG") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = shareCodeInput.trim().isNotEmpty(),
                    onClick = {
                        audio.playClick()
                        vm.importDeckByShareCode(shareCodeInput.trim())
                        showImportDialog = false
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }
}
@Composable
private fun DeckRow(
    d: DeckListItemUi,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(d.leaderImageUrl)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = d.leaderName ?: "Leader",
                        modifier = Modifier
                            .width(56.dp)
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            d.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Leader: ${d.leaderName ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Cards: ${d.totalCards}/50", style = MaterialTheme.typography.bodySmall)
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Deck")
                    }
                }
            }

            // 🔹 SHARE CODE BADGE (top-right overlay)
            d.sharecode?.let { shareId ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "ShareCode: $shareId",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
package com.koi.thepiece.ui.screens.deckbuilderscreen

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
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.ui.components.SfxButton
import kotlinx.coroutines.delay

/**
 * Deck list screen displaying the user's saved decks.
 *
 * Responsibilities:
 * - Collects deck list UI models from [DeckListViewModel]
 * - Provides actions to create a new deck, import a deck by share code, and delete decks
 * - Displays transient feedback via snackbar (messages emitted by vm.snackbar)
 *
 * Notes:
 * - Deck creation uses [DeckViewModel.startNewDeck] to reset builder state before navigation.
 * - Import and delete actions delegate to [DeckListViewModel].
 * - UI uses Material3 Scaffold with TopAppBar + snackbar host.
 */
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
    /** Reactive deck list derived by ViewModel (auto-updates via Flow combine). */
    val decks by vm.decksUi.collectAsState()

    /** Pending deck id to delete (triggers delete confirmation dialog). */
    var deckToDelete by remember { mutableStateOf<Long?>(null) }

    // -------------------------
    // Entry animation / UI polish
    // -------------------------

    /** Controls initial button fade-in on first composition. */
    var showBtn1 by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(120); showBtn1 = true }

    /** Fade-in spec used by AnimatedVisibility for top buttons. */
    fun fadeSpec() = fadeIn(animationSpec = tween(durationMillis = 250))

    // -------------------------
    // Snackbar (one-shot messages)
    // -------------------------

    /** Snackbar host state for displaying vm.snackbar messages. */
    val snackbarHostState = remember { SnackbarHostState() }

    /**
     * Collect snackbar messages emitted by the ViewModel and display them.
     * This is an event-stream pattern (SharedFlow -> snackbar).
     */
    LaunchedEffect(Unit) {
        vm.snackbar.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // -------------------------
    // Import dialog state
    // -------------------------

    /** Controls visibility of the import dialog. */
    var showImportDialog by remember { mutableStateOf(false) }

    /** Text input for share code entry. */
    var shareCodeInput by remember { mutableStateOf("") }

    // -------------------------
    // Back navigation debounce
    // -------------------------

    /**
     * Prevents double-tap on the back button causing repeated navigation.
     * backLocked is reset after a short debounce window.
     */
    var backLocked by remember { mutableStateOf(false) }
    LaunchedEffect(backLocked) {
        if (backLocked) {
            kotlinx.coroutines.delay(350) // debounce window
            backLocked = false
        }
    }

    /** Initial refresh to ensure deck list is synced from server on screen entry. */
    LaunchedEffect(Unit) { vm.refreshDecksFromServer() }

    // -------------------------
    // Screen layout
    // -------------------------

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Deck List") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // Guard against accidental double taps.
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
            // Top actions: Create / Import (fade-in on entry)
            AnimatedVisibility(visible = showBtn1, enter = fadeSpec()) {

                // Row with 2 primary actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Create new deck:
                    // Reset builder state then navigate to builder screen.
                    SfxButton(
                        onClick = {
                            deckVm.startNewDeck()
                            onGoCreateNewDeck()
                        },
                        audio = audio,
                        modifier = Modifier.weight(1f)
                    ) { Text("Create New Deck") }

                    // Import deck:
                    // Open dialog for share code input.
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

            // Deck list content
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

    // -------------------------
    // Delete confirmation dialog
    // -------------------------

    if (deckToDelete != null) {
        AlertDialog(
            onDismissRequest = { deckToDelete = null },
            title = { Text("Delete Deck?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteDeck(deckToDelete!!)
                        deckToDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deckToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // -------------------------
    // Import dialog
    // -------------------------

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

/**
 * Single row item representing a deck entry.
 *
 * Responsibilities:
 * - Shows leader image + deck name + leader name + total card count
 * - Allows opening the deck (row click)
 * - Allows deletion (trash icon)
 * - Displays optional share code badge as a top-right overlay
 */
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
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Entire row is clickable to open the deck.
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leader card image
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

                    // Deck metadata
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
                        Text(
                            "Cards: ${d.totalCards}/50",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Delete action button
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Deck")
                    }
                }
            }

            // Optional share code badge (top-right overlay)
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
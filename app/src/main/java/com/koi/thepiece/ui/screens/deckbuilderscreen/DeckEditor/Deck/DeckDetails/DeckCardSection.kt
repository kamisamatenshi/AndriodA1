package com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.Deck.DeckDetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.Deck.DeckViewMode
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckUiState
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel
import kotlin.collections.component1
import kotlin.collections.component2


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
        items(deckEntries, key = { it.key }) { (cardId, qty) ->
            val card = state.allCards.firstOrNull { it.id == cardId } ?: return@items

            DeckCardRow(
                card = card,
                imageLoader = imageLoader,
                trailing = {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("x$qty", style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = { vm.removeFromDeck(card) }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Remove")
                        }
                    }
                }
            )
        }
    }
}
package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.deck.deckdetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckUiState
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel

/**
 * UI section that displays the currently selected Leader card for the deck.
 *
 * Responsibilities:
 * - Renders a placeholder message when no leader is selected
 * - Displays the selected leader using a [DeckCardRow]
 * - Delegates interactions back to [DeckViewModel] (open card preview)
 *
 * Notes:
 * - Stock quantity is resolved via [DeckViewModel.getFromStockQty] to show owned amount.
 * - Leader quantity is always 1 (Leader slot is fixed).
 */
@Composable
fun LeaderSection(
    state: DeckUiState,
    vm: DeckViewModel,
    imageLoader: ImageLoader,
    card: Card?
) {

    // No leader selected: show centered empty state.
    if (card == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) { Text("No leader selected") }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Section header
        Text(
            text = "Selected Leader",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Leader row (fixed qty = 1)
        DeckCardRow(
            card = card,
            stockqty = vm.getFromStockQty(card.id),
            1,
            imageLoader = imageLoader,
            onClick = { vm.openCard(card) }
        )
    }
}
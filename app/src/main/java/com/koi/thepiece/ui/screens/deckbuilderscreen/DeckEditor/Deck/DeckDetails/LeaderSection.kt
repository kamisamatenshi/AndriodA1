package com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.Deck.DeckDetails

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckUiState
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel


@Composable
fun LeaderSection(
    state: DeckUiState,
    vm: DeckViewModel,
    imageLoader: ImageLoader,
    card: Card?
) {

    if (card == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) { Text("No leader selected") }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp)
    ) {
        Text(
            text = "Selected Leader",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        DeckCardRow(
            card = card,
            stockqty = vm.getFromStockQty(card.id),
            1,
            imageLoader = imageLoader,
            onClick = { vm.openCard(card) }
        )

    }
}
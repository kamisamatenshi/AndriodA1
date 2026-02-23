package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.deck

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * [Derived from CatalogHeaderBlock] Header block for the Deck Builder screen.
 *
 * This is a simplified version of `CatalogHeaderBlock`:
 * - Shows the currently active set code
 * - Shows loading / error / ready state
 *
 * Deck-specific difference vs catalogue:
 * - No set completion progress (owned/total + progress indicators) is shown here.
 *   The deck flow cares more about "can I build/edit now?" than collection progress.
 *
 * @param setCode Active set code selection (e.g., OP01, EB04, ALL).
 * @param loading Whether deck/catalouge data is currently loading/refreshing.
 * @param error Optional error message (e.g., refresh/network issues).
 */
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
            // [Shared with CatalogHeaderBlock] Displays the active set code in a compact chip.
            AssistChip(onClick = {}, enabled = false, label = { Text(setCode) })
            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                // [Shared with CatalogHeaderBlock] Title line showing selected set.
                Text("Set: $setCode", style = MaterialTheme.typography.titleMedium)

                // [Derived from CatalogHeaderBlock] Status line.
                // Catalogue shows loading/error/owned progress; deck shows loading/error/ready.
                when {
                    loading -> {
                        Text("Loading…", style = MaterialTheme.typography.bodySmall)
                    }

                    error != null -> {
                        Text(
                            "Error: $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    else -> {
                        Text("Ready", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
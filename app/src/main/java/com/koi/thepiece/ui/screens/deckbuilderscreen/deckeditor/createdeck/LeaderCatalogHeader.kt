package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck

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
 * Header block for the Leader selection screen.
 *
 * This component intentionally follows the structural layout of CatalogHeaderBlock
 * to preserve visual and interaction consistency across browsing screens.
 *
 * UI Alignment with CatalogHeaderBlock:
 * - AssistChip displaying active set code
 * - Status line showing loading/error/ready state
 * - Surface container with tonal elevation
 *
 * Differences vs CatalogHeaderBlock:
 * - Does NOT display collection completion percentage
 * - Does NOT observe SetCompletion flow
 * - Only shows simple state (Loading / Error / Ready)
 *
 * Purpose:
 * - Provide contextual information about the currently selected set
 * - Indicate loading/error state during leader data refresh
 *
 * @param setCode Active set code (e.g., OP01, EB04, all).
 * @param loading Whether leader data is currently being loaded/refreshed.
 * @param error Optional error message when refresh fails.
 */
@Composable
fun LeaderHeaderBlock(
    setCode: String,
    loading: Boolean,
    error: String?
) {
    /**
     * Surface provides subtle elevation separation from scrolling content,
     * consistent with catalogue header styling.
     */
    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Compact chip displaying the currently active set code.
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(setCode) }
            )

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {

                // Title line indicating active set context.
                Text(
                    text = "Set: $setCode",
                    style = MaterialTheme.typography.titleMedium
                )

                /**
                 * Status line:
                 * - Loading → indicates data fetch in progress
                 * - Error → shows failure message in error color
                 * - Else → simple ready state
                 */
                when {
                    loading -> Text("Loading…", style = MaterialTheme.typography.bodySmall)
                    error != null -> Text("Error: $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    else -> Text("Ready", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
package com.koi.thepiece.ui.screens.catalogscreen.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.koi.thepiece.ui.screens.catalogscreen.CatalogViewModel
import com.koi.thepiece.data.repo.SetCompletion
import kotlin.math.roundToInt

/**
 * Header block for the catalogue screen.
 *
 * Displays:
 * - Selected set code
 * - Loading or error state message (when refreshing catalogue)
 * - Collection completion based on owned quantity (ownedQty > 0 counts as collected)
 * - Linear progress bar and circular percentage indicator
 *
 * Data flow:
 * - Uses viewModel.observeSetCompletion(setCode) to compute completion reactively.
 * - Completion updates automatically whenever the local Room data changes.
 *
 * @param setCode Active set code selection (e.g., OP01, EB04, ALL).
 * @param loading Whether catalogue data is currently being refreshed.
 * @param error Optional error message to present when network refresh fails.
 * @param viewModel ViewModel providing completion Flow derived from repository data.
 */
@Composable
fun CatalogHeaderBlock(
    setCode: String,
    loading: Boolean,
    error: String?,
    viewModel: CatalogViewModel
) {
    /**
     * Completion flow is keyed by setCode so switching sets re-subscribes
     * to the correct completion stream.
     */
    val completionFlow = remember(setCode) { viewModel.observeSetCompletion(setCode) }
    val completion by completionFlow.collectAsState(initial = SetCompletion(0, 0, 0f))

    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Displays the active set code in a compact chip.
            AssistChip(onClick = {}, enabled = false, label = { Text(setCode) })
            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text("Set: $setCode", style = MaterialTheme.typography.titleMedium)
                // Status line displays loading, error, or completion summary.
                when {
                    loading -> Text("Loading…", style = MaterialTheme.typography.bodySmall)
                    error != null -> Text("Error: $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    else -> Text(
                        "${completion.owned}/${completion.total} collected",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Linear progress bar showing completion ratio.
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { completion.percent.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
            // Circular progress with numeric percentage.
            ProgressCirclePercent(progress = completion.percent)
        }
    }
}

/**
 * Search bar component with optional suggestion dropdown list.
 *
 * Behavior:
 * - Uses an OutlinedTextField for query input.
 * - Displays a dropdown suggestion list when suggestions are not empty.
 * - Selecting a suggestion calls onSuggestionClick(suggestion), allowing callers to:
 *   - populate query
 *   - trigger search
 *   - hide suggestions
 *
 * @param query Current search query string.
 * @param suggestions List of suggested results to show under the search bar.
 * @param onQueryChange Callback invoked when the user types.
 * @param onSuggestionClick Callback invoked when a suggestion is selected.
 */
@Composable
fun SearchBarRow(
    query: String,                       // Current text inside search field
    suggestions: List<String>,           // Suggested results
    onQueryChange: (String) -> Unit,     // Triggered when typing
    onSuggestionClick: (String) -> Unit  // Triggered when suggestion clicked
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {

        // The main search text field
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,    // Updates ViewModel
            singleLine = true,
            placeholder = { Text("Search (code / name / traits)") },
            modifier = Modifier
                .fillMaxWidth()
        )

        // Only show dropdown if suggestions exist
        if (suggestions.isNotEmpty()) {

            // Surface creates dropdown card effect
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Scrollable suggestion list
                LazyColumn (
                    modifier = Modifier.heightIn(max = 220.dp)
                ) {
                    items(suggestions) { suggestion ->
                        // Each suggestion row
                        Text(
                            text = suggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .clickable {
                                    // When clicked:
                                    // 1) Put suggestion into search bar
                                    // 2) Hide dropdown
                                    onSuggestionClick(suggestion)
                                }
                        )
                    }
                }
            }
        }
    }
}


/**
 * Circular progress indicator showing completion percentage text.
 *
 * @param progress Completion progress ratio from 0.0 to 1.0.
 * @param modifier Optional modifier for sizing/positioning.
 */
@Composable
fun ProgressCirclePercent(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val p = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { p },
            strokeWidth = 4.dp,
            modifier = Modifier.matchParentSize()
        )
        Text(
            text = "${(p * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Pagination control row for catalogue navigation.
 *
 * Displays:
 * - Current page index and total pages
 * - Prev/Next buttons with proper enable/disable states
 *
 * @param page Current page number (1-indexed).
 * @param totalPages Total number of pages available.
 * @param onPrev Callback for previous page action.
 * @param onNext Callback for next page action.
 */
@Composable
fun PagingRow(
    page: Int,
    totalPages: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Page $page / $totalPages", style = MaterialTheme.typography.bodySmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPrev,
                enabled = page > 1,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("Prev") }

            OutlinedButton(
                onClick = onNext,
                enabled = page < totalPages,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("Next") }
        }
    }
}

/**
 * Footer block for the catalogue screen.
 *
 * Displays:
 * - Currency toggle button (JPY / SGD)
 * - Total net worth based on owned quantities and retrieved price values
 *
 * Currency conversion:
 * - SGD conversion uses a fixed ratio (JPY/SGD ~ 120) for display purposes.
 *
 * @param totalNetWorth Total portfolio value in JPY (source-of-truth value).
 * @param isSgd If true, display total value in SGD; otherwise display JPY.
 * @param onToggleCurrency Callback to switch currency display mode.
 * @param modifier Optional modifier for placement.
 */
@Composable
fun CatalogFooter(
    totalNetWorth: Double,
    isSgd: Boolean,
    onToggleCurrency: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // toggle button
            CreateOutlinedButton(
                text = if (isSgd) "SGD" else "JPY",
                onClick = onToggleCurrency
            )

            // Total net worth
            val displayText = if (isSgd) {
                val sgd = totalNetWorth.toDouble() / 120.0
                "S$${"%.2f".format(sgd)}"
            } else {
                "¥${"%,d".format(totalNetWorth.toLong())}"
            }

            Text(
                text = "Total NetWorth: $displayText",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Small outlined button used for simple toggles in catalogue UI.
 *
 * Used in CatalogFooter as a currency toggle control.
 *
 * @param text Display label.
 * @param onClick Click callback.
 */
@Composable
private fun CreateOutlinedButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 60.dp, height = 35.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
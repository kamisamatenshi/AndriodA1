package com.koi.thepiece.ui.screens.catalogscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

@Composable
fun CatalogHeaderBlock(
    setCode: String,
    loading: Boolean,
    error: String?,
    viewModel: CatalogViewModel
) {
    val completionFlow = remember(setCode) { viewModel.observeSetCompletion(setCode) }
    val completion by completionFlow.collectAsState(initial = SetCompletion(0, 0, 0f))

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
                    else -> Text(
                        "${completion.owned}/${completion.total} collected",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                androidx.compose.material3.LinearProgressIndicator(
                    progress = { completion.percent.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
            /*
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text("${(completion.percent * 100).toInt()}%") }
            )
            */
            ProgressCirclePercent(progress = completion.percent)
        }
    }
}
@Composable
fun SearchBarRow(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text("Search (code / name / traits)") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

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

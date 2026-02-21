package com.koi.thepiece.ui.screens.catalogscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun CatalogHeaderBlock(
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

            AssistChip(onClick = {}, enabled = false, label = { Text("0%") })
        }
    }
}

@Composable
//fun SearchBarRow(
//    query: String,
//    onQueryChange: (String) -> Unit
//) {
//    OutlinedTextField(
//        value = query,
//        onValueChange = onQueryChange,
//        singleLine = true,
//        placeholder = { Text("Search (code / name / traits)") },
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 12.dp, vertical = 8.dp)
//    )
//}
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

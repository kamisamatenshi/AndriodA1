package com.koi.thepiece.ui.screens.catalogscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
 fun FilterBottomSheet(
    currentSet: String,
    currentColorOrType: String,
    currentRarity: String,
    onSetChange: (String) -> Unit,
    onColorOrTypeChange: (String) -> Unit,
    onRarityChange: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Filters", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // Set filter (NO "all")
            FilterSectionTitle("Set")
            FlowRowChips(
                options = (1..14).map { "OP" + it.toString().padStart(2, '0') },
                selected = currentSet,
                onSelect = onSetChange
            )

            Spacer(Modifier.height(12.dp))

            // Color/Type
            FilterSectionTitle("Color / Type")
            FlowRowChips(
                options = listOf("all", "Red", "Green", "Blue", "Purple", "Black", "Yellow", "Leader", "Character", "Event", "Stage"),
                selected = currentColorOrType,
                onSelect = onColorOrTypeChange
            )

            Spacer(Modifier.height(12.dp))

            // Rarity
            FilterSectionTitle("Rarity")
            FlowRowChips(
                options = listOf("All", "A-SEC", "SEC", "A-SR", "SR", "A-R", "R", "A-L", "L","SP","UC","C"),
                selected = currentRarity,
                onSelect = onRarityChange
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = onClear) { Text("Clear") }
                Button(onClick = onDismiss) { Text("Done") }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { opt ->
            val isSelected = opt.equals(selected, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(opt) },
                label = { Text(opt) }
            )
        }
    }
}



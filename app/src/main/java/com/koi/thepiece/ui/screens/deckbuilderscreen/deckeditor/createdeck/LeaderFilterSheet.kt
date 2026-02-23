package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object ChipStyle {
    var elevationUnselected = 2.dp
    var elevationSelected = 10.dp

    var textSelected = Color(0xFF1E1F22) // dark text on gold
    var textUnselected = Color(0xFF1E1F22)

    var selectedOverrideColor: Color? = null
    var unselectedOverrideColor: Color? = null

    var borderUnselected = BorderStroke(1.dp, Color(0x33000000))

    // One Piece gold glow
    var gold = Color(0xFFD6B15E)
}

val colorOptions = buildList {
    add("all")

    // Colors
    addAll(listOf("Red", "Green", "Blue", "Purple", "Black", "Yellow" ,"Mix"))
}

val typeOption = buildList {
    // Card Types
    addAll(listOf("Leader"))
}
val rarityOption = buildList {
    addAll(listOf("all", "A-L", "L"))
}

@Composable
private fun unselectedContainerColor(): Color {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val bg = MaterialTheme.colorScheme.surface
    // 0.0 = base, 1.0 = bg (more "washed"/lighter)
    return lerp(base, bg, 0.55f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderFilterBottomSheet(
    currentSet: String,
    currentColor: String,
    currentRarity: String,
    currentType : String,
    onSetChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onCardTypeChange:(String) -> Unit,
    onRarityChange: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
)  {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val setGroups = mapOf(
        "all" to listOf("all"),
        "OP" to (1..14).map { "OP" + it.toString().padStart(2, '0') },
        "EB" to (1..4).map { "EB" + it.toString().padStart(2, '0') },
        "PRB" to (1..2).map { "PRB" + it.toString().padStart(2, '0') },
        "ST" to (1..29).map { "ST" + it.toString().padStart(2, '0') }
    )

    val currentGroup = when {
        currentSet.equals("all", true) -> "all"
        currentSet.startsWith("OP", true) -> "OP"
        currentSet.startsWith("EB", true) -> "EB"
        currentSet.startsWith("PRB", true) -> "PRB"
        currentSet.startsWith("ST", true) -> "ST"
        else -> "all"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Filters", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // Set filter
            FilterSectionTitle("Set")

            // Category row
            FlowRowChips(
                options = listOf("all", "OP", "EB", "PRB", "ST"),
                selected = currentGroup,
                onSelect = { group ->
                    if (group == "all") onSetChange("all")
                    else onSetChange(setGroups[group]!!.first())
                }
            )

            // Horizontal scrolling chips for that group
            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(setGroups[currentGroup] ?: emptyList()) { opt ->
                    val isSelected = opt.equals(currentSet, ignoreCase = true)

                    OPFilterChip(
                        text = opt,
                        selected = isSelected,
                        onClick = { onSetChange(opt) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Color
            FilterSectionTitle("Color")
            FlowRowChips(
                options = colorOptions,
                selected = currentColor,
                onSelect = onColorChange
            )

            Spacer(Modifier.height(12.dp))

            // Type
            FilterSectionTitle("Type")
            FlowRowChips(
                options = typeOption,
                selected = currentType,
                onSelect = onCardTypeChange
            )

            Spacer(Modifier.height(12.dp))

            // Rarity
            FilterSectionTitle("Rarity")
            FlowRowChips(
                options = rarityOption,
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
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { opt ->
            val isSelected = opt.equals(selected, ignoreCase = true)

            OPFilterChip(
                text = opt,
                selected = isSelected,
                onClick = { onSelect(opt) }
            )
        }
    }
}

@Composable
private fun OPFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val targetElevation = if (selected) ChipStyle.elevationSelected else ChipStyle.elevationUnselected
    val elevation by animateDpAsState(targetValue = targetElevation, label = "chipElevation")

    val targetScale = if (pressed) 0.96f else 1f
    val scale by animateFloatAsState(targetValue = targetScale, label = "chipScale")

    val containerSelected =
        ChipStyle.selectedOverrideColor ?: ChipStyle.gold.copy(alpha = 0.92f)
    val containerUnselected = ChipStyle.unselectedOverrideColor ?: unselectedContainerColor()

    val infinite = rememberInfiniteTransition(label = "sparkle")

    val bandWidthFrac = 0.28f

    val sparkleT by infinite.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkleT"
    )

    val sparkleModifier =
        if (selected) Modifier.drawWithContent {
            drawContent()

            val w = size.width
            val h = size.height
            val bandW = w * bandWidthFrac

            val startX = -bandW
            val endX = w + bandW
            val cx = startX + (endX - startX) * sparkleT

            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                start = Offset(cx - bandW, 0f),
                end = Offset(cx + bandW, h)
            )

            drawRect(brush = brush)
        } else Modifier

    val glowModifier =
        if (selected) Modifier.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = ChipStyle.gold.copy(alpha = 0.55f),
            spotColor = ChipStyle.gold.copy(alpha = 0.55f)
        ) else Modifier

    ElevatedFilterChip(
        selected = selected,
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        modifier = modifier
            .then(glowModifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.shape = shape
                clip = true
            }
            .then(sparkleModifier),
        label = {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = FilterChipDefaults.elevatedFilterChipColors(
            containerColor = containerUnselected,
            selectedContainerColor = containerSelected,
            labelColor = ChipStyle.textUnselected,
            selectedLabelColor = ChipStyle.textSelected
        ),
        elevation = FilterChipDefaults.elevatedFilterChipElevation(
            elevation = elevation,
            pressedElevation = elevation,
            focusedElevation = elevation,
            hoveredElevation = elevation,
            draggedElevation = elevation,
            disabledElevation = 0.dp
        ),
        border = if (selected) null else ChipStyle.borderUnselected
    )
}

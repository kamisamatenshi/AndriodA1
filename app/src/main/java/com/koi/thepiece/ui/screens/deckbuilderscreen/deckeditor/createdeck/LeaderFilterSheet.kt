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

/**
 * Centralized styling configuration for Leader filter chips.
 *
 * This file mirrors the catalogue FilterBottomSheet chip aesthetics:
 * - Selected chips use a One Piece-inspired gold container + glow + sparkle shimmer
 * - Unselected chips use softened surface styling with subtle border
 *
 * Note:
 * These are mutable vars for optional tuning, but usually treated as constants.
 */
object ChipStyle {
    /** Base elevation for unselected chips. */
    var elevationUnselected = 2.dp

    /** Elevated look for selected chips (also used for glow intensity). */
    var elevationSelected = 10.dp

    /** Text color when selected (dark text on gold). */
    var textSelected = Color(0xFF1E1F22)

    /** Text color when unselected. */
    var textUnselected = Color(0xFF1E1F22)

    /** Optional override for selected chip container color (defaults to gold). */
    var selectedOverrideColor: Color? = null

    /** Optional override for unselected chip container color (defaults to soft surface). */
    var unselectedOverrideColor: Color? = null

    /** Border used for unselected chips to retain separation on light backgrounds. */
    var borderUnselected = BorderStroke(1.dp, Color(0x33000000))

    /** One Piece-inspired gold tone. */
    var gold = Color(0xFFD6B15E)
}

/**
 * Filter options for colour selection.
 * "all" indicates no filtering on this category.
 */
val colorOptions = buildList {
    add("all")
    addAll(listOf("Red", "Green", "Blue", "Purple", "Black", "Yellow", "Mix"))
}

/**
 * Filter options for card type selection (Leader creation flow).
 *
 * Note:
 * In leader selection flow, you currently only allow "Leader".
 * If you later expand this sheet to reuse the same filter UI for other picks,
 * add "all" and other types here (like the catalogue sheet).
 */
val typeOption = buildList {
    addAll(listOf("Leader"))
}

/**
 * Filter options for rarity selection in leader picking flow.
 */
val rarityOption = buildList {
    addAll(listOf("all", "A-L", "L"))
}

/**
 * Produces a softened unselected chip background color.
 *
 * The color is derived by interpolating between surfaceVariant and surface,
 * reducing contrast and making chips look less "heavy" in the bottom sheet.
 */
@Composable
private fun unselectedContainerColor(): Color {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val bg = MaterialTheme.colorScheme.surface
    // 0.0 = base, 1.0 = bg (more "washed"/lighter)
    return lerp(base, bg, 0.55f)
}

/**
 * Bottom sheet UI for filtering Leader selection.
 *
 * Provides:
 * - Set selection (grouped by OP/EB/PRB/ST with quick group tabs + horizontal set chips)
 * - Color filtering
 * - Type filtering (Leader-only in this flow)
 * - Rarity filtering (Leader-focused subset)
 *
 * Interaction design:
 * - Selecting a group tab (OP/EB/PRB/ST) updates the displayed list of set codes.
 * - Selecting a set code calls onSetChange(setCode).
 * - Clear resets all filters via onClear().
 * - Done closes the bottom sheet via onDismiss().
 *
 * Differences vs catalogue FilterBottomSheet:
 * - Type options are constrained to Leader for this flow
 * - Rarity options are constrained to leader-related rarities (A-L, L)
 *
 * @param currentSet Currently selected set code (e.g., OP01, EB04) or "all".
 * @param currentColor Currently selected color option or "all".
 * @param currentRarity Currently selected rarity option or "all".
 * @param currentType Currently selected type option (Leader only).
 * @param onSetChange Callback for set selection updates.
 * @param onColorChange Callback for color filter updates.
 * @param onCardTypeChange Callback for type filter updates.
 * @param onRarityChange Callback for rarity filter updates.
 * @param onClear Callback to reset filter state.
 * @param onDismiss Callback to close the bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderFilterBottomSheet(
    currentSet: String,
    currentColor: String,
    currentRarity: String,
    currentType: String,
    onSetChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onCardTypeChange: (String) -> Unit,
    onRarityChange: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    /**
     * Set codes are grouped to avoid presenting a very long unstructured list.
     * The group tabs control which list is shown in the horizontal LazyRow.
     */
    val setGroups = mapOf(
        "all" to listOf("all"),
        "OP" to (1..14).map { "OP" + it.toString().padStart(2, '0') },
        "EB" to (1..4).map { "EB" + it.toString().padStart(2, '0') },
        "PRB" to (1..2).map { "PRB" + it.toString().padStart(2, '0') },
        "ST" to (1..29).map { "ST" + it.toString().padStart(2, '0') }
    )

    /**
     * Determines which group tab should be highlighted based on currentSet.
     */
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

            // Group selector (all / OP / EB / PRB / ST)
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

            // Bottom actions: clear filters and close
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

/**
 * Standard title block used for each filter category.
 */
@Composable
private fun FilterSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
}

/**
 * FlowRow wrapper that renders a collection of selectable filter chips.
 *
 * Used for:
 * - Group tabs (all/OP/EB/PRB/ST)
 * - Colour filter options
 * - Type filter options (Leader-only here)
 * - Rarity filter options
 *
 * @param options List of chip labels.
 * @param selected Currently selected label.
 * @param onSelect Callback invoked when a chip is selected.
 */
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

/**
 * Customized filter chip styled to match One Piece themed UI.
 *
 * Selected-state enhancements:
 * - Gold container colour
 * - Glow shadow (gold-tinted)
 * - Sparkle shimmer band animation across the chip surface
 * - Higher elevation and subtle press scaling feedback
 *
 * Unselected-state:
 * - Softer neutral container with border
 *
 * @param text Chip label.
 * @param selected Whether this chip is currently selected.
 * @param onClick Click callback for selection.
 * @param modifier Optional modifier.
 * @param shape Chip shape (rounded by default).
 */
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

    // Animate elevation between selected/unselected states
    val targetElevation = if (selected) ChipStyle.elevationSelected else ChipStyle.elevationUnselected
    val elevation by animateDpAsState(targetValue = targetElevation, label = "chipElevation")

    // Press feedback: slight scale down on press
    val targetScale = if (pressed) 0.96f else 1f
    val scale by animateFloatAsState(targetValue = targetScale, label = "chipScale")

    val containerSelected = ChipStyle.selectedOverrideColor ?: ChipStyle.gold.copy(alpha = 0.92f)
    val containerUnselected = ChipStyle.unselectedOverrideColor ?: unselectedContainerColor()

    // Sparkle animation for selected chips
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

    /**
     * Sparkle overlay is only applied when chip is selected.
     * A translucent diagonal highlight band is drawn across the chip surface.
     */
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

    /**
     * Gold glow shadow applied only when selected.
     * Uses the same elevation state for consistent visual intensity.
     */
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
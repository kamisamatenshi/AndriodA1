package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.deck

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
 * [Derived from Catalog FilterBottomSheet] Centralized styling configuration for deck filter chips.
 *
 * Same intent as the catalogue version: a One Piece-inspired "gold glow" for selected chips.
 * This object is kept in the deck module so deck screens can tune chip look independently
 * (even though the behaviour is equivalent to the catalogue implementation).
 *
 * Visual rules:
 * - Selected: gold container + glow shadow + sparkle shimmer band
 * - Unselected: softened surface background + border
 *
 * Note:
 * - Values are mutable for future tuning, but typically treated like constants.
 */
object ChipStyle {
    /** Base elevation for unselected chips. */
    var elevationUnselected = 2.dp

    /** Elevated look for selected chips (also used for glow). */
    var elevationSelected = 10.dp

    /** Text color when selected (dark text on gold). */
    var textSelected = Color(0xFF1A1A1A) // dark text on gold

    /** Text color when unselected. */
    var textUnselected = Color(0xFF111111)

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
 * Filter options for color selection in deck builder.
 *
 * Deck-specific difference vs catalogue:
 * - Catalogue includes "Mix" and uses it for leader color logic.
 * - Deck builder here restricts to main colors only.
 *
 * "all" indicates no filtering on this category.
 */
val colorOptions = buildList {
    add("all")
    addAll(listOf("Red", "Green", "Blue", "Purple", "Black", "Yellow"))
}

/**
 * Filter options for card type selection in deck builder.
 *
 * Deck-specific difference vs catalogue:
 * - Catalogue includes Leader and Don.
 * - Deck builder excludes Leader/Don here because leader selection is handled via editor mode,
 *   and Don cards are typically excluded from main deck selection UI.
 *
 * "all" indicates no filtering on this category.
 */
val typeOption = buildList {
    add("all")
    addAll(listOf("Normal", "Event", "Stage"))
}

/**
 * Filter options for rarity selection.
 *
 * Deck-specific difference vs catalogue:
 * - Catalogue includes A-L and L (Leader rarity) because catalogue can show leaders.
 * - Deck builder excludes those rarities to match the main deck card selection scope.
 */
val rarityOption = buildList {
    addAll(listOf("all", "A-SEC", "SEC", "A-SR", "SR", "A-R", "R", "SP", "UC", "C"))
}

/**
 * [Shared with Catalog FilterBottomSheet] Produces a softened unselected chip background color.
 *
 * The color is derived by interpolating between surfaceVariant and surface,
 * reducing contrast and making chips look less "heavy" in the bottom sheet.
 *
 * Identical to catalogue implementation.
 */
@Composable
private fun unselectedContainerColor(): Color {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val bg = MaterialTheme.colorScheme.surface
    // 0.0 = base, 1.0 = bg (more "washed"/lighter)
    return lerp(base, bg, 0.55f)
}

/**
 * [Derived from Catalog FilterBottomSheet] Bottom sheet UI for deck filtering.
 *
 * Provides:
 * - Set selection (grouped by OP/EB/PRB/ST with quick group tabs + horizontal set chips)
 * - Color filtering
 * - Card type filtering
 * - Rarity filtering
 *
 * Interaction design:
 * - Selecting a group (OP/EB/PRB/ST) updates the displayed list of set codes.
 * - Selecting a set code calls onSetChange(setCode).
 * - Clear resets all filters via onClear().
 * - Done closes the bottom sheet via onDismiss().
 *
 * Deck-specific notes:
 * - The UI structure is the same as catalogue, but option lists differ (see above).
 *
 * @param currentSet Currently selected set code (e.g., OP01, EB04) or "all".
 * @param currentColor Currently selected color option or "all".
 * @param currentRarity Currently selected rarity option or "all".
 * @param currentType Currently selected card type option or "all".
 * @param onSetChange Callback for set selection updates.
 * @param onColorChange Callback for color filter updates.
 * @param onCardTypeChange Callback for type filter updates.
 * @param onRarityChange Callback for rarity filter updates.
 * @param onClear Callback to reset filter state.
 * @param onDismiss Callback to close the bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckFilterBottomSheet(
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
     * [Shared with Catalog FilterBottomSheet] Set codes grouped to avoid a long unstructured list.
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
     * [Shared with Catalog FilterBottomSheet] Determines which group tab is active from currentSet.
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

            // -------------------------
            // Set filter
            // -------------------------
            FilterSectionTitle("Set")

            // [Shared with Catalog FilterBottomSheet] Group selector (all / OP / EB / PRB / ST)
            FlowRowChips(
                options = listOf("all", "OP", "EB", "PRB", "ST"),
                selected = currentGroup,
                onSelect = { group ->
                    // Selecting a group picks the first code in that group (same as catalogue behaviour).
                    if (group == "all") onSetChange("all")
                    else onSetChange(setGroups[group]!!.first())
                }
            )

            // [Shared with Catalog FilterBottomSheet] Horizontal chips for set codes in the active group
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

            // -------------------------
            // Color filter
            // -------------------------
            FilterSectionTitle("Color")
            FlowRowChips(
                options = colorOptions,
                selected = currentColor,
                onSelect = onColorChange
            )

            Spacer(Modifier.height(12.dp))

            // -------------------------
            // Type filter
            // -------------------------
            FilterSectionTitle("Type")
            FlowRowChips(
                options = typeOption,
                selected = currentType,
                onSelect = onCardTypeChange
            )

            Spacer(Modifier.height(12.dp))

            // -------------------------
            // Rarity filter
            // -------------------------
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
 * [Shared with Catalog FilterBottomSheet] Standard title block used for each filter category.
 * Identical to catalogue implementation.
 */
@Composable
private fun FilterSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
}

/**
 * [Shared with Catalog FilterBottomSheet] FlowRow wrapper that renders a collection of selectable filter chips.
 *
 * Used for:
 * - Group tabs (all/OP/EB/PRB/ST)
 * - Color options
 * - Type options
 * - Rarity options
 *
 * Identical structure to catalogue implementation.
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
 * [Shared with Catalog FilterBottomSheet] Customized filter chip styled to match One Piece themed UI.
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
 * Deck-specific note:
 * - Your current implementation references a different ChipStyle package
 *   (createdeck.ChipStyle). This comment assumes it is intentional, but the behaviour
 *   matches the catalogue OPFilterChip.
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

    // Animate elevation between selected/unselected states (same as catalogue).
    val targetElevation =
        if (selected)
            com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck.ChipStyle.elevationSelected
        else
            com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck.ChipStyle.elevationUnselected
    val elevation by animateDpAsState(targetValue = targetElevation, label = "chipElevation")

    // Press feedback: slight scale down on press (same as catalogue).
    val targetScale = if (pressed) 0.96f else 1f
    val scale by animateFloatAsState(targetValue = targetScale, label = "chipScale")

    val containerSelected =
        com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck.ChipStyle.selectedOverrideColor
            ?: com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck.ChipStyle.gold.copy(alpha = 0.92f)
    val containerUnselected =
        com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck.ChipStyle.unselectedOverrideColor
            ?: unselectedContainerColor()

    // Sparkle animation for selected chips (same as catalogue).
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

    // Sparkle overlay applied only when selected.
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

    // Gold glow shadow applied only when selected.
    val glowModifier =
        if (selected) Modifier.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck.ChipStyle.gold.copy(alpha = 0.55f),
            spotColor = com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck.ChipStyle.gold.copy(alpha = 0.55f)
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
            labelColor = com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck.ChipStyle.textUnselected,
            selectedLabelColor = com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck.ChipStyle.textSelected
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

package com.koi.thepiece.ui.screens.catalogscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.koi.thepiece.data.model.Card

/**
 * List-row UI for displaying a single card in catalogue list view.
 *
 * Features:
 * - Thumbnail image rendered via Coil with disk + memory caching enabled
 * - Owned quantity badge (QtyBadge) displayed on the thumbnail corner
 * - Card name displayed as primary text
 * - Compact +/- buttons for fast owned quantity adjustments
 * - Entire row is clickable (typically opens the card detail / preview dialog)
 *
 * This component is designed for efficient scanning of many items while
 * retaining quick inventory update controls.
 *
 * @param card Domain card model used for display.
 * @param imageLoader Shared Coil ImageLoader for consistent caching behavior.
 * @param onClick Called when the row is tapped (typically opens detail dialog).
 * @param onPlus Called when the + button is pressed.
 * @param onMinus Called when the − button is pressed.
 */
@Composable
fun CardTileList(
    card: Card,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onPlus: () -> Unit,
    onMinus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        /**
         * Thumbnail container.
         * aspectRatio keeps visual consistency across list rows.
         */
        Box(
            modifier = Modifier
                .width(56.dp)
                .aspectRatio(0.72f)
        ) {
            // Card thumbnail image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(card.imageUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .scale(Scale.FIT)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = card.code ?: "Card",
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(8.dp))
            )
            // Owned quantity badge overlay
            QtyBadge(
                qty = card.ownedQty,
                modifier = Modifier
                    .align(Alignment.TopEnd)

            )
        }

        Spacer(Modifier.width(10.dp))
        /**
         * Card text section.
         * weight(1f) ensures the title expands and the buttons stay aligned right.
         */
        Column(Modifier.weight(1f)) {
            Text(card.name?: "-", style = MaterialTheme.typography.titleSmall)
        }
        /**
         * Compact quantity adjustment controls.
         */
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallCircleButton(text = "−", onClick = onMinus)
            SmallCircleButton(text = "+", onClick = onPlus)
        }
    }
}

/**
 * Small circular button used for quantity adjustments in list rows.
 *
 * Compared to OverlayCircleButton (grid), this version is:
 * - Smaller footprint for list density
 * - Lower tonal elevation for subtle appearance
 *
 * @param text Button label (typically "+" or "−").
 * @param onClick Click callback.
 */
@Composable
private fun SmallCircleButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        tonalElevation = 1.dp,
        modifier = Modifier.size(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

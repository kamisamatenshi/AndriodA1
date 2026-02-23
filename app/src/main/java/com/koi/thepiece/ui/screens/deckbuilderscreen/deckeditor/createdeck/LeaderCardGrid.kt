package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.koi.thepiece.data.model.Card

/**
 * Grid tile UI for displaying a single Leader card in leader selection grid view.
 *
 * This component intentionally follows the structural layout of CardTileGrid
 * from the catalogue module to maintain UI consistency across browsing screens.
 *
 * UI Alignment with CardTileGrid:
 * - Same tile container styling (RoundedCornerShape + outline border + surface background)
 * - Same artwork thumbnail sizing and aspect ratio (0.72f) for consistent grid density
 * - Clickable tile behavior (opens preview dialog)
 * - Same bottom label styling for code display (labelSmall + ellipsis)
 *
 * Differences vs CardTileGrid:
 * - No owned quantity badge (QtyBadge)
 * - No overlay +/- buttons (inventory editing)
 * - No price observation/fetch side-effects (CatalogViewModel price cache)
 * - Designed purely for selection flow (leader picking)
 *
 * Purpose:
 * - Provide a lightweight, catalogue-consistent leader tile for deck creation entry
 * - Preserve user muscle memory by reusing familiar browsing UI patterns
 * - Avoid inventory/price logic during leader pick mode
 *
 * @param card Domain card model used for UI rendering.
 * @param imageLoader Shared Coil ImageLoader for consistent caching behavior.
 * @param onClick Called when the tile is clicked (opens leader preview dialog).
 */
@Composable
fun LeaderTileGrid(
    card: Card,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    /**
     * Tile layout:
     * - Image area (fixed aspect ratio for consistent grid alignment)
     * - Card code label at bottom
     */
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {

        /**
         * Artwork container.
         * aspectRatio matches catalogue tiles so the leader grid lines up visually
         * with catalogue grid density and scrolling behavior.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
        ) {
            // Leader artwork thumbnail rendered via Coil (disk + memory cache enabled)
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
                modifier = Modifier.fillMaxSize()
            )
        }

        /**
         * Card code label.
         * Kept consistent with CardTileGrid label style to maintain uniform tile height.
         */
        Text(
            text = card.code ?: "-",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
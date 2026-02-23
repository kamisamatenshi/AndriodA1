package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.deck

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
 * Grid tile UI for displaying a single card in Deck Builder catalogue grid view.
 *
 * This implementation follows the same structural pattern as:
 * - CardTileGrid (catalogscreen.components) in CardGrid.kt
 *
 * Structural Similarities:
 * - Rounded tile container
 * - Image area with fixed aspect ratio (0.72f)
 * - Coil AsyncImage with memory + disk caching
 * - Card code label at bottom
 *
 * Key Differences:
 * - No owned quantity badge
 * - No +/- overlay buttons
 * - No price observation or fetching logic
 *
 * @param card Domain card model used for UI rendering.
 * @param imageLoader Shared Coil ImageLoader for consistent caching behavior.
 * @param onClick Called when the tile is clicked (typically opens preview dialog).
 */
@Composable
fun DeckTileGrid(
    card: Card,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    /**
     * Tile layout:
     * - Image area
     * - Card code label at bottom
     *
     * (Identical layout structure to CardTileGrid, minus badge and controls.)
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
         * Image container (maintains consistent aspect ratio in grid)
         * Same layout logic as CardTileGrid.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
        ) {
            /**
             * Card artwork thumbnail.
             * Implementation identical to CardTileGrid thumbnail rendering logic.
             */
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
         * Same typography and overflow behavior as CardTileGrid.
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
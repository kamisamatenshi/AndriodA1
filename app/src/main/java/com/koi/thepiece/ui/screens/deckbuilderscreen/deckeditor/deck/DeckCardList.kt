package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
 * List-row UI for displaying a single card inside Deck Builder catalogue mode.
 *
 * This implementation follows the same structural pattern as:
 * - CardTileList (catalogscreen.components) in CardList.kt
 *
 * Structural Similarities:
 * - Rounded row container
 * - Coil AsyncImage thumbnail
 * - Weighted text column layout
 *
 * Key Differences:
 * - No owned quantity badge
 * - No +/- quantity controls
 * - Displays only card.code (minimal information)
 *
 * Refer to CardTileList for the full-featured list-row pattern.
 *
 * @param card Domain card model used for display.
 * @param imageLoader Shared Coil ImageLoader for consistent caching.
 * @param onClick Callback triggered when the row is tapped.
 */
@Composable
fun DeckTileList(
    card: Card,
    imageLoader: ImageLoader,
    onClick: () -> Unit
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
                .width(56.dp)
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(Modifier.width(10.dp))

        /**
         * Card text section.
         * weight(1f) ensures the title expands and the buttons stay aligned right.
         */
        Column(Modifier.weight(1f)) {
            Text(card.code?: "-", style = MaterialTheme.typography.titleSmall)
        }
    }
}
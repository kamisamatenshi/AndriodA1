package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.deck.deckdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
 * Reusable row component for displaying a card entry inside the deck builder UI.
 *
 * Responsibilities:
 * - Displays card thumbnail, code, name, and color
 * - Shows owned stock quantity using [Qtybadge]
 * - Provides a clickable surface to open card preview/details
 * - Optionally renders a trailing slot (e.g., +/- quantity controls)
 *
 * Parameters:
 * @param card Card metadata to render (name/code/color/imageUrl).
 * @param stockqty Owned quantity for this card (used by [Qtybadge]).
 * @param requiredqty Required quantity in deck (not rendered directly here, but passed for
 *                    consistency with callers that use the same signature).
 * @param imageLoader Coil ImageLoader instance used for caching and reuse.
 * @param modifier Optional modifier for outer row styling/placement.
 * @param onClick Called when user taps the row.
 * @param trailing Optional trailing composable slot (e.g., quantity stepper UI).
 *
 * Notes:
 * - Image caching is enabled via Coil diskCachePolicy/memoryCachePolicy.
 * - The stock badge is drawn as an overlay on the card image.
 */
@Composable
fun DeckCardRow(
    card: Card,
    stockqty: Int,
    requiredqty: Int,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail container (fixed width, card-like aspect ratio)
        Box(
            modifier = Modifier
                .width(72.dp)
                .aspectRatio(0.72f)
        ) {
            // Card image (cached)
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
                    .width(72.dp)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(8.dp))
            )

            // Owned stock badge overlay (top-right)
            Qtybadge(
                qty = stockqty,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Card text details
        Column(Modifier.weight(1f)) {
            Text(card.code ?: "-", style = MaterialTheme.typography.titleSmall)
            Text(card.name, style = MaterialTheme.typography.bodySmall)
            Text("Color: ${card.color}", style = MaterialTheme.typography.bodySmall)
        }

        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}
package com.koi.thepiece.ui.screens.catalogscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.catalogscreen.CatalogViewModel
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset

/**
 * Grid tile UI for displaying a single card in catalogue grid view.
 *
 * Features:
 * - Card artwork thumbnail rendered via Coil (with memory + disk cache enabled)
 * - Owned quantity badge (QtyBadge) displayed on the top-right corner
 * - Overlay +/- buttons for quick owned quantity adjustments
 * - Clickable tile for opening card detail / preview dialog
 *
 * Price flow:
 * - Observes price cache from CatalogViewModel (prices: Map<url, price>)
 * - Triggers price fetch via LaunchedEffect when the yuyuUrl changes
 * - Price value is computed but not displayed in this tile (used optionally by caller)
 *
 * @param card Domain card model used for UI rendering.
 * @param imageLoader Shared Coil ImageLoader for consistent caching behavior.
 * @param onClick Called when the tile is clicked (typically opens detail dialog).
 * @param onPlus Called when the + overlay button is pressed.
 * @param onMinus Called when the − overlay button is pressed.
 * @param viewModel ViewModel used to fetch and cache prices keyed by marketplace URL.
 */
@Composable
fun CardTileGrid(
    card: Card,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    viewModel: CatalogViewModel
) {
    /**
     * Observes cached prices from ViewModel.
     * Map structure: url -> priceInt
     */
    val prices by viewModel.prices.collectAsState()

    /**
     * Marketplace URL used as the key for fetching and caching price.
     */
    val url = card.yuyuUrl

    /**
     * Triggers a price fetch whenever the card URL changes.
     * Price results are stored inside ViewModel state and can be shared across UI components.
     *
     * If fetchPrice2 handles null internally, this is safe.
     * Otherwise, guard with `if (!url.isNullOrBlank())` before calling.
     */
    LaunchedEffect(url) {
        viewModel.fetchPrice2(url)
    }

    /**
     * Resolved price for this card from the ViewModel cache.
     * Null indicates price has not been fetched or URL is not available.
     */
    val price = if (!url.isNullOrBlank()) prices[url] else null

    /**
     * Tile layout:
     * - Image area with badge and overlay buttons
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
        // Image container (maintains consistent aspect ratio in grid)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
        ) {
            // Card artwork thumbnail
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
            // Owned quantity badge
            QtyBadge(
                qty = card.ownedQty,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(0.dp)

            )
            // Overlay +/- buttons for quick quantity adjustment
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverlayCircleButton(text = "−", onClick = onMinus)
                OverlayCircleButton(text = "+", onClick = onPlus)
            }
        }

        // Card code label
        Text(
            text = card.code.orEmpty().ifBlank { "-" },
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Displays a small circular badge representing owned card quantity.
 *
 * Visual rules:
 * - qty >= 4: gold badge (highlighted as "playset / extra") with shimmer effect
 * - qty > 0: green badge
 * - qty == 0: red badge (not owned)
 *
 * Motion rules:
 * - "Pop" animation triggers whenever qty changes (scale up then return)
 * - Shimmer animation runs continuously only for the gold badge state
 *
 * @param qty Owned quantity to display.
 * @param modifier Modifier to position/size the badge from the parent layout.
 */
@Composable
fun QtyBadge(qty: Int, modifier: Modifier = Modifier) {

    /**
     * Gold state used as a visual emphasis when quantity indicates at least 4 copies.
     */
    val isGold = qty >= 4

    // Badge colors and elevation based on quantity state.
    val backgroundColor: Color
    val contentColor: Color
    val elev: Dp

    when {
        qty >= 4 -> {
            backgroundColor = Color(0xFFD4AF37) // gold
            contentColor = Color.Black
            elev = 4.dp
        }
        qty > 0 -> {
            backgroundColor = Color(0xFF2E7D32) // green
            contentColor = Color.White
            elev = 0.dp
        }
        else -> {
            backgroundColor = MaterialTheme.colorScheme.error // red
            contentColor = MaterialTheme.colorScheme.onError
            elev = 0.dp
        }
    }

    /**
     * Pop animation: provides feedback when quantity changes.
     */
    val pop = remember { Animatable(1f) }
    LaunchedEffect(qty) {

            pop.snapTo(1f)
            pop.animateTo(1.12f, tween(120, easing = LinearEasing))
            pop.animateTo(1f, tween(160, easing = LinearEasing))

    }

    /**
     * Shimmer animation parameter used for gold state.
     * Animates a moving highlight band across the badge.
     */
    val shimmerT = rememberInfiniteTransition(label = "badgeShimmer")
        .animateFloat(
            initialValue = -0.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "badgeShimmerT"
        ).value

    /**
     * Shimmer modifier is applied only when qty >= 4.
     * Draws a translucent diagonal highlight band across the badge.
     */
    val shimmerModifier =
        if (isGold) Modifier.drawWithContent {
            drawContent()

            val w = size.width
            val h = size.height
            val bandW = w * 0.35f
            val startX = -bandW
            val endX = w + bandW
            val cx = startX + (endX - startX) * shimmerT

            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.22f),
                    Color.Transparent
                ),
                start = Offset(cx - bandW, 0f),
                end = Offset(cx + bandW, h)
            )
            drawRect(brush = brush)
        } else Modifier

    /**
     * Surface is used to apply shape, color, and elevation consistently.
     * graphicsLayer is used to apply pop scaling while keeping the badge circular.
     */
    Box(
        modifier = modifier.size(22.dp), // badge size
        contentAlignment = Alignment.Center
    ) {

        Surface(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    shape = CircleShape
                    clip = true
                }
                .then(shimmerModifier),
            shape = CircleShape,
            color = backgroundColor,
            contentColor = contentColor,
            tonalElevation = elev
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(qty.toString(), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Small circular overlay button used on top of the card image.
 *
 * Used for quick quantity adjustment actions inside CardTileGrid.
 *
 * @param text Label displayed inside the button (typically "+" or "−").
 * @param onClick Callback invoked when the button is pressed.
 */
@Composable
fun OverlayCircleButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        tonalElevation = 2.dp,
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

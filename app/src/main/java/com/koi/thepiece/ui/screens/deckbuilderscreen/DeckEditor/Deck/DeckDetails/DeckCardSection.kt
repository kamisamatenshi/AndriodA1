package com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.Deck.DeckDetails

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.Deck.DeckViewMode
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckUiState
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel
import com.koi.thepiece.ui.screens.deckbuilderscreen.QtyClass
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.text.orEmpty

@Composable
fun CardsSection(
    state: DeckUiState,
    vm: DeckViewModel,
    viewMode: DeckViewMode,
    imageLoader: ImageLoader,
) {
    val deckEntries = state.deck.entries.toList()

    when (viewMode) {
        DeckViewMode.LIST -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deckEntries, key = { it.key }) { (cardId, qty) ->
                    val card = state.allCards.firstOrNull { it.id == cardId } ?: return@items
                    DeckCardTileList(card, imageLoader, qty, vm , onClick = { vm.openCard(card) })
                }
            }
        }

        DeckViewMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(deckEntries, key = { it.key }) { (cardId, qty) ->
                    val card = state.allCards.firstOrNull { it.id == cardId } ?: return@items
                    DeckCardTileGrid(card, imageLoader, qty,vm ,onClick = { vm.openCard(card) })
                }
            }
        }
    }
}

@Composable
fun DeckCardTileList(
    card: Card,
    imageLoader: ImageLoader,
    qty: QtyClass,
    vm: DeckViewModel,
    onClick: () -> Unit
){
    DeckCardRow(
        card = card,
        stockqty = qty.stockQty,
        requiredqty = qty.requiredQty,
        imageLoader = imageLoader,
        onClick = onClick,
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 6.dp),

                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canMinus = qty.requiredQty > 0
                    val canPlus = qty.requiredQty < 4

                    OverlayCircleButton(text = "−", enabled = canMinus, onClick = { vm.removeFromDeck(card) })
                    Text(
                        text = qty.requiredQty.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center
                    )

                    OverlayCircleButton(text = "+", enabled = canPlus, onClick = { vm.addToDeck(card) })
                }
            }
        }
    )
}

@Composable
fun DeckCardTileGrid(
    card: Card,
    imageLoader: ImageLoader,
    qty: QtyClass,
    vm: DeckViewModel,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
        ) {
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

            // Stock (top-left)
            Qtybadge(
                qty.stockQty,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )

            // Required (top-right)
            RequiredBadge(
                qty.requiredQty,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }



        Text(
            text = card.code.orEmpty().ifBlank { "-" },
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun Qtybadge(qty: Int, modifier: Modifier = Modifier) {

    val isGold = qty >= 4

    // Colors
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

    val pop = remember { Animatable(1f) }
    LaunchedEffect(qty) {

        pop.snapTo(1f)
        pop.animateTo(1.12f, tween(120, easing = LinearEasing))
        pop.animateTo(1f, tween(160, easing = LinearEasing))

    }

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

@Composable
fun RequiredBadge(
    qty: Int,
    modifier: Modifier = Modifier
) {

    val pop = remember { Animatable(1f) }
    LaunchedEffect(qty) {

        pop.snapTo(1f)
        pop.animateTo(1.12f, tween(120, easing = LinearEasing))
        pop.animateTo(1f, tween(160, easing = LinearEasing))

    }


    Surface(
        modifier = modifier.size(22.dp)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                shape = CircleShape
                clip = true
            },
        shape = CircleShape,
        color = Color.White,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = qty.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black
            )
        }
    }
}

@Composable
fun ReqStockPill(
    req: Int,
    stock: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 6.dp
    ) {
        Text(
            text = "${req}/${stock}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
@Composable
fun OverlayCircleButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        tonalElevation = 2.dp,
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}
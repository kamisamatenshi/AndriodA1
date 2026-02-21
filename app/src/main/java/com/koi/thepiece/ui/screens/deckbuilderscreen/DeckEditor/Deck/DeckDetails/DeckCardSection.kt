package com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.Deck.DeckDetails

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckEditor.Deck.DeckViewMode
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckUiState
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun CardsSection(
    state: DeckUiState,
    vm: DeckViewModel,
    viewMode: DeckViewMode,
    imageLoader: ImageLoader
) {
    val deckEntries = state.deck.entries.toList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(deckEntries, key = { it.key }) { (cardId, qty) ->
            val card = state.allCards.firstOrNull { it.id == cardId } ?: return@items

            DeckCardRow(
                card = card,
                imageLoader = imageLoader,
                trailing = {
                    Column(horizontalAlignment = Alignment.End) {
                        Qtybadge(
                            qty,
                            modifier = Modifier.align(Alignment.End)
                        )
                        IconButton(onClick = { vm.removeFromDeck(card) }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Remove")
                        }
                    }
                }
            )
        }
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

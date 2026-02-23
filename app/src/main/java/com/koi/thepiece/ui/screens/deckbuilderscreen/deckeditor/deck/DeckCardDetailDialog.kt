package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.screens.deckbuilderscreen.DeckViewModel

/**
 * Displays a detailed card preview dialog for Deck Builder.
 *
 * This dialog follows the same structural pattern as:
 * - CardPreviewDialog (catalogscreen.components)
 *
 * Structural Similarities:
 * - Card artwork preview with pinch-to-zoom and pan (transformable + graphicsLayer)
 * - Skill text display with JP/EN toggle
 * - Card metadata display (color, type, set, rarity, traits, obtain source)
 * - Live price display fetched through ViewModel (cached by URL)
 *
 * Key Differences (Deck Builder):
 * - Uses DeckViewModel price cache + fetch (instead of CatalogViewModel)
 * - Reads deck composition state to display requiredQty currently in deck
 * - Shows deck add/remove controls (max 4) instead of owned qty controls
 * - `normal` flag gates whether quantity controls are shown (your deck rules)
 *
 * @param card Domain card model for display.
 * @param imageLoader Shared Coil ImageLoader for consistent caching behavior.
 * @param onDismiss Called when the dialog is dismissed.
 * @param viewModel DeckViewModel used for deck state + price caching/fetching.
 * @param normal If true, show quantity controls (Event/Stage/Normal logic decided by caller).
 * @param onAddToDeck Callback hook for adding card (note: you also call viewModel.addToDeck directly).
 */
@Composable
fun DeckPreviewDialog(
    card: Card,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
    viewModel: DeckViewModel,
    normal: Boolean,
    onAddToDeck: (Card) -> Unit
) {
    /**
     * Observes cached price results from the ViewModel.
     * Map structure: url -> price (Int)
     */
    val prices by viewModel.prices.collectAsState()

    /**
     * Marketplace URL used as the price lookup key.
     * If url is null/blank, price fetching is skipped.
     */
    val url = card.yuyuUrl

    /**
     * Triggers a price fetch whenever the URL changes.
     * LaunchedEffect ensures the call runs once per unique URL.
     */
    LaunchedEffect(url) {
        viewModel.fetchPrice2(url)
    }

    /**
     * Looks up the latest fetched price for the card URL.
     * If not available yet, price remains null and UI shows a loading label.
     */
    val price = if (!url.isNullOrBlank()) prices[url] else null

    // Zoom/pan state for card image preview. (Same as CardPreviewDialog)
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    /**
     * Deck-specific derived state:
     * - Observes DeckUiState so we can read the current requiredQty in deck.
     * - qtyInDeck is used to enable/disable +/- (0..4) and display current count.
     */
    val ui by viewModel.state.collectAsState()
    val qtyInDeck = ui.deck[card.id]?.requiredQty ?: 0

    /**
     * Gesture state for pinch-to-zoom and panning.
     * Zoom is clamped to avoid excessive scaling.
     */
    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    /**
     * Language toggle for skill text display.
     * false => JP, true => EN
     */
    var isEnglish by remember { mutableStateOf(false) }

    /**
     * AlertDialog layout:
     * - title: card code (optional) and name
     * - text: main content (image, skills, metadata, deck controls)
     * - confirmButton: contains JP/EN toggle and close action
     *
     * (Same comment as CardPreviewDialog; content differs in the deck controls)
     */
    AlertDialog(
        onDismissRequest = onDismiss,
        /**
         * Custom dialog footer row:
         * - Language toggle (JP/EN)
         * - Close button
         */
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Language toggle container (JP / EN) (Same as CardPreviewDialog)
                Row(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {

                    // JP option (Same as CardPreviewDialog)
                    Box(
                        modifier = Modifier
                            .background(if (!isEnglish) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isEnglish = false }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JP",
                            color = if (!isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // EN option (Same as CardPreviewDialog)
                    Box(
                        modifier = Modifier
                            .background(if (isEnglish) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isEnglish = true }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EN",
                            color = if (isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Close action (Same as CardPreviewDialog)
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    )
                ) {
                    Text("Close", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },

        /**
         * Title section:
         * - Card code is shown when name differs from code (avoids duplicating for special cases)
         * - Card name displayed as the main title
         */
        title = {
            Column {
                // Display Code if it is not DON card (Same as CardPreviewDialog)
                if (!card.name.isNullOrBlank() && card.name != card.code) {
                    Text(
                        text = card.code ?: "Card",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },

        /**
         * Main content:
         * - Card image with zoom/pan
         * - Skill text box with scroll + language toggle
         * - Card metadata fields
         * - Price display (JPY or converted SGD)
         * - Deck quantity controls (+/-) gated by `normal`
         */
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                /**
                 * Card artwork (cached via Coil policies)
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
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .aspectRatio(0.69f)
                        .transformable(state)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )

                Spacer(Modifier.height(10.dp))

                /**
                 * Skill text (scrollable box)
                 */
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Skill: ", style = MaterialTheme.typography.bodyMedium)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 25.dp, max = 50.dp)
                            .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (isEnglish) card.skillEn ?: "-" else card.skillJp ?: "-",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                /**
                 * Card metadata + price display
                 */
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Color:")
                        Text(card.color)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Type:")
                        Text(card.type)
                    }

                    if (!card.cardSet.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Set:")
                            Text(card.cardSet)
                        }
                    }

                    if (!card.rarity.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rarity:")
                            Text(card.rarity)
                        }
                    }

                    if (!card.traits.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Traits:")
                            Text(card.traits)
                        }
                    }

                    if (!card.obtainFrom.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Obtain:")
                            Text(card.obtainFrom)
                        }
                    }

                    /**
                     * Price display:
                     * - Shows "Loading..." until price is retrieved
                     * - EN mode converts to SGD using a fixed conversion ratio (JPY/SGD ~ 120)
                     */
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Price:")
                        val displayText = when {
                            price == null -> "Loading..."
                            isEnglish -> {
                                val sgd = price.toDouble() / 120.0
                                "S$${"%.2f".format(sgd)}"
                            }
                            else -> "¥${"%,d".format(price)}"
                        }
                        Text(displayText)
                    }
                }

                /**
                 * Deck-specific controls:
                 * - Shown only when `normal == true`
                 * - Uses qtyInDeck from DeckUiState (requiredQty)
                 * - Minus disabled at 0
                 * - Plus disabled at 4
                 */
                if (normal) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quantity selector row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Minus
                            OverlayCircleButton(
                                text = "−",
                                enabled = qtyInDeck > 0,
                                onClick = { viewModel.removeFromDeck(card) }
                            )

                            // Qty display
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                tonalElevation = 1.dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = qtyInDeck.toString(),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            // Plus
                            OverlayCircleButton(
                                text = "+",
                                enabled = qtyInDeck < 4,
                                onClick = { viewModel.addToDeck(card) }
                            )
                        }
                    }
                }
            }
        }
    )
}

/**
 * Small circular overlay button used inside DeckPreviewDialog.
 *
 * This follows the same pattern as OverlayCircleButton used in CardTileGrid,
 * but adds an enabled flag for deck rule clamping (0..4).
 *
 * @param text Label displayed inside the button (typically "+" or "−").
 * @param enabled Controls whether the button is clickable.
 * @param onClick Callback invoked when the button is pressed.
 */
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
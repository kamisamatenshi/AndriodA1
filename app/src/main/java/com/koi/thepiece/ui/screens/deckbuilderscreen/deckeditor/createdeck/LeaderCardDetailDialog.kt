package com.koi.thepiece.ui.screens.deckbuilderscreen.deckeditor.createdeck

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * Displays a detailed Leader preview dialog for the deck creation flow.
 *
 * This dialog intentionally follows the same structural pattern as CardPreviewDialog
 * (catalogscreen.components) to standardize the UI and preserve user muscle memory
 * across Catalogue browsing and Deck Builder flows.
 *
 * UI Alignment with CardPreviewDialog:
 * - AlertDialog-based layout with custom footer row
 * - Card artwork preview with pinch-to-zoom and pan (transformable + graphicsLayer)
 * - Skill text display with JP/EN toggle (isEnglish flag)
 * - Scrollable skill text box with consistent sizing and border styling
 * - Card metadata display (color, type, set, rarity, traits, obtain source)
 * - Live price display fetched through ViewModel (cached by yuyuUrl)
 * - JP mode shows JPY formatting; EN mode displays SGD conversion (fixed JPY/SGD ratio)
 *
 * Key Differences (Leader / Deck Creation):
 * - Uses DeckViewModel price cache + fetch instead of CatalogViewModel
 * - Does NOT expose owned quantity +/− inventory controls
 * - Adds a primary action ("Build with this leader") to advance the flow
 * - onGoCreateNewDeck(card) is the transition hook into deck creation
 *
 * Data flow:
 * - Dialog receives Card domain model for display.
 * - Price is retrieved via DeckViewModel and observed via StateFlow (prices: Map<url, Int>).
 * - LaunchedEffect(url) triggers price fetch when yuyuUrl changes.
 *
 * @param card Domain card model for display.
 * @param imageLoader Shared Coil ImageLoader for consistent caching behavior.
 * @param onDismiss Called when the dialog is dismissed.
 * @param viewModel DeckViewModel used for deck state + price caching/fetching.
 * @param onGoCreateNewDeck Callback invoked when user confirms leader to start deck creation.
 */
@Composable
fun LeaderPreviewDialog(
    card: Card,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
    viewModel: DeckViewModel,
    onGoCreateNewDeck: (Card) -> Unit
) {
    /**
     * Observes cached price results from the ViewModel.
     * Map structure: url -> price (Int)
     */
    val prices by viewModel.prices.collectAsState()

    /**
     * Marketplace URL used as the price lookup key.
     * If url is null/blank, price lookup remains null and UI shows "Loading...".
     */
    val url = card.yuyuUrl

    /**
     * Triggers a price fetch whenever the URL changes.
     * LaunchedEffect ensures the call runs once per unique URL.
     *
     * Note: If fetchPrice2 does not handle null internally, guard with:
     * if (!url.isNullOrBlank()) viewModel.fetchPrice2(url)
     */
    LaunchedEffect(url) {
        viewModel.fetchPrice2(url)
    }

    /**
     * Looks up the latest fetched price for the card URL.
     * Null indicates price has not been fetched yet or URL is unavailable.
     */
    val price = if (!url.isNullOrBlank()) prices[url] else null

    // Zoom/pan state for card image preview.
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

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
     * - title: card code (optional) + card name
     * - text: artwork, skill, metadata, price, primary action
     * - confirmButton: JP/EN toggle + Close button
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

                // Language toggle container (JP / EN)
                Row(
                    modifier = Modifier
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                ) {

                    // JP option
                    Box(
                        modifier = Modifier
                            .background(
                                if (!isEnglish) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
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

                    // EN option
                    Box(
                        modifier = Modifier
                            .background(
                                if (isEnglish) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
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

                // Close action
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    )
                ) {
                    Text(
                        "Close",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },

        /**
         * Title section:
         * - Card code is shown when name differs from code (avoids duplicating for some cases)
         * - Card name displayed as the main title
         */
        title = {
            Column {
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
         * - Primary flow action: Build with this leader
         */
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Card artwork (cached via Coil policies)
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
                        .fillMaxWidth()
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

                // Skill text (scrollable box)
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
                                text = if (isEnglish) (card.skillEn ?: "-") else (card.skillJp ?: "-"),
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Card metadata + price display
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) { Text("Color:"); Text(card.color) }

                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) { Text("Type:"); Text(card.type) }

                    if (!card.cardSet.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) { Text("Set:"); Text(card.cardSet) }
                    }

                    if (!card.rarity.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) { Text("Rarity:"); Text(card.rarity) }
                    }

                    if (!card.traits.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) { Text("Traits:"); Text(card.traits) }
                    }

                    if (!card.obtainFrom.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) { Text("Obtain:"); Text(card.obtainFrom) }
                    }

                    /**
                     * Price display:
                     * - Shows "Loading..." until price is retrieved
                     * - JP mode shows JPY formatting
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

                Spacer(Modifier.height(20.dp))

                /**
                 * Primary action for leader flow:
                 * Proceeds into deck creation using the selected leader card.
                 */
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onGoCreateNewDeck(card) }) {
                        Text("Build with this leader")
                    }
                }
            }
        }
    )
}
package com.koi.thepiece.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.ui.components.SfxButton
import com.koi.thepiece.ui.components.SfxFAB
import com.koi.thepiece.ui.screens.catalogscreen.CatalogViewModel
import com.koi.thepiece.ui.screens.catalogscreen.components.QtyBadge
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// ─── Data types ──────────────────────────────────────────────────────────────

enum class CardVariant(val rarityKey: String, val label: String) {
    UNKNOWN("", "—"),
    A_SEC("p-sec", "A-SEC"),
    SEC("sec",     "SEC"),
    A_SR("p-sr",   "A-SR"),
    SR("sr",       "SR"),
    A_R("p-r",     "A-R"),
    R("r",         "R"),
    A_L("p-l",     "A-L"),
    L("l",         "L"),
    SP("sp",       "SP"),
    UC("uc",       "UC"),
    C("c",         "C");

    companion object {
        fun fromRarity(rarity: String?): CardVariant? {
            val key = rarity?.lowercase() ?: return null
            return entries.firstOrNull { it != UNKNOWN && it.rarityKey == key }
        }
    }
}

data class CardEntry(
    val code: String,
    val quantity: Int,
    val variant: CardVariant = CardVariant.UNKNOWN,
    val selectedCardId: Int? = null,
    var ownedQty: Int,
)

/**
 * Builds the map key used to uniquely identify a [CardEntry] in the detected cards map.
 *
 * Format: `"CODE|VARIANT_NAME|CARD_ID"`, where CARD_ID is omitted when null
 * (e.g. `"OP01-001|RARE|42"` or `"OP01-001|UNKNOWN|"`).
 *
 * @param code The card code (e.g. `"OP01-001"`).
 * @param variant The card's [CardVariant].
 * @param cardId The specific card ID when a printing has been selected, or null.
 * @return A pipe-delimited string key.
 */
fun cardKey(code: String, variant: CardVariant, cardId: Int? = null) =
    "$code|${variant.name}|${cardId ?: ""}"

// ─── OCR helpers ─────────────────────────────────────────────────────────────

/**
 * Extracts and normalizes a card code from raw OCR or user-entered text.
 *
 * Expected format: `XX00-000` where the first two characters are letters (A–Z or 0/O),
 * followed by two alphanumeric digits, a dash, and three digits.
 *
 * Normalization applied:
 * - Em dashes (–) are replaced with hyphens (-)
 * - The 2-character prefix has digits coerced to 'O' (e.g. `AB0C` → `ABOC`)
 * - The numeric suffix has 'O' characters coerced to '0' (e.g. `O1O` → `010`)
 *
 * @param rawText The raw input string to search within.
 * @return The first matching and normalized card code, or `null` if none is found.
 */
fun extractCardCode(rawText: String): String? {
    val codeRegex = Regex("""[A-Z][A-Z0-9][0-9O]{2}[-–][0-9O]{3}""")
    return codeRegex.find(rawText)?.value
        ?.replace('–', '-')
        ?.let { raw ->
            val dashIdx = raw.indexOf('-')
            if (dashIdx < 2) return@let raw
            val prefix  = raw.substring(0, 2).replace('0', 'O')
            val numbers = raw.substring(2).replace('O', '0')
            prefix + numbers
        }
}

/**
 * Crops the horizontal center strip of a bitmap, targeting the region most likely
 * to contain a card code in a typical card scan or photo.
 *
 * The crop region is defined as a fixed proportion of the bitmap dimensions:
 * - Horizontally: the middle 80%, skipping 10% on each side
 * - Vertically:   the middle 30%, starting at 35% from the top
 *
 * All coordinates are clamped to valid bitmap bounds before cropping.
 *
 * @param bitmap The source bitmap to crop. Not recycled or modified.
 * @return A new bitmap representing the cropped center strip.
 */
fun cropCenterStrip(bitmap: Bitmap): Bitmap {
    val cropLeft   = (bitmap.width  * 0.10f).toInt()
    val cropWidth  = (bitmap.width  * 0.80f).toInt()
    val cropTop    = (bitmap.height * 0.35f).toInt()
    val cropHeight = (bitmap.height * 0.30f).toInt()
    val safeLeft   = cropLeft.coerceIn(0, bitmap.width - 1)
    val safeTop    = cropTop.coerceIn(0, bitmap.height - 1)
    val safeWidth  = cropWidth.coerceAtMost(bitmap.width - safeLeft)
    val safeHeight = cropHeight.coerceAtMost(bitmap.height - safeTop)
    return Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeWidth, safeHeight)
}

/**
 * Upscales a bitmap by 2.5x to improve OCR accuracy on small or low-resolution images.
 *
 * Scaling is applied via a [Matrix] with bilinear filtering enabled, which produces
 * smoother edges compared to nearest-neighbour scaling.
 *
 * @param src The source bitmap to scale. Not recycled or modified.
 * @return A new bitmap scaled to 2.5x the original dimensions.
 */
fun preprocessBitmap(src: Bitmap): Bitmap {
    val scale = 2.5f
    val matrix = Matrix().apply { postScale(scale, scale) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}

/**
 * Converts this [ImageProxy] to a correctly-oriented [Bitmap].
 *
 * Assumes the image is in RGBA_8888 format (e.g. as produced by CameraX ImageAnalysis
 * with [androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888]).
 * Using a different format will produce corrupted pixel data.
 *
 * Rotation metadata from [ImageInfo.rotationDegrees] is applied so the returned
 * bitmap is upright regardless of device orientation at capture time.
 *
 * @receiver An [ImageProxy] with at least one plane containing raw pixel data.
 * @return A new upright [Bitmap] in ARGB_8888 format.
 */
fun ImageProxy.toBitmap(): Bitmap {
    val buffer: ByteBuffer = planes[0].buffer
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(buffer)
    val matrix = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// ─── Camera helpers ───────────────────────────────────────────────────────────

/**
 * Triggers continuous autofocus and auto-exposure metering at the center of the preview.
 *
 * Initiates a [FocusMeteringAction] targeting the center point (0.5, 0.5) of the
 * [PreviewView], with both AF and AE flags set. The action auto-cancels after 3 seconds,
 * after which the camera returns to its default continuous-focus behavior.
 *
 * Does nothing if [cameraControl] is null.
 *
 * @param cameraControl The [CameraControl] instance for the active camera session, or null.
 * @param previewView The [PreviewView] used to derive the metering point coordinate space.
 */
fun triggerContinuousAutofocus(cameraControl: CameraControl?, previewView: PreviewView) {
    cameraControl ?: return
    val meteringPoint = previewView.meteringPointFactory.createPoint(0.5f, 0.5f)
    val action = FocusMeteringAction.Builder(
        meteringPoint,
        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
    )
        .setAutoCancelDuration(3, TimeUnit.SECONDS)
        .build()
    cameraControl.startFocusAndMetering(action)
}

// ─── Card list helpers ────────────────────────────────────────────────────────

/**
 * Returns all cards whose code matches [code], case-insensitively.
 *
 * @param code The card code to search for.
 * @param allCards The pool of cards to search within.
 * @return A list of matching cards, or an empty list if none are found.
 */
fun matchingCards(code: String, allCards: List<Card>): List<Card> =
    allCards.filter { it.code.equals(code, ignoreCase = true) }

/**
 * Returns the subset of known [CardVariant]s represented in the given list of cards,
 * ordered by their declaration order in [CardVariant.entries].
 *
 * [CardVariant.UNKNOWN] is always excluded from the result, even if present in the list.
 *
 * @param cards The cards to derive available variants from.
 * @return A filtered, ordered list of [CardVariant]s with no duplicates.
 */
fun availableVariantsFor(cards: List<Card>): List<CardVariant> {
    val present = cards.mapNotNull { CardVariant.fromRarity(it.rarity) }.toSet()
    return CardVariant.entries.filter { it != CardVariant.UNKNOWN && it in present }
}

/**
 * Resolves a [CardEntry] to a specific [Card] from a pre-filtered candidate list,
 * and updates [CardEntry.ownedQty] as a side effect.
 *
 * Resolution priority:
 * 1. If [CardEntry.selectedCardId] is set, match by ID.
 * 2. If the variant is [CardVariant.UNKNOWN], fall back to the first candidate.
 * 3. Otherwise, match by variant via [CardVariant.fromRarity].
 *
 * [CardEntry.ownedQty] is set to 0 for [CardVariant.UNKNOWN] entries, or to the
 * resolved card's owned quantity if found, or 0 if resolution failed.
 *
 * @param entry The card entry to resolve. [CardEntry.ownedQty] will be mutated.
 * @param cards A pre-filtered list of candidate cards (e.g. matching by code).
 * @return The resolved [Card], or `null` if no match was found.
 */
fun resolveCardEntity(entry: CardEntry, cards: List<Card>): Card? {

    val resolved = when {
        entry.selectedCardId != null ->
            cards.find { it.id == entry.selectedCardId }

        entry.variant == CardVariant.UNKNOWN ->
            cards.firstOrNull()

        else ->
            cards.find { CardVariant.fromRarity(it.rarity) == entry.variant }
    }


    entry.ownedQty = when {
        entry.variant == CardVariant.UNKNOWN -> 0
        else -> resolved?.ownedQty ?: 0
    }

    return resolved
}

/**
 * Applies a variant selection to the detected cards map, re-keying the entry from
 * [oldKey] to [newKey] and merging quantities if an entry already exists at [newKey].
 *
 * If [newKey] is already present, the existing entry is preserved and [entry]'s
 * quantity is added to it. Otherwise, [entry] is inserted with [variant] and
 * [cardId] applied.
 *
 * @param detectedCards The current state of detected cards. Not modified.
 * @param oldKey The key of the entry being replaced.
 * @param newKey The key the resolved entry should be stored under.
 * @param entry The card entry being updated.
 * @param variant The newly selected [CardVariant] to apply if no merge occurs.
 * @param cardId The selected card ID to apply if no merge occurs.
 * @return A new [LinkedHashMap] reflecting the variant selection, preserving insertion order.
 */
fun applyVariantSelection(
    detectedCards: LinkedHashMap<String, CardEntry>,
    oldKey: String,
    newKey: String,
    entry: CardEntry,
    variant: CardVariant,
    cardId: Int
): LinkedHashMap<String, CardEntry> {
    val updated = LinkedHashMap(detectedCards)
    updated.remove(oldKey)
    val existing = updated[newKey]
    updated[newKey] = if (existing != null) {
        existing.copy(quantity = existing.quantity + entry.quantity)
    } else {
        entry.copy(variant = variant, selectedCardId = cardId)
    }
    return updated
}

// ─── Composables ─────────────────────────────────────────────────────────────

/**
 * A selectable tile displaying a card's art, used for choosing between variant printings.
 *
 * Renders the card image at a fixed 90dp width with a standard card aspect ratio (0.72).
 * A highlighted border is shown when [isSelected] is true. If [showAltLabel] is true,
 * an "Alt N" label is shown below the image, useful for distinguishing multiple alternate
 * art variants of the same card.
 *
 * Images are loaded via [imageLoader] with both memory and disk caching enabled.
 *
 * @param card The card whose art and image URL are displayed.
 * @param isSelected Whether this tile is the currently selected variant.
 * @param altIndex The 1-based index shown in the alt label (e.g. "Alt 2").
 * @param showAltLabel Whether to show the "Alt N" label below the image.
 * @param imageLoader The [ImageLoader] instance used for async image loading.
 * @param colorPrimary Accent colour applied to the border and label when selected.
 * @param colorOnSurfaceVariant Muted colour applied to the border and label when unselected.
 * @param onClick Called when the tile is tapped.
 */
@Composable
private fun VariantArtTile(
    card: Card,
    isSelected: Boolean,
    altIndex: Int,
    showAltLabel: Boolean,
    imageLoader: ImageLoader,
    colorPrimary: androidx.compose.ui.graphics.Color,
    colorOnSurfaceVariant: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colorPrimary else colorOnSurfaceVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(card.imageUrl)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .scale(Scale.FIT)
                .build(),
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(6.dp))
                .background(colorOnSurfaceVariant.copy(alpha = 0.1f))
        )
        if (showAltLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Alt $altIndex",
                fontSize = 11.sp,
                color = if (isSelected) colorPrimary else colorOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Displays a labeled, horizontally scrollable row of [VariantArtTile]s for a single
 * [CardVariant], allowing the user to select a specific printing.
 *
 * The variant label is highlighted with [colorPrimary] when [variant] matches the
 * currently selected variant in [entry], and muted otherwise.
 *
 * A tile is considered selected if [variant] matches [entry.variant] and either:
 * - The tile's card ID matches [entry.selectedCardId], or
 * - No card ID is explicitly selected and the tile is the first in the row.
 *
 * Alt labels ("Alt 1", "Alt 2", …) are shown below tiles only when there are
 * multiple cards in [variantCards].
 *
 * @param variant The card variant this section represents.
 * @param variantCards The list of cards belonging to this variant (alternate printings).
 * @param entry The current [CardEntry] used to determine the active selection.
 * @param imageLoader The [ImageLoader] instance passed through to each [VariantArtTile].
 * @param colorPrimary Accent colour used for the active variant label and selected tile border.
 * @param colorOnSurface Base content colour (currently unused directly but passed for consistency).
 * @param colorOnSurfaceVariant Muted colour used for inactive labels and tile borders.
 * @param onSelect Called with the tapped [Card] when the user selects a tile.
 */
@Composable
private fun VariantSection(
    variant: CardVariant,
    variantCards: List<Card>,
    entry: CardEntry,
    imageLoader: ImageLoader,
    colorPrimary: androidx.compose.ui.graphics.Color,
    colorOnSurface: androidx.compose.ui.graphics.Color,
    colorOnSurfaceVariant: androidx.compose.ui.graphics.Color,
    onSelect: (Card) -> Unit
) {
    Text(
        text = variant.label,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (variant == entry.variant) colorPrimary else colorOnSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp)
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        variantCards.forEachIndexed { index, variantCard ->
            val isSelected = variant == entry.variant &&
                    (entry.selectedCardId == variantCard.id ||
                            (entry.selectedCardId == null && index == 0))
            VariantArtTile(
                card = variantCard,
                isSelected = isSelected,
                altIndex = index + 1,
                showAltLabel = variantCards.size > 1,
                imageLoader = imageLoader,
                colorPrimary = colorPrimary,
                colorOnSurfaceVariant = colorOnSurfaceVariant,
                onClick = { onSelect(variantCard) }
            )
        }
    }
}

/**
 * A dialog that lets the user pick a specific variant and printing for a scanned card.
 *
 * Renders a vertically scrollable list of [VariantSection]s, one per entry in
 * [availableVariants]. Each section receives the subset of [allMatchingCards] that
 * belong to that variant, filtered by [CardVariant.fromRarity].
 *
 * Dismissed by tapping outside the dialog or via [onDismiss].
 *
 * @param entry The [CardEntry] representing the current selection state.
 * @param allMatchingCards All cards matching the scanned code, across all variants.
 * @param availableVariants The ordered list of variants to display, typically derived
 * from [availableVariantsFor].
 * @param imageLoader The [ImageLoader] instance passed through to each [VariantSection].
 * @param colorPrimary Accent colour forwarded to [VariantSection] for active selections.
 * @param colorOnSurface Base content colour forwarded to [VariantSection].
 * @param colorOnSurfaceVariant Muted colour forwarded to [VariantSection] for inactive elements.
 * @param onDismiss Called when the dialog is dismissed without a selection.
 * @param onSelect Called with the chosen [CardVariant] and [Card] when the user taps a tile.
 */
@Composable
private fun VariantPickerDialog(
    entry: CardEntry,
    allMatchingCards: List<Card>,
    availableVariants: List<CardVariant>,
    imageLoader: ImageLoader,
    colorPrimary: androidx.compose.ui.graphics.Color,
    colorOnSurface: androidx.compose.ui.graphics.Color,
    colorOnSurfaceVariant: androidx.compose.ui.graphics.Color,
    onDismiss: () -> Unit,
    onSelect: (CardVariant, Card) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Rarity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                availableVariants.forEach { variant ->
                    val variantCards = allMatchingCards.filter {
                        CardVariant.fromRarity(it.rarity) == variant
                    }
                    VariantSection(
                        variant = variant,
                        variantCards = variantCards,
                        entry = entry,
                        imageLoader = imageLoader,
                        colorPrimary = colorPrimary,
                        colorOnSurface = colorOnSurface,
                        colorOnSurfaceVariant = colorOnSurfaceVariant,
                        onSelect = { card -> onSelect(variant, card) }
                    )
                }
            }
        }
    }
}

/**
 * A single row in the scanned card list, displaying a card's thumbnail, code, rarity,
 * price, quantity, and +/− controls.
 *
 * Tapping the rarity badge opens a [VariantPickerDialog]. Selecting a variant re-keys
 * the entry in [currentDetectedCards] via [applyVariantSelection] and notifies the
 * caller via [onDetectedCardsChange].
 *
 * Price is fetched lazily via [CatalogViewModel.fetchPrice2] whenever the resolved
 * [priceUrl] changes. While the variant is [CardVariant.UNKNOWN] no fetch is made
 * and "—" is shown. "..." indicates a fetch in progress; "N/A" indicates no URL
 * was available for the resolved card.
 *
 * The +/− buttons increment or decrement [CardEntry.quantity]. Decrementing to zero
 * removes the entry from [currentDetectedCards] entirely.
 *
 * @param mapKey The key identifying this entry in [currentDetectedCards].
 * @param entry The card entry to display.
 * @param allCards The full card pool used to resolve variants and card entity.
 * @param viewModel The [CatalogViewModel] used for price fetching.
 * @param imageLoader The [ImageLoader] instance used for the card thumbnail.
 * @param colorOnSurface Base content colour for text and icons.
 * @param colorOnSurfaceVariant Muted colour for secondary text and inactive borders.
 * @param colorPrimary Accent colour for promo/SP rarity labels and the add button.
 * @param colorError Colour applied to the rarity badge and border when variant is [CardVariant.UNKNOWN].
 * @param useSgd If true, prices are displayed in SGD rather than JPY.
 * @param onDetectedCardsChange Called with the updated map after any quantity or variant change.
 * @param currentDetectedCards The current state of all detected cards, used as the base for updates.
 * @param onCodeDetected Called with [CardEntry.code] after a quantity increment, e.g. to trigger a rescan beep.
 */
@Composable
private fun CardRow(
    mapKey: String,
    entry: CardEntry,
    allCards: List<Card>,
    viewModel: CatalogViewModel,
    imageLoader: ImageLoader,
    colorOnSurface: androidx.compose.ui.graphics.Color,
    colorOnSurfaceVariant: androidx.compose.ui.graphics.Color,
    colorPrimary: androidx.compose.ui.graphics.Color,
    colorError: androidx.compose.ui.graphics.Color,
    useSgd: Boolean,
    onDetectedCardsChange: (LinkedHashMap<String, CardEntry>) -> Unit,
    currentDetectedCards: LinkedHashMap<String, CardEntry>,
    onCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val cards = remember(entry.code, allCards) { matchingCards(entry.code, allCards) }
    val availableVariants = remember(entry.code, allCards) { availableVariantsFor(cards) }
    val cardEntity = remember(entry.code, entry.variant, entry.selectedCardId, entry.ownedQty, allCards) {
        resolveCardEntity(entry, cards)
    }
    var pickerVisible by remember { mutableStateOf(false) }

    // Resolve yuyuUrl from the variant-specific card so price updates when rarity changes.
    // While variant is UNKNOWN we show "—" — price is meaningless until rarity is picked.
    val priceUrl = remember(entry.variant, entry.selectedCardId, cardEntity) {
        if (entry.variant == CardVariant.UNKNOWN) null else cardEntity?.yuyuUrl
    }
    val prices by viewModel.prices.collectAsState()

    LaunchedEffect(priceUrl) {
        viewModel.fetchPrice2(priceUrl)
    }

    val priceYen = if (!priceUrl.isNullOrBlank()) prices[priceUrl] else null
    val priceText = when {
        entry.variant == CardVariant.UNKNOWN -> "—"
        priceUrl.isNullOrBlank()             -> "N/A"
        priceYen == null                     -> "..."
        useSgd -> "S${"$"}${"%.2f".format(priceYen / 115.0)}"
        else   -> "¥$priceYen"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(36.dp)
                .aspectRatio(0.72f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(cardEntity?.imageUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .scale(Scale.FIT)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = entry.code,
                modifier = Modifier
                    .width(36.dp)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorOnSurfaceVariant.copy(alpha = 0.1f))
            )
            QtyBadge(
                qty = cardEntity?.ownedQty ?: 0,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        // Card code
        Text(
            entry.code,
            color = colorOnSurface,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        // Rarity badge
        Box(
            modifier = Modifier
                .width(56.dp)
                .border(
                    width = 1.dp,
                    color = if (entry.variant == CardVariant.UNKNOWN)
                        colorError.copy(alpha = 0.6f)
                    else
                        colorOnSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable { pickerVisible = true }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.variant.label,
                color = when (entry.variant) {
                    CardVariant.UNKNOWN -> colorError
                    else -> if (entry.variant.rarityKey.startsWith("p-") || entry.variant == CardVariant.SP)
                        colorPrimary else colorOnSurface
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        if (pickerVisible) {
            VariantPickerDialog(
                entry = entry,
                allMatchingCards = cards,
                availableVariants = availableVariants,
                imageLoader = imageLoader,
                colorPrimary = colorPrimary,
                colorOnSurface = colorOnSurface,
                colorOnSurfaceVariant = colorOnSurfaceVariant,
                onDismiss = { pickerVisible = false },
                onSelect = { variant, card ->
                    pickerVisible = false
                    val newKey = cardKey(entry.code, variant, card.id)
                    onDetectedCardsChange(
                        applyVariantSelection(currentDetectedCards, mapKey, newKey, entry, variant, card.id)
                    )
                }
            )
        }

        // Price
        Text(
            text = priceText,
            color = if (priceYen != null) colorOnSurface else colorOnSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // Quantity
        Text(
            "x${entry.quantity}",
            color = colorOnSurface,
            fontSize = 15.sp,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
        )

        // +/- buttons
        Row(modifier = Modifier.width(88.dp)) {
            IconButton(
                onClick = {
                    val updated = LinkedHashMap(currentDetectedCards)
                    updated[mapKey] = entry.copy(quantity = entry.quantity + 1)
                    onDetectedCardsChange(updated)
                    onCodeDetected(entry.code)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add ${entry.code}", tint = colorPrimary)
            }
            IconButton(
                onClick = {
                    val updated = LinkedHashMap(currentDetectedCards)
                    val newQty = entry.quantity - 1
                    if (newQty <= 0) updated.remove(mapKey) else updated[mapKey] = entry.copy(quantity = newQty)
                    onDetectedCardsChange(updated)
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Remove ${entry.code}", tint = colorError)
            }
        }
    }
}

// ─── Save confirmation dialog ─────────────────────────────────────────────────

/**
 * A confirmation dialog shown before saving scanned cards to the collection.
 *
 * Splits [entries] into saveable entries (variant known) and skipped entries
 * ([CardVariant.UNKNOWN]), and presents both groups to the user before they commit.
 *
 * - Saveable entries are listed with their code, variant label, and quantity.
 * - Unknown-variant entries are listed in red with a warning and will not be saved.
 * - If all entries are unknown, the confirm button is disabled and an error message
 *   prompts the user to assign rarities first.
 *
 * @param entries The full list of [CardEntry]s pending save.
 * @param colorError Colour used for unknown-variant warnings and the disabled state message.
 * @param colorOnSurface Base content colour for saveable entry text.
 * @param colorOnSurfaceVariant Muted colour for saveable entry detail rows.
 * @param onConfirm Called when the user confirms. Only saveable entries should be persisted;
 * filtering by variant != [CardVariant.UNKNOWN] is expected at the call site.
 * @param onDismiss Called when the dialog is dismissed or cancelled.
 */
@Composable
private fun SaveConfirmDialog(
    entries: List<CardEntry>,
    colorError: androidx.compose.ui.graphics.Color,
    colorOnSurface: androidx.compose.ui.graphics.Color,
    colorOnSurfaceVariant: androidx.compose.ui.graphics.Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val unknownEntries  = entries.filter { it.variant == CardVariant.UNKNOWN }
    val saveableEntries = entries.filter { it.variant != CardVariant.UNKNOWN }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to Collection?", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (saveableEntries.isNotEmpty()) {
                    Text(
                        "${saveableEntries.size} card${if (saveableEntries.size > 1) "s" else ""} will be added to your collection:",
                        fontSize = 13.sp,
                        color = colorOnSurface
                    )
                    saveableEntries.forEach { entry ->
                        Text(
                            "  • ${entry.code}  ${entry.variant.label}  ×${entry.quantity}",
                            fontSize = 12.sp,
                            color = colorOnSurfaceVariant
                        )
                    }
                }
                if (unknownEntries.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "⚠ ${unknownEntries.size} entr${if (unknownEntries.size > 1) "ies" else "y"} have no rarity and will be skipped:",
                        fontSize = 13.sp,
                        color = colorError,
                        fontWeight = FontWeight.SemiBold
                    )
                    unknownEntries.forEach { entry ->
                        Text(
                            "  • ${entry.code}  ×${entry.quantity}",
                            fontSize = 12.sp,
                            color = colorError
                        )
                    }
                }
                if (saveableEntries.isEmpty()) {
                    Text(
                        "No cards with a rarity assigned. Please assign rarities before saving.",
                        fontSize = 13.sp,
                        color = colorError
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = saveableEntries.isNotEmpty()) {
                Text(
                    "Save",
                    fontWeight = FontWeight.SemiBold,
                    color = if (saveableEntries.isNotEmpty())
                        androidx.compose.ui.graphics.Color.Unspecified
                    else
                        colorOnSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Screen ───────────────────────────────────────────────────────────────────

/**
 * Full-screen composable for scanning One Piece card codes using the device camera and MLKit OCR.
 *
 * The screen is split into two panels:
 * - **Top (40%):** Live camera preview with a 300×80dp targeting reticle. Tapping the preview
 *   triggers a one-shot AF action at the tapped point.
 * - **Bottom (60%):** Scrollable list of scanned [CardEntry]s with variant, price, quantity,
 *   and edit controls, plus Scan/Working, Reset, Save, and Back buttons.
 *
 * #### Scanning flow
 * Pressing "Scan" sets [buttonWorking] to true, enabling OCR processing on incoming frames.
 * Each frame is cropped via [cropCenterStrip], upscaled via [preprocessBitmap], then passed
 * to the MLKit text recognizer. On a successful match against [allCards], the entry is added
 * or its quantity incremented in [CatalogViewModel.detectedCards], [onCodeDetected] is called,
 * and scanning stops automatically. On failure the state reverts to "retry" and the next
 * frame is attempted. Pressing "Working..." while active cancels the current scan.
 *
 * #### Camera setup
 * A CameraX pipeline is configured with a 4:3 aspect ratio, RGBA_8888 image analysis,
 * and keep-only-latest backpressure. Continuous autofocus is triggered on bind via
 * [triggerContinuousAutofocus]. The camera executor is shut down on disposal.
 *
 * #### Dialogs
 * - **Reset:** Clears [CatalogViewModel.detectedCards] after confirmation.
 * - **Save:** Shows [SaveConfirmDialog]; on confirm calls [CatalogViewModel.saveDetectedCards]
 *   with the current entries, which internally skips [CardVariant.UNKNOWN] entries.
 *
 * #### Currency toggle
 * A JPY/SGD toggle in the results header switches price display for all rows.
 * SGD conversion uses a fixed rate of ¥115 = S$1.
 *
 * @param modifier Modifier applied to the root [Box].
 * @param audioManager [AudioManager] forwarded to [SfxButton]s for sound feedback.
 * @param imageLoader [ImageLoader] forwarded to [CardRow] for card thumbnail loading.
 * @param viewModel [CatalogViewModel] providing card data, detected cards state, and price fetching.
 * @param onBack Called when the Back button is tapped.
 * @param onCodeDetected Called with the card code each time a successful scan or quantity
 * increment occurs, e.g. to trigger an audio beep in the caller.
 */
@Composable
fun OnePieceCardScan(
    modifier: Modifier = Modifier,
    audioManager: AudioManager,
    imageLoader: ImageLoader,
    viewModel: CatalogViewModel,
    onBack: () -> Unit = {},
    onCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = rememberUpdatedState(LocalContext.current as androidx.lifecycle.LifecycleOwner)
    val catalogState by viewModel.state.collectAsState()
    val allCards = catalogState.allCards

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    var latestFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scanningState by remember { mutableStateOf("idle") }
    var buttonWorking by remember { mutableStateOf(false) }
    var cameraControl: CameraControl? by remember { mutableStateOf<CameraControl?>(null) }
    val detectedCards by viewModel.detectedCards.collectAsState()
    var isProcessing by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showSaveConfirm by remember { mutableStateOf(false) }
    var useSgd by remember { mutableStateOf(false) }

    val colorBackground       = MaterialTheme.colorScheme.background
    val colorOnSurface        = MaterialTheme.colorScheme.onSurface
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val colorPrimary          = MaterialTheme.colorScheme.primary
    val colorError            = MaterialTheme.colorScheme.error

    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    // ── Reset confirmation ────────────────────────────────────────────────────
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Scanned Cards?", fontWeight = FontWeight.SemiBold) },
            text = { Text("This will remove all scanned cards from the list.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearDetectedCards()
                    showResetConfirm = false
                }) {
                    Text("Yes", color = colorError, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("No") }
            }
        )
    }

    // ── Save confirmation ─────────────────────────────────────────────────────
    if (showSaveConfirm) {
        SaveConfirmDialog(
            entries = detectedCards.values.toList(),
            colorError = colorError,
            colorOnSurface = colorOnSurface,
            colorOnSurfaceVariant = colorOnSurfaceVariant,
            onConfirm = {
                showSaveConfirm = false
                viewModel.saveDetectedCards(
                    entries = detectedCards.values.toList(),
                    allCards = allCards
                )
            },
            onDismiss = { showSaveConfirm = false }
        )
    }

    // ── OCR effect ────────────────────────────────────────────────────────────
    LaunchedEffect(latestFrameBitmap) {
        val bitmap = latestFrameBitmap ?: return@LaunchedEffect
        if (!buttonWorking || isProcessing || scanningState == "done") return@LaunchedEffect

        isProcessing = true
        scanningState = "scanning"

        val inputImage = InputImage.fromBitmap(preprocessBitmap(cropCenterStrip(bitmap)), 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val code = extractCardCode(visionText.text)
                if (code != null && matchingCards(code, allCards).isNotEmpty()) {
                    val updated = LinkedHashMap(detectedCards)
                    val key = cardKey(code, CardVariant.UNKNOWN)
                    val existing = updated[key]
                    updated[key] = if (existing != null) {
                        existing.copy(quantity = existing.quantity + 1)
                    } else {
                        CardEntry(code = code, quantity = 1, variant = CardVariant.UNKNOWN , ownedQty = 0)
                    }
                    viewModel.updateDetectedCards(updated)
                    scanningState = "done"
                    buttonWorking = false
                    onCodeDetected(code)
                } else {
                    scanningState = "retry"
                }
                isProcessing = false
            }
            .addOnFailureListener { e ->
                Log.e("OCRScan", "Text recognition error", e)
                scanningState = "retry"
                isProcessing = false
            }
    }

    // ── Camera setup ──────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val analysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try { latestFrameBitmap = imageProxy.toBitmap() }
                catch (e: Exception) { Log.e("OCRScan", "Analyzer error", e) }
                finally { imageProxy.close() }
            }

            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner.value,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
            cameraControl = camera.cameraControl
            triggerContinuousAutofocus(cameraControl, previewView)

        }, ContextCompat.getMainExecutor(context))
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(modifier = modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Camera preview
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
                    .clickable {
                        cameraControl?.let { control ->
                            val action = FocusMeteringAction.Builder(
                                previewView.meteringPointFactory.createPoint(
                                    previewView.width / 2f,
                                    previewView.height / 2f
                                ),
                                FocusMeteringAction.FLAG_AF
                            ).build()
                            control.startFocusAndMetering(action)
                        }
                    }
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(80.dp)
                        .align(Alignment.Center)
                        .border(width = 3.dp, color = Color.Gray, shape = RoundedCornerShape(6.dp))
                )
            }

            // Results panel
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
                    .background(colorBackground)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (detectedCards.isEmpty()) {
                        Text(
                            "No cards scanned yet",
                            color = colorOnSurfaceVariant,
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        // Reset button
                        SfxButton(
                            audio = audioManager,
                            onClick = { showResetConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colorError),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text("Reset Scanned Cards", fontWeight = FontWeight.SemiBold)
                        }

                        // Header row with currency toggle
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Card Name", color = colorOnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("Rarity",    color = colorOnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                            // Currency toggle button inline with header
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .border(1.dp, colorOnSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .clickable { useSgd = !useSgd }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (useSgd) "SGD" else "JPY",
                                    color = colorPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text("Qty",       color = colorOnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            Text("Edit",      color = colorOnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(88.dp), textAlign = TextAlign.Center)
                        }

                        detectedCards.entries.forEach { (mapKey, entry) ->
                            CardRow(
                                mapKey = mapKey,
                                entry = entry,
                                allCards = allCards,
                                viewModel = viewModel,
                                imageLoader = imageLoader,
                                colorOnSurface = colorOnSurface,
                                colorOnSurfaceVariant = colorOnSurfaceVariant,
                                colorPrimary = colorPrimary,
                                colorError = colorError,
                                useSgd = useSgd,
                                onDetectedCardsChange = { viewModel.updateDetectedCards(it) },
                                currentDetectedCards = detectedCards,
                                onCodeDetected = onCodeDetected
                            )
                        }
                    }
                }

                // Scan / Working button
                Box(modifier = Modifier.fillMaxWidth()) {
                    SfxButton(
                        audio = audioManager,
                        onClick = {
                            if (buttonWorking) {
                                scanningState = "idle"
                                isProcessing = false
                                buttonWorking = false
                            } else {
                                buttonWorking = true
                                scanningState = "idle"
                            }
                        },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(text = if (buttonWorking) "Working..." else "Scan")
                    }
                }
            }
        }

        // Back button — bottom start
        SfxButton(
            audio = audioManager,
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }

        // Save button — bottom end, only shown when there are cards
        if (detectedCards.isNotEmpty()) {
            SfxButton(
                audio = audioManager,
                onClick = { showSaveConfirm = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.ui.components.SfxButton
import com.koi.thepiece.ui.components.SfxFAB
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// Represents the three possible card variants. Cycles in order: Normal → Star → SP → Normal.
enum class CardVariant(val label: String) {
    NORMAL(""),
    STAR("★"),
    SP("SP");

    fun next(): CardVariant = entries[(ordinal + 1) % entries.size]
}

// Holds the quantity and selected variant for a single scanned card code.
data class CardEntry(val quantity: Int, val variant: CardVariant = CardVariant.NORMAL)

@Composable
fun OnePieceCardScan(
    modifier: Modifier = Modifier,
    audioManager: AudioManager,
    onBack: () -> Unit = {},
    onCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = rememberUpdatedState(LocalContext.current as androidx.lifecycle.LifecycleOwner)

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    var latestFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // Tracks the current scan lifecycle: idle → scanning → done or retry
    var scanningState by remember { mutableStateOf("idle") }
    // Drives the button label independently of scanningState so internal retry churn
    // (scanning → retry → scanning → ...) doesn't cause the button to flicker.
    var buttonWorking by remember { mutableStateOf(false) }
    var cameraControl: CameraControl? by remember { mutableStateOf<CameraControl?>(null) }
    // Maps each detected card code to its entry (quantity + variant). Preserves insertion order.
    var detectedCards by remember { mutableStateOf(linkedMapOf<String, CardEntry>()) }
    // Guards against concurrent OCR calls when frames arrive faster than processing completes
    var isProcessing by remember { mutableStateOf(false) }

    // Snapshot theme colors once so composable lambdas below can reference them safely.
    val colorBackground = MaterialTheme.colorScheme.background
    val colorOnSurface = MaterialTheme.colorScheme.onSurface
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorError = MaterialTheme.colorScheme.error

    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    // Runs OCR on each new camera frame, but only while the user has actively triggered a scan
    // (buttonWorking == true). Skips if a recognition pass is already in progress or a code
    // has been found. Crops and upscales the frame before passing to ML Kit.
    LaunchedEffect(latestFrameBitmap) {
        val bitmap = latestFrameBitmap ?: return@LaunchedEffect
        if (!buttonWorking || isProcessing || scanningState == "done") return@LaunchedEffect

        isProcessing = true
        scanningState = "scanning"

        val croppedBitmap = cropCenterStrip(bitmap)
        val upscaledBitmap = preprocessBitmap(croppedBitmap)
        val inputImage = InputImage.fromBitmap(upscaledBitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text

                // Extracts the base card code only. Variant is always set to NORMAL on scan
                // and can be adjusted manually by tapping the variant badge in the card list.
                val codeRegex = Regex("""[A-Z][A-Z0-9][0-9O]{2}[-–][0-9O]{3}""")
                val code = codeRegex.find(rawText)?.value
                    ?.replace('–', '-')
                    ?.replace('O', '0')

                if (code != null) {
                    val updated = LinkedHashMap(detectedCards)
                    val existing = updated[code]
                    updated[code] = if (existing != null) {
                        existing.copy(quantity = existing.quantity + 1)
                    } else {
                        CardEntry(quantity = 1)
                    }
                    detectedCards = updated
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
                // Leave buttonWorking true so the button stays in "Working..." state and
                // the next frame triggers another attempt automatically.
            }
    }

    // Binds the camera preview and frame analysis use cases to the lifecycle, then starts
    // continuous autofocus so the card stays sharp in the scan area.
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

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
                cameraSelector,
                preview,
                analysis
            )
            cameraControl = camera.cameraControl
            triggerContinuousAutofocus(cameraControl, previewView)

        }, ContextCompat.getMainExecutor(context))
    }

    // Screen layout: camera preview occupies the top 40%, results and controls the bottom 60%.
    // Wrapped in a Box so the Back FAB can float at BottomStart, matching the mute button
    // position on MenuScreen for a seamless visual transition between the two screens.
    Box(modifier = modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Camera preview with a centered grey guide box showing the active scan region.
            // Tapping anywhere on the preview triggers a manual autofocus at the center point.
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

            // Results panel showing detected card codes. Scrollable to accommodate large lists.
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
                    .background(colorBackground)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card list with quantity and variant controls. Each row shows the base code,
                // a tappable variant badge that cycles Normal → ★ → SP → Normal, the quantity,
                // and + / - buttons. Removing the last copy of a card removes the row entirely.
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    if (detectedCards.isEmpty()) {
                        Text(
                            "No cards scanned yet",
                            color = colorOnSurfaceVariant,
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        // Header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Card Name",
                                color = colorOnSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Variant",
                                color = colorOnSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(56.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Qty",
                                color = colorOnSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(40.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Edit",
                                color = colorOnSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(88.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        detectedCards.entries.forEach { (code, entry) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Base card code
                                Text(
                                    code,
                                    color = colorOnSurface,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                // Tappable variant badge — cycles Normal (blank) → ★ → SP on each tap.
                                // Outlined box makes it clear the field is interactive.
                                Box(
                                    modifier = Modifier
                                        .width(56.dp)
                                        .border(
                                            width = 1.dp,
                                            color = colorOnSurfaceVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            val updated = LinkedHashMap(detectedCards)
                                            updated[code] = entry.copy(variant = entry.variant.next())
                                            detectedCards = updated
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = entry.variant.label.ifEmpty { "—" },
                                        color = if (entry.variant == CardVariant.NORMAL)
                                            colorOnSurfaceVariant
                                        else
                                            colorPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Quantity
                                Text(
                                    "x${entry.quantity}",
                                    color = colorOnSurface,
                                    fontSize = 15.sp,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.Center
                                )

                                // + and - quantity controls
                                Row(modifier = Modifier.width(88.dp)) {
                                    IconButton(
                                        onClick = {
                                            val updated = LinkedHashMap(detectedCards)
                                            updated[code] = entry.copy(quantity = entry.quantity + 1)
                                            detectedCards = updated
                                            onCodeDetected(code)
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add $code", tint = colorPrimary)
                                    }
                                    IconButton(
                                        onClick = {
                                            val updated = LinkedHashMap(detectedCards)
                                            val newQty = entry.quantity - 1
                                            if (newQty <= 0) updated.remove(code) else updated[code] = entry.copy(quantity = newQty)
                                            detectedCards = updated
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Remove $code", tint = colorError)
                                    }
                                }
                            }
                        }
                    }
                }

                // Scan button pinned to the bottom center of the results panel.
                // "Scan" — waiting for user to trigger.
                // "Working..." — actively cycling through frames until a code is found.
                // Pressing while working cancels the active scan.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
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

        // Back FAB floats at BottomStart, mirroring the mute button position on MenuScreen
        // so the button appears in the same spot during navigation between the two screens.
        SfxFAB(
            audio = audioManager,
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text("Back", fontSize = 14.sp)
        }
    }
}

// Crops the center horizontal strip of a camera frame to match the grey guide box region.
// Uses percentage-based coordinates (80% wide, 30% tall, vertically centered) and clamps
// values to prevent out-of-bounds crashes on unusual frame dimensions.
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

// Scales the cropped bitmap up by 2.5× before passing it to ML Kit. Camera frames contain
// small text that sits below ML Kit's reliable recognition threshold at native resolution.
fun preprocessBitmap(src: Bitmap): Bitmap {
    val scale = 2.5f
    val matrix = Matrix().apply { postScale(scale, scale) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}

// Starts autofocus and autoexposure metering locked to the center of the preview.
// The auto-cancel duration causes the camera to re-evaluate focus every 3 seconds,
// keeping the scan area sharp as the card is repositioned.
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

// Converts a CameraX ImageProxy (RGBA_8888 format) to a correctly oriented Bitmap
// by applying the frame's reported rotation before returning.
fun ImageProxy.toBitmap(): Bitmap {
    val buffer: ByteBuffer = planes[0].buffer
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(buffer)
    val matrix = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
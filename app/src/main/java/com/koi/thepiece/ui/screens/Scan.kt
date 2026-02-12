@file:Suppress("UnsafeOptInUsageError")

package com.koi.thepiece.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.koi.thepiece.scanner.camera.BoundingBox
import com.koi.thepiece.scanner.detector.Detector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import com.koi.thepiece.scanner.detector.Constants.LABELS_PATH
import com.koi.thepiece.scanner.detector.Constants.MODEL_PATH

/**
 * Screen: Scan
 * - Camera preview (PreviewView)
 * - Runs your existing Detector.detect(rotatedBitmap)
 * - Draws BoundingBox overlay aligned with PreviewView center-crop (FILL_CENTER)
 *
 * Requirements:
 * - model + labels are in assets, paths passed in
 * - BoundingBox is normalized (0..1) x1,y1,x2,y2 relative to the bitmap passed into detector.detect()
 */
@Composable
fun Scan(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // PreviewView (for CameraX)
    val previewView = remember {
        PreviewView(context).apply {
            // IMPORTANT: our overlay mapping assumes center-crop
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Analyzer executor (camera thread)
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Latest detection boxes
    val boxesState = remember { mutableStateOf<List<BoundingBox>>(emptyList()) }

    // Size of the EXACT bitmap you feed to detector.detect(...)
    // We need this to map normalized boxes -> preview coordinates correctly.
    val frameSizeState = remember { mutableStateOf(0 to 0) }

    // Detector listener
    val listener = remember {
        object : Detector.DetectorListener {
            override fun onEmptyDetect() {
                boxesState.value = emptyList()
            }

            override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
                boxesState.value = boundingBoxes
            }
        }
    }

    // Detector instance (lifetime = this screen)
    val detector = remember {
        Detector(
            context = context.applicationContext,
            MODEL_PATH,
            LABELS_PATH,
            detectorListener = listener
        )
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            try { detector.close() } catch (_: Throwable) {}
            cameraExecutor.shutdown()
        }
    }

    // Bind camera once
    LaunchedEffect(Unit) {
        bindCamera(
            previewView = previewView,
            lifecycleOwner = lifecycleOwner,
            cameraExecutor = cameraExecutor,
            isFrontCamera = isFrontCamera,
            onBitmapReady = { rotatedBitmap ->
                frameSizeState.value = rotatedBitmap.width to rotatedBitmap.height
                detector.detect(rotatedBitmap)
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )

        val (frameW, frameH) = frameSizeState.value
        DetectionOverlay(
            modifier = Modifier.fillMaxSize(),
            previewView = previewView,
            boxes = boxesState.value,
            frameW = frameW,
            frameH = frameH
        )
    }
}

/**
 * CameraX binding:
 * - AspectRatio 4:3 (same as your sample)
 * - RGBA_8888 output for fast bitmap copy
 */
private fun bindCamera(
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraExecutor: ExecutorService,
    isFrontCamera: Boolean,
    onBitmapReady: (Bitmap) -> Unit
) {
    val context = previewView.context
    val providerFuture = ProcessCameraProvider.getInstance(context)

    providerFuture.addListener({
        val cameraProvider = providerFuture.get()

        val rotation = previewView.display.rotation

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(
                if (isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            )
            .build()

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        val analysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
            try {
                // RGBA_8888: planes[0] contains full buffer
                val bitmapBuffer = Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
                bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)

                val m = Matrix().apply {
                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                    if (isFrontCamera) {
                        // Mirror horizontally for front camera
                        postScale(
                            -1f, 1f,
                            imageProxy.width.toFloat(),
                            imageProxy.height.toFloat()
                        )
                    }
                }

                val rotatedBitmap = Bitmap.createBitmap(
                    bitmapBuffer,
                    0, 0,
                    bitmapBuffer.width,
                    bitmapBuffer.height,
                    m,
                    true
                )

                onBitmapReady(rotatedBitmap)
            } catch (e: Exception) {
                Log.e("Scan", "Analyzer error", e)
            } finally {
                imageProxy.close()
            }
        }

        cameraProvider.unbindAll()
        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                analysis
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: Exception) {
            Log.e("Scan", "Use case binding failed", e)
        }

    }, ContextCompat.getMainExecutor(context))
}

/**
 * Overlay mapping (IMPORTANT):
 * BoundingBox coords are normalized (0..1) relative to the *rotatedBitmap* passed to detector.detect().
 *
 * PreviewView uses center-crop when scaleType = FILL_CENTER.
 * So we apply the same center-crop transform when drawing:
 *   view = image * scale + offset
 */
@Composable
private fun DetectionOverlay(
    modifier: Modifier,
    previewView: PreviewView,
    boxes: List<BoundingBox>,
    frameW: Int,
    frameH: Int
) {
    Canvas(modifier = modifier) {
        if (frameW <= 0 || frameH <= 0) return@Canvas
        if (boxes.isEmpty()) return@Canvas

        val viewW = size.width
        val viewH = size.height

        // FILL_CENTER => center-crop => use max scale
        val scale = max(viewW / frameW.toFloat(), viewH / frameH.toFloat())
        val dx = (viewW - frameW * scale) / 2f
        val dy = (viewH - frameH * scale) / 2f

        val stroke = 4.dp.toPx()

        boxes.forEach { b ->
            // normalized -> image pixels
            val lImg = b.x1 * frameW
            val tImg = b.y1 * frameH
            val rImg = b.x2 * frameW
            val bImg = b.y2 * frameH

            // image pixels -> view pixels (center-crop)
            val left = lImg * scale + dx
            val top = tImg * scale + dy
            val right = rImg * scale + dx
            val bottom = bImg * scale + dy

            val w = (right - left).coerceAtLeast(0f)
            val h = (bottom - top).coerceAtLeast(0f)

            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = Size(w, h),
                style = Stroke(width = stroke)
            )
        }
    }
}

package com.koi.thepiece.ui.screens.catalogscreen.components

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.koi.thepiece.AppGraph.provideCatalogRepository
import com.koi.thepiece.data.model.Card
import com.koi.thepiece.data.repo.CatalogRepository

@Composable
fun CardPreviewDialog(
    card: Card,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
    onPlus: () -> Unit,
    onMinus: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text(card.code ?: "Card") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .transformable(state)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )



                Spacer(Modifier.height(10.dp))
                Text("Code: ${card.code ?: "-"}")
                Text("Color: ${card.color}   Type: ${card.type}")
                if (!card.cardSet.isNullOrBlank()) Text("Set: ${card.cardSet}")
                if (!card.rarity.isNullOrBlank()) Text("Rarity: ${card.rarity}")
                if (!card.traits.isNullOrBlank()) Text("Traits: ${card.traits}")
                if (!card.obtainFrom.isNullOrBlank()) Text("Obtain: ${card.obtainFrom}")

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onMinus) { Text("-") }
                    Text("Owned: x${card.ownedQty}", modifier = Modifier.padding(top = 10.dp))
                    OutlinedButton(onClick = onPlus) { Text("+") }
                }
            }
        }
    )
}
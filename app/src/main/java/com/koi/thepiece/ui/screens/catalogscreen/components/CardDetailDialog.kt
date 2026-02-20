package com.koi.thepiece.ui.screens.catalogscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import org.intellij.lang.annotations.JdkConstants

@Composable
fun CardPreviewDialog(
    card: Card,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    var isEnglish by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Lang
                Row(
                    modifier = Modifier
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                ) {

                    // JP
                    Box(
                        modifier = Modifier
                            .background(
                                if (!isEnglish)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Transparent
                            )
                            .clickable { isEnglish = false }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JP",
                            color = if (!isEnglish)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // EN
                    Box(
                        modifier = Modifier
                            .background(
                                if (isEnglish)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Transparent
                            )
                            .clickable { isEnglish = true }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EN",
                            color = if (isEnglish)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))

                // Close
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(
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
        title = {
            Column {
                // Display Code if it is not DON card
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
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Image
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

                // Skills
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Skill: ", style = MaterialTheme.typography.bodyMedium)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 25.dp, max = 75.dp)
                            .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (isEnglish) {
                                    card.skillEn ?: "-"
                                } else {
                                    card.skillJp ?: "-"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Other Properties
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
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CreateOutlinedButton(text = "-",onMinus)
                    Text("Owned: ${card.ownedQty}", style = MaterialTheme.typography.bodyMedium)
                    CreateOutlinedButton(text = "+", onPlus)
                }
            }
        }
    )
}

@Composable
private fun CreateOutlinedButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp, 28.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
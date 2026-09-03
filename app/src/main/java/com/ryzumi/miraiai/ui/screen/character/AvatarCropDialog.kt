package com.ryzumi.miraiai.ui.screen.character

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ryzumi.miraiai.domain.util.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun AvatarCropDialog(
    rawImageUri: String,
    onDismiss: () -> Unit,
    onCropSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    var imageWidth by remember { mutableFloatStateOf(0f) }
    var imageHeight by remember { mutableFloatStateOf(0f) }
    var isProcessing by remember { mutableStateOf(false) }

    val cropBoxPx = if (viewportWidthPx > 0f && viewportHeightPx > 0f) {
        minOf(viewportWidthPx, viewportHeightPx) * 0.78f
    } else 240f

    val minScale = remember(imageWidth, imageHeight, viewportWidthPx, viewportHeightPx, cropBoxPx) {
        if (imageWidth > 0f && imageHeight > 0f && viewportWidthPx > 0f && viewportHeightPx > 0f) {
            val fitScale = minOf(viewportWidthPx / imageWidth, viewportHeightPx / imageHeight)
            val w0 = imageWidth * fitScale
            val h0 = imageHeight * fitScale
            maxOf(cropBoxPx / w0, cropBoxPx / h0)
        } else 1f
    }

    androidx.compose.runtime.LaunchedEffect(minScale) {
        scale = minScale
        panX = 0f
        panY = 0f
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isProcessing,
            dismissOnClickOutside = !isProcessing
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crop Avatar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            scale = minScale
                            panX = 0f
                            panY = 0f
                        },
                        enabled = !isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Framing"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Viewport with natural full image display & 1:1 Crop Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .onGloballyPositioned { coordinates ->
                            viewportWidthPx = coordinates.size.width.toFloat()
                            viewportHeightPx = coordinates.size.height.toFloat()
                        }
                        .pointerInput(imageWidth, imageHeight, viewportWidthPx, viewportHeightPx, minScale, cropBoxPx) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (imageWidth > 0f && imageHeight > 0f && viewportWidthPx > 0f && viewportHeightPx > 0f) {
                                    val fitScale = minOf(viewportWidthPx / imageWidth, viewportHeightPx / imageHeight)
                                    val w0 = imageWidth * fitScale
                                    val h0 = imageHeight * fitScale

                                    scale = (scale * zoom).coerceIn(minScale, minScale * 5f)
                                    val wCurr = w0 * scale
                                    val hCurr = h0 * scale

                                    val maxPanX = maxOf(0f, (wCurr - cropBoxPx) / 2f)
                                    val maxPanY = maxOf(0f, (hCurr - cropBoxPx) / 2f)

                                    panX = (panX + pan.x).coerceIn(-maxPanX, maxPanX)
                                    panY = (panY + pan.y).coerceIn(-maxPanY, maxPanY)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 1. Natural un-cropped image
                    AsyncImage(
                        model = rawImageUri,
                        contentDescription = "Avatar Preview",
                        onSuccess = { state ->
                            val drawable = state.result.drawable
                            imageWidth = drawable.intrinsicWidth.toFloat()
                            imageHeight = drawable.intrinsicHeight.toFloat()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = panX,
                                translationY = panY
                            ),
                        contentScale = ContentScale.Fit
                    )

                    // 2. 1:1 Square Crop overlay with semi-transparent scrim outside
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cropBoxSize = minOf(w, h) * 0.78f
                        val cropLeft = (w - cropBoxSize) / 2f
                        val cropTop = (h - cropBoxSize) / 2f
                        val cropRight = cropLeft + cropBoxSize
                        val cropBottom = cropTop + cropBoxSize

                        // Dark scrim outside the crop box
                        drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, 0f), Size(w, cropTop))
                        drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, cropBottom), Size(w, h - cropBottom))
                        drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, cropTop), Size(cropLeft, cropBoxSize))
                        drawRect(Color.Black.copy(alpha = 0.55f), Offset(cropRight, cropTop), Size(w - cropRight, cropBoxSize))

                        // 1:1 Square border
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(cropLeft, cropTop),
                            size = Size(cropBoxSize, cropBoxSize),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 1:1 Circular guide
                        drawCircle(
                            color = Color.White.copy(alpha = 0.7f),
                            center = Offset(w / 2f, h / 2f),
                            radius = cropBoxSize / 2f,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                            )
                        )
                    }

                    // Processing scrim & spinner
                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Geser & cubit untuk menyesuaikan posisi foto",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isProcessing
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    Button(
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                val cropBoxPx = if (viewportWidthPx > 0f && viewportHeightPx > 0f) {
                                    minOf(viewportWidthPx, viewportHeightPx) * 0.78f
                                } else 240f

                                val cropped = ImageUtils.cropAndSaveAvatarFromFitWindow(
                                    context = context,
                                    imageSource = rawImageUri,
                                    panX = panX,
                                    panY = panY,
                                    zoomScale = scale,
                                    viewportW = if (viewportWidthPx > 0f) viewportWidthPx else 320f,
                                    viewportH = if (viewportHeightPx > 0f) viewportHeightPx else 320f,
                                    cropBoxSize = cropBoxPx,
                                    targetDimension = 720,
                                    quality = 85
                                ) ?: ImageUtils.cropAndSaveAvatar(
                                    context = context,
                                    imageSource = rawImageUri,
                                    targetDimension = 720,
                                    quality = 85
                                )

                                isProcessing = false
                                if (cropped != null) {
                                    onCropSuccess(cropped)
                                } else {
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Crop")
                    }
                }
            }
        }
    }
}

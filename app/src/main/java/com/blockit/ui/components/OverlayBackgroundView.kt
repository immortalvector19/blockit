package com.blockit.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.blockit.domain.model.OverlayBackgroundType

@Composable
fun OverlayBackgroundView(
    backgroundType: OverlayBackgroundType,
    bitmaps: List<ImageBitmap?>,
    modifier: Modifier = Modifier,
    scrimAlphaTop: Float = 0.65f,
    scrimAlphaBottom: Float = 0.75f,
    showScrim: Boolean = true
) {
    Box(modifier = modifier) {
        when (backgroundType) {
            OverlayBackgroundType.SINGLE_IMAGE -> {
                val singleBitmap = bitmaps.firstOrNull { it != null }
                if (singleBitmap != null) {
                    Image(
                        bitmap = singleBitmap,
                        contentDescription = "Overlay Background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF16181C))
                    )
                }
            }
            OverlayBackgroundType.COLLAGE_4 -> {
                CollageQuadLayout(
                    bitmaps = bitmaps,
                    modifier = Modifier.fillMaxSize()
                )
            }
            OverlayBackgroundType.DEFAULT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121316))
                )
            }
        }

        // Dark gradient scrim for WCAG AAA contrast
        if (showScrim && backgroundType != OverlayBackgroundType.DEFAULT && bitmaps.any { it != null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = scrimAlphaTop),
                                Color.Black.copy(alpha = 0.50f),
                                Color.Black.copy(alpha = scrimAlphaBottom)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun CollageQuadLayout(
    bitmaps: List<ImageBitmap?>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                bitmaps.getOrNull(0)?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Collage photo 1",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E2024)))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                bitmaps.getOrNull(1)?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Collage photo 2",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(modifier = Modifier.fillMaxSize().background(Color(0xFF26292E)))
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                bitmaps.getOrNull(2)?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Collage photo 3",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(modifier = Modifier.fillMaxSize().background(Color(0xFF26292E)))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                bitmaps.getOrNull(3)?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Collage photo 4",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E2024)))
            }
        }
    }
}

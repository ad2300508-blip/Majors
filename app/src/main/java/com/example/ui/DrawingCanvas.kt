package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DrawingStroke
import com.example.data.StylusPoint
import kotlin.math.sqrt

@Composable
fun DrawingCanvas(
    strokes: List<DrawingStroke>,
    selectedColor: Color,
    selectedWidth: Float,
    onStrokesChanged: (List<DrawingStroke>) -> Unit,
    modifier: Modifier = Modifier,
    isEraser: Boolean = false,
    backgroundStyle: String = "blank"
) {
    val livePoints = remember { mutableStateListOf<StylusPoint>() }

    // Viewport scaling and translation panning track
    var scale by remember { mutableStateOf(1.0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isPanMode by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(Color.White) // High contrast drawing slate
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isEraser, selectedColor, selectedWidth, isPanMode, scale, offsetX, offsetY) {
                    if (isPanMode) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5.0f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = { offset ->
                                livePoints.clear()
                                val worldX = (offset.x - offsetX) / scale
                                val worldY = (offset.y - offsetY) / scale
                                livePoints.add(StylusPoint(worldX, worldY, 1.0f))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val offset = change.position
                                val worldX = (offset.x - offsetX) / scale
                                val worldY = (offset.y - offsetY) / scale
                                
                                var pressure = change.pressure
                                if (pressure == 1.0f || pressure == 0f) {
                                    val lastPoint = livePoints.lastOrNull()
                                    if (lastPoint != null) {
                                        val dx = worldX - lastPoint.x
                                        val dy = worldY - lastPoint.y
                                        val dist = sqrt(dx * dx + dy * dy)
                                        val maxDist = 30f
                                        val speedFactor = (dist / maxDist).coerceIn(0f, 1f)
                                        val targetPressure = 1.3f - (speedFactor * 0.8f) // faster speed -> thinner stroke
                                        pressure = lastPoint.pressure * 0.7f + targetPressure * 0.3f
                                    }
                                }
                                livePoints.add(StylusPoint(worldX, worldY, pressure))
                            },
                            onDragEnd = {
                                if (livePoints.isNotEmpty()) {
                                    if (isEraser) {
                                        val eraserRadius = 30.0f / scale
                                        val updated = strokes.filter { stroke ->
                                            stroke.points.none { pt ->
                                                livePoints.any { lpt ->
                                                    val dx = pt.x - lpt.x
                                                    val dy = pt.y - lpt.y
                                                    (dx * dx + dy * dy) < (eraserRadius * eraserRadius)
                                                }
                                            }
                                        }
                                        onStrokesChanged(updated)
                                    } else {
                                        val strokeColor = selectedColor.value.toLong().toInt()
                                        val stroke = DrawingStroke(
                                            points = livePoints.toList(),
                                            color = strokeColor,
                                            width = selectedWidth
                                        )
                                        onStrokesChanged(strokes + stroke)
                                    }
                                }
                                livePoints.clear()
                            }
                        )
                    }
                }
        ) {
            // Apply scale and panning values onto drawing space
            withTransform({
                translate(offsetX, offsetY)
                scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset.Zero)
            }) {
                // Guiding backgrounds scaled up dynamically to a massive visual sheet bounding box
                when (backgroundStyle) {
                    "ruled" -> {
                        val spacing = 28.dp.toPx()
                        val startY = -4000f
                        val endY = 8000f
                        val startX = -4000f
                        val endX = 8000f
                        
                        var y = (startY / spacing).toInt() * spacing
                        while (y < endY) {
                            drawLine(
                                color = Color(0xFF03A9F4).copy(alpha = 0.15f), // Classic notebook blue lines
                                start = androidx.compose.ui.geometry.Offset(startX, y),
                                end = androidx.compose.ui.geometry.Offset(endX, y),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            y += spacing
                        }
                        
                        // Notebook red margin indicator line
                        val marginX = 64.dp.toPx()
                        drawLine(
                            color = Color(0xFFE91E63).copy(alpha = 0.2f),
                            start = androidx.compose.ui.geometry.Offset(marginX, startY),
                            end = androidx.compose.ui.geometry.Offset(marginX, endY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    "grid" -> {
                        val spacing = 22.dp.toPx()
                        val startY = -4000f
                        val endY = 8000f
                        val startX = -4000f
                        val endX = 8000f
                        
                        var y = (startY / spacing).toInt() * spacing
                        while (y < endY) {
                            drawLine(
                                color = Color(0xFF9E9E9E).copy(alpha = 0.12f),
                                start = androidx.compose.ui.geometry.Offset(startX, y),
                                end = androidx.compose.ui.geometry.Offset(endX, y),
                                strokeWidth = 1.dp.toPx()
                            )
                            y += spacing
                        }
                        
                        var x = (startX / spacing).toInt() * spacing
                        while (x < endX) {
                            drawLine(
                                color = Color(0xFF9E9E9E).copy(alpha = 0.12f),
                                start = androidx.compose.ui.geometry.Offset(x, startY),
                                end = androidx.compose.ui.geometry.Offset(x, endY),
                                strokeWidth = 1.dp.toPx()
                            )
                            x += spacing
                        }
                    }
                    else -> { /* Blank canvas slate */ }
                }

                // Render vector strokes applying styling brush rules
                strokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val colorVal = Color(stroke.color)
                        val isHighlighter = colorVal.alpha < 0.9f

                        if (isHighlighter) {
                            // Highlighters are painted with uniform alpha via smoothed Bezier path strokes
                            val path = Path().apply {
                                val first = stroke.points.first()
                                moveTo(first.x, first.y)
                                var prevPt = first
                                for (i in 1 until stroke.points.size) {
                                    val pt = stroke.points[i]
                                    val midX = (prevPt.x + pt.x) / 2f
                                    val midY = (prevPt.y + pt.y) / 2f
                                    if (i == 1) {
                                        lineTo(midX, midY)
                                    } else {
                                        quadraticTo(prevPt.x, prevPt.y, midX, midY)
                                    }
                                    prevPt = pt
                                }
                                lineTo(prevPt.x, prevPt.y)
                            }
                            drawPath(
                                path = path,
                                color = colorVal,
                                style = Stroke(
                                    width = stroke.width,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        } else {
                            // Ink fountain pens and graphite draw seamless velocity-tapered segment lines
                            for (i in 0 until stroke.points.size - 1) {
                                val p1 = stroke.points[i]
                                val p2 = stroke.points[i + 1]
                                val segWidth = stroke.width * ((p1.pressure + p2.pressure) / 2f)
                                drawLine(
                                    color = colorVal,
                                    start = androidx.compose.ui.geometry.Offset(p1.x, p1.y),
                                    end = androidx.compose.ui.geometry.Offset(p2.x, p2.y),
                                    strokeWidth = segWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }

                // Render current active interactive stroke segments
                if (livePoints.size > 1) {
                    val colorVal = if (isEraser) Color.Gray.copy(alpha = 0.4f) else selectedColor
                    val isHighlighter = colorVal.alpha < 0.9f && !isEraser
                    
                    if (isHighlighter) {
                        val livePath = Path().apply {
                            val first = livePoints.first()
                            moveTo(first.x, first.y)
                            var prevPt = first
                            for (i in 1 until livePoints.size) {
                                val pt = livePoints[i]
                                val midX = (prevPt.x + pt.x) / 2f
                                val midY = (prevPt.y + pt.y) / 2f
                                if (i == 1) {
                                    lineTo(midX, midY)
                                } else {
                                    quadraticTo(prevPt.x, prevPt.y, midX, midY)
                                }
                                prevPt = pt
                            }
                            lineTo(prevPt.x, prevPt.y)
                        }
                        drawPath(
                            path = livePath,
                            color = colorVal,
                            style = Stroke(
                                width = selectedWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    } else {
                        val activeWidth = if (isEraser) 30.0f / scale else selectedWidth
                        for (i in 0 until livePoints.size - 1) {
                            val p1 = livePoints[i]
                            val p2 = livePoints[i + 1]
                            val segWidth = activeWidth * (if (isEraser) 1.2f else ((p1.pressure + p2.pressure) / 2f))
                            drawLine(
                                color = colorVal,
                                start = androidx.compose.ui.geometry.Offset(p1.x, p1.y),
                                end = androidx.compose.ui.geometry.Offset(p2.x, p2.y),
                                strokeWidth = segWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }

        // Floating zoom/pan controls widget
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Toggle mode between Free-draw vs Pan/Zoom Viewport drag navigation
                IconButton(
                    onClick = { isPanMode = !isPanMode },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isPanMode) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isPanMode) Icons.Default.ZoomOutMap else Icons.Default.Gesture,
                        contentDescription = "Toggle Pan/Zoom navigation mode",
                        tint = if (isPanMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(20.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Zoom Out Button
                IconButton(
                    onClick = { scale = (scale - 0.2f).coerceIn(0.5f, 5.0f) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Current Zoom Percent status label
                Text(
                    text = "${(scale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Zoom In Button
                IconButton(
                    onClick = { scale = (scale + 0.2f).coerceIn(0.5f, 5.0f) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(20.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Reset Canvas position to central home
                IconButton(
                    onClick = {
                        scale = 1.0f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Reset viewport",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Utility to convert vector drawing strokes to an ARGB_8888 Bitmap for sending to Gemini Vision API.
 * Updated to fully mimic the speed-tapered pressure smoothing of the hardware view layers.
 */
fun strokesToBitmap(strokes: List<DrawingStroke>, width: Int, height: Int): Bitmap {
    val validWidth = width.coerceAtLeast(400)
    val validHeight = height.coerceAtLeast(400)
    val bitmap = Bitmap.createBitmap(validWidth, validHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Fill canvas backdrop white
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        isDither = true
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        style = android.graphics.Paint.Style.STROKE
    }

    strokes.forEach { stroke ->
        if (stroke.points.size > 1) {
            val colorVal = Color(stroke.color)
            val isHighlighter = colorVal.alpha < 0.9f
            
            if (isHighlighter) {
                paint.color = stroke.color
                paint.strokeWidth = stroke.width
                paint.alpha = (colorVal.alpha * 255).toInt()
                val path = android.graphics.Path()
                val first = stroke.points.first()
                path.moveTo(first.x, first.y)
                var prevPt = first
                for (i in 1 until stroke.points.size) {
                    val pt = stroke.points[i]
                    val midX = (prevPt.x + pt.x) / 2f
                    val midY = (prevPt.y + pt.y) / 2f
                    if (i == 1) {
                        path.lineTo(midX, midY)
                    } else {
                        path.quadTo(prevPt.x, prevPt.y, midX, midY)
                    }
                    prevPt = pt
                }
                path.lineTo(prevPt.x, prevPt.y)
                canvas.drawPath(path, paint)
            } else {
                paint.color = stroke.color
                paint.alpha = 255
                for (i in 0 until stroke.points.size - 1) {
                    val p1 = stroke.points[i]
                    val p2 = stroke.points[i+1]
                    paint.strokeWidth = stroke.width * ((p1.pressure + p2.pressure) / 2f)
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
                }
            }
        }
    }

    return bitmap
}

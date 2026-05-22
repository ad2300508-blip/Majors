package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
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

    var scale by remember { mutableStateOf(1.0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isPanMode by remember { mutableStateOf(false) }

    val currentStrokes by rememberUpdatedState(strokes)
    val currentSelectedColor by rememberUpdatedState(selectedColor)
    val currentSelectedWidth by rememberUpdatedState(selectedWidth)
    val currentIsEraser by rememberUpdatedState(isEraser)

    // Slightly warm off-white paper background
    val paperColor = Color(0xFFFBFBFF)

    Box(modifier = modifier.background(paperColor)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isPanMode, scale, offsetX, offsetY) {
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
                                        val speedFactor = (dist / 30f).coerceIn(0f, 1f)
                                        val targetPressure = 1.3f - (speedFactor * 0.8f)
                                        pressure = lastPoint.pressure * 0.7f + targetPressure * 0.3f
                                    }
                                }
                                livePoints.add(StylusPoint(worldX, worldY, pressure))
                            },
                            onDragEnd = {
                                if (livePoints.isNotEmpty()) {
                                    if (currentIsEraser) {
                                        val eraserRadius = 30.0f / scale
                                        val updated = currentStrokes.filter { stroke ->
                                            stroke.points.none { pt ->
                                                livePoints.any { lpt ->
                                                    val dx = pt.x - lpt.x
                                                    val dy = pt.y - lpt.y
                                                    (dx * dx + dy * dy) < (eraserRadius * eraserRadius)
                                                }
                                            }
                                        }
                                        onStrokesChanged(updated)
                                        livePoints.clear()
                                    } else {
                                        val stroke = DrawingStroke(
                                            points = livePoints.toList(),
                                            color = currentSelectedColor.toArgb(),
                                            width = currentSelectedWidth
                                        )
                                        onStrokesChanged(currentStrokes + stroke)
                                        livePoints.clear()
                                    }
                                }
                            },
                            onDragCancel = {
                                if (livePoints.isNotEmpty() && !currentIsEraser) {
                                    val stroke = DrawingStroke(
                                        points = livePoints.toList(),
                                        color = currentSelectedColor.toArgb(),
                                        width = currentSelectedWidth
                                    )
                                    onStrokesChanged(currentStrokes + stroke)
                                }
                                livePoints.clear()
                            }
                        )
                    }
                }
        ) {
            withTransform({
                translate(offsetX, offsetY)
                scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset.Zero)
            }) {
                val bgRange = -4000f..8000f
                when (backgroundStyle) {
                    "ruled" -> {
                        val spacing = 28.dp.toPx()
                        var y = (bgRange.start / spacing).toInt() * spacing
                        while (y < bgRange.endInclusive) {
                            drawLine(
                                color = Color(0xFF90CAF9).copy(alpha = 0.30f),
                                start = androidx.compose.ui.geometry.Offset(bgRange.start, y),
                                end = androidx.compose.ui.geometry.Offset(bgRange.endInclusive, y),
                                strokeWidth = 1.2.dp.toPx()
                            )
                            y += spacing
                        }
                        // Red margin line
                        drawLine(
                            color = Color(0xFFEF9A9A).copy(alpha = 0.35f),
                            start = androidx.compose.ui.geometry.Offset(60.dp.toPx(), bgRange.start),
                            end = androidx.compose.ui.geometry.Offset(60.dp.toPx(), bgRange.endInclusive),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                    "grid" -> {
                        val spacing = 24.dp.toPx()
                        var y = (bgRange.start / spacing).toInt() * spacing
                        while (y < bgRange.endInclusive) {
                            drawLine(
                                color = Color(0xFF9E9E9E).copy(alpha = 0.15f),
                                start = androidx.compose.ui.geometry.Offset(bgRange.start, y),
                                end = androidx.compose.ui.geometry.Offset(bgRange.endInclusive, y),
                                strokeWidth = 0.8.dp.toPx()
                            )
                            y += spacing
                        }
                        var x = (bgRange.start / spacing).toInt() * spacing
                        while (x < bgRange.endInclusive) {
                            drawLine(
                                color = Color(0xFF9E9E9E).copy(alpha = 0.15f),
                                start = androidx.compose.ui.geometry.Offset(x, bgRange.start),
                                end = androidx.compose.ui.geometry.Offset(x, bgRange.endInclusive),
                                strokeWidth = 0.8.dp.toPx()
                            )
                            x += spacing
                        }
                    }
                    "dotted" -> {
                        val spacing = 24.dp.toPx()
                        val dotRadius = 1.2.dp.toPx()
                        var y = (bgRange.start / spacing).toInt() * spacing
                        while (y < bgRange.endInclusive) {
                            var x = (bgRange.start / spacing).toInt() * spacing
                            while (x < bgRange.endInclusive) {
                                drawCircle(
                                    color = Color(0xFF9E9E9E).copy(alpha = 0.35f),
                                    radius = dotRadius,
                                    center = androidx.compose.ui.geometry.Offset(x, y)
                                )
                                x += spacing
                            }
                            y += spacing
                        }
                    }
                    else -> { /* blank */ }
                }

                // Committed strokes
                strokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val colorVal = Color(stroke.color)
                        val isHighlighter = colorVal.alpha < 0.9f

                        if (isHighlighter) {
                            val path = Path().apply {
                                val first = stroke.points.first()
                                moveTo(first.x, first.y)
                                var prev = first
                                for (i in 1 until stroke.points.size) {
                                    val pt = stroke.points[i]
                                    val midX = (prev.x + pt.x) / 2f
                                    val midY = (prev.y + pt.y) / 2f
                                    if (i == 1) lineTo(midX, midY) else quadraticTo(prev.x, prev.y, midX, midY)
                                    prev = pt
                                }
                                lineTo(stroke.points.last().x, stroke.points.last().y)
                            }
                            drawPath(path, colorVal, style = Stroke(stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        } else {
                            for (i in 0 until stroke.points.size - 1) {
                                val p1 = stroke.points[i]
                                val p2 = stroke.points[i + 1]
                                drawLine(
                                    colorVal,
                                    androidx.compose.ui.geometry.Offset(p1.x, p1.y),
                                    androidx.compose.ui.geometry.Offset(p2.x, p2.y),
                                    stroke.width * ((p1.pressure + p2.pressure) / 2f),
                                    StrokeCap.Round
                                )
                            }
                        }
                    }
                }

                // Live stroke being drawn
                if (livePoints.size > 1) {
                    val colorVal = if (isEraser) Color(0xFFBBBBBB).copy(alpha = 0.5f) else selectedColor
                    val isHighlighter = colorVal.alpha < 0.9f && !isEraser

                    if (isHighlighter) {
                        val livePath = Path().apply {
                            val first = livePoints.first()
                            moveTo(first.x, first.y)
                            var prev = first
                            for (i in 1 until livePoints.size) {
                                val pt = livePoints[i]
                                val midX = (prev.x + pt.x) / 2f
                                val midY = (prev.y + pt.y) / 2f
                                if (i == 1) lineTo(midX, midY) else quadraticTo(prev.x, prev.y, midX, midY)
                                prev = pt
                            }
                            lineTo(livePoints.last().x, livePoints.last().y)
                        }
                        drawPath(livePath, colorVal, style = Stroke(selectedWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    } else {
                        val activeWidth = if (isEraser) 30.0f / scale else selectedWidth
                        for (i in 0 until livePoints.size - 1) {
                            val p1 = livePoints[i]
                            val p2 = livePoints[i + 1]
                            val w = activeWidth * (if (isEraser) 1.2f else ((p1.pressure + p2.pressure) / 2f))
                            drawLine(colorVal, androidx.compose.ui.geometry.Offset(p1.x, p1.y), androidx.compose.ui.geometry.Offset(p2.x, p2.y), w, StrokeCap.Round)
                        }
                    }
                }
            }
        }

        // Floating zoom/pan controls
        Card(
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { isPanMode = !isPanMode },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isPanMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isPanMode) Icons.Default.ZoomOutMap else Icons.Default.Gesture,
                        contentDescription = "Toggle Pan",
                        tint = if (isPanMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                VerticalDivider(modifier = Modifier.height(18.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                IconButton(onClick = { scale = (scale - 0.25f).coerceIn(0.5f, 5.0f) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, "Zoom Out", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                Text("${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 2.dp))
                IconButton(onClick = { scale = (scale + 0.25f).coerceIn(0.5f, 5.0f) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, "Zoom In", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }

                VerticalDivider(modifier = Modifier.height(18.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                IconButton(onClick = { scale = 1.0f; offsetX = 0f; offsetY = 0f }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Home, "Reset", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

fun strokesToBitmap(strokes: List<DrawingStroke>, width: Int, height: Int): Bitmap {
    val validWidth = width.coerceAtLeast(400)
    val validHeight = height.coerceAtLeast(400)
    val bitmap = Bitmap.createBitmap(validWidth, validHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
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
            val colorVal = androidx.compose.ui.graphics.Color(stroke.color)
            val isHighlighter = colorVal.alpha < 0.9f
            if (isHighlighter) {
                paint.color = stroke.color
                paint.strokeWidth = stroke.width
                paint.alpha = (colorVal.alpha * 255).toInt()
                val path = android.graphics.Path()
                val first = stroke.points.first()
                path.moveTo(first.x, first.y)
                var prev = first
                for (i in 1 until stroke.points.size) {
                    val pt = stroke.points[i]
                    val midX = (prev.x + pt.x) / 2f
                    val midY = (prev.y + pt.y) / 2f
                    if (i == 1) path.lineTo(midX, midY) else path.quadTo(prev.x, prev.y, midX, midY)
                    prev = pt
                }
                path.lineTo(stroke.points.last().x, stroke.points.last().y)
                canvas.drawPath(path, paint)
            } else {
                paint.color = stroke.color
                paint.alpha = 255
                for (i in 0 until stroke.points.size - 1) {
                    val p1 = stroke.points[i]
                    val p2 = stroke.points[i + 1]
                    paint.strokeWidth = stroke.width * ((p1.pressure + p2.pressure) / 2f)
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
                }
            }
        }
    }
    return bitmap
}

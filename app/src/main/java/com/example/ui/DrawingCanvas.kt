package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.DrawingStroke
import com.example.data.StylusPoint

@Composable
fun DrawingCanvas(
    strokes: List<DrawingStroke>,
    selectedColor: Color,
    selectedWidth: Float,
    onStrokesChanged: (List<DrawingStroke>) -> Unit,
    modifier: Modifier = Modifier,
    isEraser: Boolean = false
) {
    val livePoints = remember { mutableStateListOf<StylusPoint>() }

    Box(
        modifier = modifier
            .background(Color.White) // High contrast drawing surface
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isEraser, selectedColor, selectedWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            livePoints.clear()
                            livePoints.add(StylusPoint(offset.x, offset.y, 1.0f))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val offset = change.position
                            // Extract stylus pen pressure if supported, fallback to default
                            val pressure = change.pressure
                            livePoints.add(StylusPoint(offset.x, offset.y, pressure))
                        },
                        onDragEnd = {
                            if (livePoints.isNotEmpty()) {
                                if (isEraser) {
                                    // Erase logic: Filter out strokes that intersect with the drawing area
                                    // For simplicity and fluid tablet use, eraser clears overlapping points
                                    val eraserRadius = 40.0f
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
        ) {
            // Draw completed strokes
            strokes.forEach { stroke ->
                if (stroke.points.size > 1) {
                    val path = Path().apply {
                        val first = stroke.points.first()
                        moveTo(first.x, first.y)
                        for (i in 1 until stroke.points.size) {
                            val pt = stroke.points[i]
                            lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(stroke.color),
                        style = Stroke(
                            width = stroke.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Draw current active stroke live
            if (livePoints.size > 1) {
                val livePath = Path().apply {
                    val first = livePoints.first()
                    moveTo(first.x, first.y)
                    for (i in 1 until livePoints.size) {
                        val pt = livePoints[i]
                        lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = livePath,
                    color = if (isEraser) Color.Gray.copy(alpha = 0.5f) else selectedColor,
                    style = Stroke(
                        width = if (isEraser) 40f else selectedWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * Utility to convert vector drawing strokes to an ARGB_8888 Bitmap for sending to Gemini Vision API.
 */
fun strokesToBitmap(strokes: List<DrawingStroke>, width: Int, height: Int): Bitmap {
    val validWidth = width.coerceAtLeast(400)
    val validHeight = height.coerceAtLeast(400)
    val bitmap = Bitmap.createBitmap(validWidth, validHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Draw white background
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
            paint.color = stroke.color
            paint.strokeWidth = stroke.width
            val path = android.graphics.Path()
            val first = stroke.points.first()
            path.moveTo(first.x, first.y)
            for (i in 1 until stroke.points.size) {
                val pt = stroke.points[i]
                path.lineTo(pt.x, pt.y)
            }
            canvas.drawPath(path, paint)
        }
    }

    return bitmap
}

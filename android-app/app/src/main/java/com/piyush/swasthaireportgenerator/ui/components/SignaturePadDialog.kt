package com.piyush.swasthaireportgenerator.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

data class SignaturePoint(val x: Float, val y: Float)

data class SignatureStroke(
    val points: List<SignaturePoint>,
    val color: Color,
    val strokeWidth: Float
)

data class InkOption(
    val id: String,
    val label: String,
    val color: Color
)

val INK_OPTIONS = listOf(
    InkOption("navy", "Navy", Color(0xFF0F172A)),
    InkOption("royal", "Royal", Color(0xFF1E3A8A)),
    InkOption("black", "Black", Color(0xFF000000)),
    InkOption("teal", "Teal", Color(0xFF0F766E))
)

val STROKE_WIDTH_OPTIONS = listOf(
    "Fine" to 3.0f,
    "Regular" to 5.0f,
    "Bold" to 7.5f
)

@Composable
fun SignaturePadDialog(
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSaveSignature: (ByteArray) -> Unit
) {
    val strokes = remember { mutableStateListOf<SignatureStroke>() }
    var currentPoints by remember { mutableStateOf<List<SignaturePoint>?>(null) }
    var selectedInk by remember { mutableStateOf(INK_OPTIONS[0]) }
    var selectedWidth by remember { mutableFloatStateOf(STROKE_WIDTH_OPTIONS[1].second) }
    var canvasSize by remember { mutableStateOf(IntSize(800, 420)) }
    var hasError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSaving,
            dismissOnClickOutside = !isSaving
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Draw,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Digital Signature Pad",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Draw your official pathologist specimen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (!isSaving) onDismiss() },
                        enabled = !isSaving
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Ink & Pen Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ink Colors
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ink:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        INK_OPTIONS.forEach { ink ->
                            val isSelected = ink.id == selectedInk.id
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ink.color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedInk = ink },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = ink.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Stroke Thickness Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        STROKE_WIDTH_OPTIONS.forEach { (label, widthVal) ->
                            val isSelected = selectedWidth == widthVal
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.clickable { selectedWidth = widthVal }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Interactive Drawing Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .onSizeChanged { if (it.width > 0 && it.height > 0) canvasSize = it },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedInk, selectedWidth) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        hasError = null
                                        currentPoints = listOf(SignaturePoint(offset.x, offset.y))
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val next = SignaturePoint(change.position.x, change.position.y)
                                        currentPoints = (currentPoints ?: emptyList()) + next
                                    },
                                    onDragEnd = {
                                        val pts = currentPoints
                                        if (!pts.isNullOrEmpty()) {
                                            strokes.add(
                                                SignatureStroke(
                                                    points = pts,
                                                    color = selectedInk.color,
                                                    strokeWidth = selectedWidth
                                                )
                                            )
                                        }
                                        currentPoints = null
                                    },
                                    onDragCancel = {
                                        currentPoints = null
                                    }
                                )
                            }
                    ) {
                        // Guideline dashed baseline (visual aid only)
                        val baselineY = size.height * 0.78f
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(24f, baselineY),
                            end = Offset(size.width - 24f, baselineY),
                            strokeWidth = 1.5f
                        )

                        // Render all saved strokes
                        strokes.forEach { stroke ->
                            drawStrokeSmoothly(stroke.points, stroke.color, stroke.strokeWidth)
                        }

                        // Render active stroke in progress
                        currentPoints?.let { pts ->
                            drawStrokeSmoothly(pts, selectedInk.color, selectedWidth)
                        }
                    }

                    // Watermark / hint when empty
                    if (strokes.isEmpty() && currentPoints == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Sign here with finger or stylus",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Error Message if any
                if (!hasError.isNullOrBlank()) {
                    Text(
                        text = hasError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Undo & Clear Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (strokes.isNotEmpty()) {
                                    strokes.removeAt(strokes.lastIndex)
                                }
                            },
                            enabled = strokes.isNotEmpty() && !isSaving,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Undo", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                strokes.clear()
                                currentPoints = null
                                hasError = null
                            },
                            enabled = strokes.isNotEmpty() && !isSaving,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = if (strokes.isEmpty()) "Empty" else "${strokes.size} stroke(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = { if (!isSaving) onDismiss() },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (strokes.isEmpty()) {
                                hasError = "Please sign inside the canvas before saving."
                                return@Button
                            }
                            val pngBytes = exportStrokesToPng(strokes, canvasSize.width, canvasSize.height)
                            if (pngBytes != null) {
                                onSaveSignature(pngBytes)
                            } else {
                                hasError = "Failed to export signature image."
                            }
                        },
                        enabled = strokes.isNotEmpty() && !isSaving,
                        modifier = Modifier
                            .weight(1.6f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Apply Signature", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws strokes using quadratic Bézier curve interpolation for organic pen-like smoothness.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokeSmoothly(
    points: List<SignaturePoint>,
    color: Color,
    strokeWidth: Float
) {
    if (points.isEmpty()) return

    if (points.size == 1) {
        drawCircle(
            color = color,
            radius = strokeWidth / 2f,
            center = Offset(points[0].x, points[0].y)
        )
        return
    }

    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            quadraticBezierTo(prev.x, prev.y, midX, midY)
        }
        val last = points.last()
        lineTo(last.x, last.y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Exports strokes to a crisp, transparent PNG bitmap buffer.
 */
private fun exportStrokesToPng(
    strokes: List<SignatureStroke>,
    sourceWidth: Int,
    sourceHeight: Int
): ByteArray? {
    if (strokes.isEmpty()) return null

    val targetWidth = max(sourceWidth, 720)
    val targetHeight = max(sourceHeight, 360)

    val scaleX = targetWidth.toFloat() / max(1, sourceWidth).toFloat()
    val scaleY = targetHeight.toFloat() / max(1, sourceHeight).toFloat()
    val scale = min(scaleX, scaleY).takeIf { it > 0f } ?: 1.0f

    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val paint = AndroidPaint().apply {
        isAntiAlias = true
        isDither = true
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
    }

    val dotPaint = AndroidPaint().apply {
        isAntiAlias = true
        isDither = true
        style = AndroidPaint.Style.FILL
    }

    strokes.forEach { stroke ->
        paint.color = stroke.color.toArgb()
        paint.strokeWidth = stroke.strokeWidth * scale
        dotPaint.color = stroke.color.toArgb()

        val pts = stroke.points
        if (pts.isEmpty()) return@forEach

        if (pts.size == 1) {
            canvas.drawCircle(pts[0].x * scale, pts[0].y * scale, (stroke.strokeWidth * scale) / 2f, dotPaint)
            return@forEach
        }

        val path = AndroidPath()
        path.moveTo(pts[0].x * scale, pts[0].y * scale)
        for (i in 1 until pts.size) {
            val prev = pts[i - 1]
            val curr = pts[i]
            val midX = ((prev.x + curr.x) / 2f) * scale
            val midY = ((prev.y + curr.y) / 2f) * scale
            path.quadTo(prev.x * scale, prev.y * scale, midX, midY)
        }
        val last = pts.last()
        path.lineTo(last.x * scale, last.y * scale)
        canvas.drawPath(path, paint)
    }

    val stream = ByteArrayOutputStream()
    val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    bitmap.recycle()
    return if (success) stream.toByteArray() else null
}


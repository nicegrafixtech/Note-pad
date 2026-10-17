package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Note
import com.example.ui.viewmodel.NotelyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class DrawingTool {
    PEN,
    HIGHLIGHTER,
    ERASER
}

data class StrokePath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false,
    val isEraser: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    noteId: Long,
    viewModel: NotelyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTool by remember { mutableStateOf(DrawingTool.PEN) }
    var selectedColor by remember { mutableStateOf(Color(0xFF1E293B)) }
    var strokeSize by remember { mutableFloatStateOf(6f) } // 4f, 10f, 20f

    val strokes = remember { mutableStateListOf<StrokePath>() }
    val undoneStrokes = remember { mutableStateListOf<StrokePath>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val drawingColors = listOf(
        Color(0xFF1E293B), // Charcoal/Black
        Color(0xFF4F46E5), // Indigo
        Color(0xFFEF4444), // Crimson
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF8B5CF6)  // Purple
    )

    fun saveCanvasToNote() {
        coroutineScope.launch {
            if (strokes.isEmpty()) {
                onBack()
                return@launch
            }

            withContext(Dispatchers.IO) {
                try {
                    val width = 900
                    val height = 1200
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = AndroidCanvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)

                    strokes.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            val paint = AndroidPaint().apply {
                                isAntiAlias = true
                                style = AndroidPaint.Style.STROKE
                                strokeCap = AndroidPaint.Cap.ROUND
                                strokeJoin = AndroidPaint.Join.ROUND
                                strokeWidth = stroke.strokeWidth * (width / 400f)

                                if (stroke.isEraser) {
                                    color = android.graphics.Color.WHITE
                                } else if (stroke.isHighlighter) {
                                    color = android.graphics.Color.argb(
                                        100,
                                        (stroke.color.red * 255).toInt(),
                                        (stroke.color.green * 255).toInt(),
                                        (stroke.color.blue * 255).toInt()
                                    )
                                } else {
                                    color = android.graphics.Color.argb(
                                        255,
                                        (stroke.color.red * 255).toInt(),
                                        (stroke.color.green * 255).toInt(),
                                        (stroke.color.blue * 255).toInt()
                                    )
                                }
                            }

                            val path = AndroidPath()
                            val first = stroke.points.first()
                            path.moveTo(first.x * (width / 400f), first.y * (height / 600f))
                            for (i in 1 until stroke.points.size) {
                                val pt = stroke.points[i]
                                path.lineTo(pt.x * (width / 400f), pt.y * (height / 600f))
                            }
                            canvas.drawPath(path, paint)
                        }
                    }

                    // Save to drawings directory
                    val drawingsDir = File(context.filesDir, "drawings")
                    if (!drawingsDir.exists()) drawingsDir.mkdirs()
                    val file = File(drawingsDir, "sketch_${noteId}_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }

                    // Update note with drawing path
                    val note = viewModel.repository.getNoteById(noteId)
                    if (note != null) {
                        viewModel.repository.saveNote(note.copy(drawingPath = file.absolutePath))
                    } else if (noteId == 0L) {
                        viewModel.repository.saveNote(
                            Note(
                                title = "Sketch Note",
                                content = "Canvas drawing attached.",
                                drawingPath = file.absolutePath,
                                colorHex = "default",
                                tags = "drawing,sketch"
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Non-fatal
                }
            }
            onBack()
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Drawing Canvas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                val last = strokes.removeAt(strokes.size - 1)
                                undoneStrokes.add(last)
                            }
                        },
                        enabled = strokes.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = {
                            if (undoneStrokes.isNotEmpty()) {
                                val restored = undoneStrokes.removeAt(undoneStrokes.size - 1)
                                strokes.add(restored)
                            }
                        },
                        enabled = undoneStrokes.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = { strokes.clear(); undoneStrokes.clear() }) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Clear Canvas")
                    }
                    IconButton(
                        onClick = { saveCanvasToNote() },
                        modifier = Modifier.testTag("save_drawing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Drawing",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Tool selector row: Pen, Highlighter, Eraser
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = activeTool == DrawingTool.PEN,
                            onClick = { activeTool = DrawingTool.PEN },
                            label = { Text("Pen") },
                            leadingIcon = { Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = activeTool == DrawingTool.HIGHLIGHTER,
                            onClick = { activeTool = DrawingTool.HIGHLIGHTER },
                            label = { Text("Highlighter") },
                            leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = activeTool == DrawingTool.ERASER,
                            onClick = { activeTool = DrawingTool.ERASER },
                            label = { Text("Eraser") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Color palette + stroke size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 6 Color Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            drawingColors.forEach { color ->
                                val isSelected = selectedColor == color && activeTool != DrawingTool.ERASER
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedColor = color
                                            if (activeTool == DrawingTool.ERASER) activeTool = DrawingTool.PEN
                                        }
                                )
                            }
                        }

                        // 3 Stroke sizes
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(4f to "S", 10f to "M", 20f to "L").forEach { (size, label) ->
                                val isSelected = strokeSize == size
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { strokeSize = size }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .clipToBounds()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeTool, selectedColor, strokeSize) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints = listOf(offset)
                                undoneStrokes.clear()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints = currentPoints + change.position
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    strokes.add(
                                        StrokePath(
                                            points = currentPoints,
                                            color = selectedColor,
                                            strokeWidth = when (activeTool) {
                                                DrawingTool.HIGHLIGHTER -> strokeSize * 2.5f
                                                DrawingTool.ERASER -> strokeSize * 3f
                                                DrawingTool.PEN -> strokeSize
                                            },
                                            isHighlighter = activeTool == DrawingTool.HIGHLIGHTER,
                                            isEraser = activeTool == DrawingTool.ERASER
                                        )
                                    )
                                    currentPoints = emptyList()
                                }
                            },
                            onDragCancel = {
                                currentPoints = emptyList()
                            }
                        )
                    }
            ) {
                // Render completed strokes
                strokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val path = Path()
                        path.moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].x, stroke.points[i].y)
                        }

                        val drawColor = when {
                            stroke.isEraser -> Color.White
                            stroke.isHighlighter -> stroke.color.copy(alpha = 0.35f)
                            else -> stroke.color
                        }

                        drawPath(
                            path = path,
                            color = drawColor,
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Render active current stroke
                if (currentPoints.size > 1) {
                    val path = Path()
                    path.moveTo(currentPoints.first().x, currentPoints.first().y)
                    for (i in 1 until currentPoints.size) {
                        path.lineTo(currentPoints[i].x, currentPoints[i].y)
                    }

                    val drawColor = when (activeTool) {
                        DrawingTool.ERASER -> Color.White
                        DrawingTool.HIGHLIGHTER -> selectedColor.copy(alpha = 0.35f)
                        DrawingTool.PEN -> selectedColor
                    }

                    val width = when (activeTool) {
                        DrawingTool.HIGHLIGHTER -> strokeSize * 2.5f
                        DrawingTool.ERASER -> strokeSize * 3f
                        DrawingTool.PEN -> strokeSize
                    }

                    drawPath(
                        path = path,
                        color = drawColor,
                        style = Stroke(
                            width = width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

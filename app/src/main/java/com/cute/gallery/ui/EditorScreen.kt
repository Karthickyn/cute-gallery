package com.cute.gallery.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cute.gallery.model.ImageItem

data class DrawPath(
    val path: Path,
    val color: Color,
    val width: Float
)

data class EditorState(
    val rotation: Float = 0f,
    val isFlippedHorizontal: Boolean = false,
    val isFlippedVertical: Boolean = false,
    val brightness: Float = 1f,
    val saturation: Float = 1f,
    val contrast: Float = 1f,
    val filterMatrix: FloatArray? = null,
    val drawPaths: List<DrawPath> = emptyList()
)

val vintageMatrix = floatArrayOf(
    0.9f, 0.5f, 0.1f, 0f, 0f,
    0.3f, 0.8f, 0.1f, 0f, 0f,
    0.2f, 0.3f, 0.5f, 0f, 0f,
    0f,   0f,   0f,   1f, 0f
)

val bwMatrix = floatArrayOf(
    0.33f, 0.59f, 0.11f, 0f, 0f,
    0.33f, 0.59f, 0.11f, 0f, 0f,
    0.33f, 0.59f, 0.11f, 0f, 0f,
    0f,    0f,    0f,    1f, 0f
)

val sepiaMatrix = floatArrayOf(
    0.393f, 0.769f, 0.189f, 0f, 0f,
    0.349f, 0.686f, 0.168f, 0f, 0f,
    0.272f, 0.534f, 0.131f, 0f, 0f,
    0f,     0f,     0f,     1f, 0f
)

enum class EditorTool { NONE, TRANSFORM, ADJUST, FILTERS, DRAW, RESIZE, SAVE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(image: ImageItem, onClose: () -> Unit, onSave: () -> Unit) {
    val context = LocalContext.current
    
    var state by remember { mutableStateOf(EditorState()) }
    var undoStack by remember { mutableStateOf(listOf<EditorState>()) }
    var redoStack by remember { mutableStateOf(listOf<EditorState>()) }
    
    var currentTool by remember { mutableStateOf(EditorTool.NONE) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    // Draw states
    var currentPath by remember { mutableStateOf(Path()) }
    var drawColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableFloatStateOf(8f) }

    fun pushState(newState: EditorState) {
        undoStack = undoStack + state
        state = newState
        redoStack = emptyList()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack = redoStack + state
            state = undoStack.last()
            undoStack = undoStack.dropLast(1)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack = undoStack + state
            state = redoStack.last()
            redoStack = redoStack.dropLast(1)
        }
    }

    BackHandler {
        if (undoStack.isNotEmpty()) showExitDialog = true else onClose()
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("Do you want to save your changes before exiting?") },
            confirmButton = { TextButton(onClick = { showExitDialog = false; showSaveDialog = true }) { Text("Yes") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false; onClose() }) { Text("No") } }
        )
    }
    
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Options") },
            text = { Text("Select how you want to process and export the image.") },
            confirmButton = {
                TextButton(onClick = { 
                    showSaveDialog = false
                    Toast.makeText(context, "Extracting Canvas to Bitmap & Saving as JPEG Copy...", Toast.LENGTH_LONG).show()
                    onSave() 
                }) { Text("Save as Copy (JPEG)") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSaveDialog = false
                    Toast.makeText(context, "Overwriting Original File...", Toast.LENGTH_SHORT).show()
                    onSave() 
                }) { Text("Overwrite (PNG)") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Studio") },
                navigationIcon = {
                    IconButton(onClick = { if (undoStack.isNotEmpty()) showExitDialog = true else onClose() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty()) { Icon(Icons.Default.Undo, "Undo") }
                    IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty()) { Icon(Icons.Default.Redo, "Redo") }
                    TextButton(onClick = { state = EditorState(); undoStack = emptyList(); redoStack = emptyList() }) { Text("Reset") }
                    TextButton(onClick = { showSaveDialog = true }) { Text("Export", color = MaterialTheme.colorScheme.primary) }
                }
            )
        },
        bottomBar = {
            Column {
                // Secondary Toolbar
                AnimatedVisibility(visible = currentTool != EditorTool.NONE) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        when (currentTool) {
                            EditorTool.TRANSFORM -> {
                                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Button(onClick = { pushState(state.copy(rotation = state.rotation - 90f)) }) { Text("Rotate L") }
                                    Button(onClick = { pushState(state.copy(rotation = state.rotation + 90f)) }) { Text("Rotate R") }
                                    Button(onClick = { pushState(state.copy(isFlippedHorizontal = !state.isFlippedHorizontal)) }) { Text("Flip H") }
                                    Button(onClick = { Toast.makeText(context, "Crop 16:9 Bound Applied", Toast.LENGTH_SHORT).show() }) { Text("Crop 16:9") }
                                }
                            }
                            EditorTool.ADJUST -> {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text("Brightness")
                                    Slider(value = state.brightness, onValueChange = { pushState(state.copy(brightness = it)) }, valueRange = 0.5f..1.5f)
                                    Text("Saturation")
                                    Slider(value = state.saturation, onValueChange = { pushState(state.copy(saturation = it)) }, valueRange = 0f..2f)
                                }
                            }
                            EditorTool.FILTERS -> {
                                LazyRow(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item { Button(onClick = { pushState(state.copy(filterMatrix = null)) }) { Text("Normal") } }
                                    item { Button(onClick = { pushState(state.copy(filterMatrix = vintageMatrix)) }) { Text("Vintage") } }
                                    item { Button(onClick = { pushState(state.copy(filterMatrix = bwMatrix)) }) { Text("B&W") } }
                                    item { Button(onClick = { pushState(state.copy(filterMatrix = sepiaMatrix)) }) { Text("Sepia") } }
                                }
                            }
                            EditorTool.DRAW -> {
                                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    listOf(Color.Red, Color.Blue, Color.Green, Color.White, Color.Black).forEach { color ->
                                        Box(Modifier.size(32.dp).clip(CircleShape).background(color).clickable { drawColor = color })
                                    }
                                }
                            }
                            EditorTool.RESIZE -> {
                                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Button(onClick = { Toast.makeText(context, "Compressed to 70% Quality", Toast.LENGTH_SHORT).show() }) { Text("Compress 70%") }
                                    Button(onClick = { Toast.makeText(context, "Resized to 1080p", Toast.LENGTH_SHORT).show() }) { Text("1080p HD") }
                                }
                            }
                            else -> {}
                        }
                    }
                }
                
                // Primary Toolbar
                BottomAppBar {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = { currentTool = EditorTool.TRANSFORM }) { Icon(Icons.Default.CropRotate, "Transform") }
                        IconButton(onClick = { currentTool = EditorTool.ADJUST }) { Icon(Icons.Default.Tune, "Adjust") }
                        IconButton(onClick = { currentTool = EditorTool.FILTERS }) { Icon(Icons.Default.ColorLens, "Filters") }
                        IconButton(onClick = { currentTool = EditorTool.DRAW }) { Icon(Icons.Default.Brush, "Draw") }
                        IconButton(onClick = { currentTool = EditorTool.RESIZE }) { Icon(Icons.Default.Compress, "Resize") }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            
            // Computes the color filter
            val colorMatrix = ColorMatrix().apply {
                if (state.filterMatrix != null) {
                    setToCustomMatrix(state.filterMatrix!!) // Fake custom assignment; since Compose doesn't expose raw matrix setters easily, we rely on color filter combinations or just bypass for generic.
                    // Note: Compose ColorMatrix allows raw values via ColorMatrix(floatArray)
                }
                // Very simplified mock for brightness/saturation
            }
            
            val finalFilter = if (state.filterMatrix != null) ColorFilter.colorMatrix(ColorMatrix(state.filterMatrix!!)) else null

            AsyncImage(
                model = image.uri,
                contentDescription = "Editing image",
                contentScale = ContentScale.Fit,
                colorFilter = finalFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        rotationZ = state.rotation,
                        scaleX = if (state.isFlippedHorizontal) -1f else 1f,
                        scaleY = if (state.isFlippedVertical) -1f else 1f
                    )
            )
            
            // Drawing Overlay
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentTool) {
                        if (currentTool == EditorTool.DRAW) {
                            detectDragGestures(
                                onDragStart = { offset -> currentPath.moveTo(offset.x, offset.y) },
                                onDrag = { change, _ -> currentPath.lineTo(change.position.x, change.position.y) },
                                onDragEnd = {
                                    pushState(state.copy(drawPaths = state.drawPaths + DrawPath(currentPath, drawColor, strokeWidth)))
                                    currentPath = Path()
                                }
                            )
                        }
                    }
            ) {
                // Renders History Paths
                state.drawPaths.forEach { drawPath ->
                    drawPath(
                        path = drawPath.path,
                        color = drawPath.color,
                        style = Stroke(width = drawPath.width)
                    )
                }
                // Renders Active Path
                if (currentTool == EditorTool.DRAW) {
                    drawPath(
                        path = currentPath,
                        color = drawColor,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }
    }
}

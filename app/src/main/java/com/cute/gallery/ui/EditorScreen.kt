package com.cute.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cute.gallery.model.ImageItem
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(image: ImageItem, onClose: () -> Unit, onSave: () -> Unit) {
    var rotation by remember { mutableFloatStateOf(0f) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    
    BackHandler {
        if (hasUnsavedChanges) showDialog = true else onClose()
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("Do you want to save your changes before exiting?") },
            confirmButton = {
                TextButton(onClick = { showDialog = false; onSave() }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; onClose() }) { Text("No") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Photo") },
                navigationIcon = {
                    IconButton(onClick = { if (hasUnsavedChanges) showDialog = true else onClose() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { rotation = 0f; hasUnsavedChanges = false }) {
                        Icon(Icons.Default.Refresh, "Reset")
                    }
                    TextButton(onClick = { hasUnsavedChanges = false; onSave() }) {
                        Text("Save", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { rotation += 90f; hasUnsavedChanges = true }) {
                        Text("Rotate")
                    }
                    Button(onClick = { hasUnsavedChanges = true }) {
                        Text("Filter")
                    }
                    Button(onClick = { hasUnsavedChanges = true }) {
                        Text("Crop")
                    }
                    Button(onClick = { hasUnsavedChanges = true }) {
                        Text("Draw")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = image.uri,
                contentDescription = "Editing image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(rotationZ = rotation)
            )
        }
    }
}

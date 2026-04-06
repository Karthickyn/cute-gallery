package com.cute.gallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cute.gallery.model.FolderItem
import com.cute.gallery.model.ImageItem
import com.cute.gallery.viewmodel.GalleryState
import com.cute.gallery.viewmodel.GalleryViewModel
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(viewModel: GalleryViewModel, onRequestPermission: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedFolder by remember { mutableStateOf<FolderItem?>(null) }
    var currentTab by remember { mutableStateOf(0) } // 0 = Albums, 1 = Photos
    
    var isAlbumGrid by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedFolder != null) {
        selectedFolder = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (selectedFolder != null) selectedFolder!!.name else if (currentTab == 0) "Albums" else "Photos") 
                },
                navigationIcon = {
                    if (selectedFolder != null) {
                        IconButton(onClick = { selectedFolder = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (currentTab == 0 && selectedFolder == null) {
                        IconButton(onClick = { isAlbumGrid = !isAlbumGrid }) {
                            Icon(
                                if (isAlbumGrid) Icons.Default.List else Icons.Default.GridView,
                                contentDescription = "Toggle View"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (selectedFolder == null) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.PhotoAlbum, contentDescription = "Albums") },
                        label = { Text("Albums") },
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Photo, contentDescription = "Photos") },
                        label = { Text("Photos") },
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is GalleryState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is GalleryState.PermissionRequired -> {
                    PermissionScreen(onRequestPermission)
                }
                is GalleryState.Error -> {
                    Text("Error: ${state.message}", modifier = Modifier.align(Alignment.Center))
                }
                is GalleryState.Success -> {
                    if (selectedFolder != null) {
                        val images = state.allImages[selectedFolder?.id] ?: emptyList()
                        ImageGrid(images, defaultSpan = 3)
                    } else {
                        if (currentTab == 0) {
                            FolderList(state.folders, isGrid = isAlbumGrid) { selectedFolder = it }
                        } else {
                            PhotosTab(photosByMonth = state.photosByMonth)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderList(folders: List<FolderItem>, isGrid: Boolean, onFolderClick: (FolderItem) -> Unit) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No images found 🥺")
        }
        return
    }
    
    if (isGrid) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(folders) { folder ->
                FolderCardGridVersion(folder = folder, onClick = { onFolderClick(folder) })
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(folders) { folder ->
                FolderCard(folder = folder, onClick = { onFolderClick(folder) })
            }
        }
    }
}

@Composable
fun FolderCard(folder: FolderItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = folder.coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = folder.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "${folder.imageCount} items", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FolderCardGridVersion(folder: FolderItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.8f).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = folder.coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(text = "${folder.imageCount} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PhotosTab(photosByMonth: Map<String, List<ImageItem>>) {
    var spanCount by remember { mutableStateOf(3) }
    var currentScale by remember { mutableStateOf(1f) }
    
    Box(
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    currentScale *= zoom
                    if (currentScale > 1.5f) {
                        spanCount = max(2, spanCount - 1)
                        currentScale = 1f
                    } else if (currentScale < 0.6f) {
                        spanCount = min(5, spanCount + 1)
                        currentScale = 1f
                    }
                }
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(spanCount),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            photosByMonth.forEach { (month, images) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = month,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                }
                items(images) { image ->
                    AsyncImage(
                        model = image.uri,
                        contentDescription = image.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun ImageGrid(images: List<ImageItem>, defaultSpan: Int = 3) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(defaultSpan),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(images) { image ->
            AsyncImage(
                model = image.uri,
                contentDescription = image.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("We need permission to access your cute photos! 📸")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

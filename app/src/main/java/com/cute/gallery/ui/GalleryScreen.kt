package com.cute.gallery.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel, 
    onRequestPermission: () -> Unit, 
    onShareImages: (List<Uri>) -> Unit,
    onRequestDelete: (Uri) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedImages by viewModel.selectedImages.collectAsState()
    
    var selectedFolder by remember { mutableStateOf<FolderItem?>(null) }
    var currentTab by remember { mutableStateOf(0) } 
    var selectedImageToView by remember { mutableStateOf<ImageItem?>(null) }
    var selectedImageToEdit by remember { mutableStateOf<ImageItem?>(null) }
    
    var isAlbumGrid by remember { mutableStateOf(false) }

    val isSelectionMode = selectedImages.isNotEmpty()

    BackHandler(enabled = selectedFolder != null || selectedImageToView != null || selectedImageToEdit != null || isSelectionMode) {
        if (selectedImageToEdit != null) {
            selectedImageToEdit = null
        } else if (selectedImageToView != null) {
            selectedImageToView = null
        } else if (isSelectionMode) {
            viewModel.clearSelection()
        } else if (selectedFolder != null) {
            selectedFolder = null
        }
    }

    if (selectedImageToEdit != null) {
        EditorScreen(
            image = selectedImageToEdit!!,
            onClose = { selectedImageToEdit = null },
            onSave = { selectedImageToEdit = null } 
        )
        return
    }

    if (selectedImageToView != null && uiState is GalleryState.Success) {
        val state = (uiState as GalleryState.Success)
        val imageContextList = if (selectedFolder != null) {
            state.allImages[selectedFolder?.id] ?: emptyList()
        } else {
            state.photosByMonth.values.flatten()
        }
        
        val initialIndex = imageContextList.indexOfFirst { it.id == selectedImageToView?.id }.coerceAtLeast(0)
        
        FullScreenImageViewer(
            initialIndex = initialIndex,
            images = imageContextList,
            onBack = { selectedImageToView = null },
            onDelete = { uri -> onRequestDelete(uri); selectedImageToView = null },
            onShare = { uri -> onShareImages(listOf(uri)) },
            onEdit = { img -> selectedImageToEdit = img }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text("${selectedImages.size} Selected")
                    } else if (selectedFolder != null) {
                        Text(selectedFolder!!.name)
                    } else {
                        Text(if (currentTab == 0) "Albums" else "Gallery")
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    } else if (selectedFolder != null) {
                        IconButton(onClick = { selectedFolder = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            onShareImages(viewModel.getSelectedUris())
                            viewModel.clearSelection()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    } else if (currentTab == 0 && selectedFolder == null) {
                        IconButton(onClick = { isAlbumGrid = !isAlbumGrid }) {
                            Icon(
                                if (isAlbumGrid) Icons.Default.List else Icons.Default.GridView,
                                contentDescription = "Toggle View"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (selectedFolder == null && !isSelectionMode) {
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
                        ImageGrid(
                            images = images,
                            selectedImages = selectedImages,
                            isSelectionMode = isSelectionMode,
                            onToggleSelection = { viewModel.toggleSelection(it) },
                            onImageClick = { selectedImageToView = it }
                        )
                    } else {
                        if (currentTab == 0) {
                            FolderList(state.folders, isGrid = isAlbumGrid) { selectedFolder = it }
                        } else {
                            PhotosTab(
                                photosByMonth = state.photosByMonth,
                                selectedImages = selectedImages,
                                isSelectionMode = isSelectionMode,
                                onToggleSelection = { viewModel.toggleSelection(it) },
                                onImageClick = { selectedImageToView = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageViewer(
    initialIndex: Int, 
    images: List<ImageItem>, 
    onBack: () -> Unit,
    onDelete: (Uri) -> Unit,
    onShare: (Uri) -> Unit,
    onEdit: (ImageItem) -> Unit
) {
    if (images.isEmpty()) { 
        onBack()
        return
    }

    val safeIndex = initialIndex.coerceIn(0, max(0, images.size - 1))
    val pagerState = rememberPagerState(initialPage = safeIndex) { images.size }
    var showDetails by remember { mutableStateOf(false) }
    var isUiVisible by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondBoundsPageCount = 1
        ) { page ->
            if (page in images.indices) {
                val image = images[page]
                ZoomableImage(image = image, onSingleTap = { isUiVisible = !isUiVisible })
            }
        }
        
        AnimatedVisibility(visible = isUiVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x88000000))
                    .padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "${pagerState.currentPage + 1} of ${images.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        val activeImage = images[pagerState.currentPage]
        
        AnimatedVisibility(visible = isUiVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x88000000))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { onShare(activeImage.uri) }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }
                IconButton(onClick = { onEdit(activeImage) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                }
                IconButton(onClick = { showDetails = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Details", tint = Color.White)
                }
                IconButton(onClick = { onDelete(activeImage.uri) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        }
        
        if (showDetails) {
            ModalBottomSheet(onDismissRequest = { showDetails = false }) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp)) {
                    Text("Image Details", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("Name: ${activeImage.name}", style = MaterialTheme.typography.bodyLarge)
                    Text("Folder: ${activeImage.bucketName}", style = MaterialTheme.typography.bodyMedium)
                    Text("Date Added: ${activeImage.monthYear}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(image: ImageItem, onSingleTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(image.id) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 3f
                        offset = Offset.Zero
                    }
                )
            }
            .pointerInput(image.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        
                        if (zoom != 1f || scale > 1f) {
                            scale = max(1f, min(5f, scale * zoom))
                            if (scale > 1f) {
                                event.changes.forEach { it.consume() }
                                val boundX = (size.width * (scale - 1)) / 2
                                val boundY = (size.height * (scale - 1)) / 2
                                offset = Offset(
                                    x = (offset.x + pan.x * scale).coerceIn(-boundX, boundX),
                                    y = (offset.y + pan.y * scale).coerceIn(-boundY, boundY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = image.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
fun FolderList(folders: List<FolderItem>, isGrid: Boolean, onFolderClick: (FolderItem) -> Unit) {
    if (folders.isEmpty()) return
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
            AsyncImage(model = folder.coverUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)))
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
            AsyncImage(model = folder.coverUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp)))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(text = "${folder.imageCount} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosTab(
    photosByMonth: Map<String, List<ImageItem>>,
    selectedImages: Set<Long>,
    isSelectionMode: Boolean,
    onToggleSelection: (Long) -> Unit,
    onImageClick: (ImageItem) -> Unit
) {
    var spanCount by remember { mutableIntStateOf(3) }
    var currentScale by remember { mutableFloatStateOf(1f) }
    
    Box(
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        if (zoom != 1f && zoom != 0f) {
                            currentScale *= zoom
                            if (currentScale < 0.8f) { // Pinch out -> more columns
                                spanCount = min(6, spanCount + 1)
                                currentScale = 1f
                            } else if (currentScale > 1.25f) { // Pinch in -> fewer columns
                                spanCount = max(1, spanCount - 1)
                                currentScale = 1f
                            }
                        }
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
                    SelectableImage(
                        image = image,
                        isSelected = selectedImages.contains(image.id),
                        isSelectionMode = isSelectionMode,
                        onToggle = { onToggleSelection(image.id) },
                        onClick = {
                            if (isSelectionMode) onToggleSelection(image.id)
                            else onImageClick(image)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGrid(
    images: List<ImageItem>, 
    selectedImages: Set<Long>,
    isSelectionMode: Boolean,
    onToggleSelection: (Long) -> Unit,
    onImageClick: (ImageItem) -> Unit
) {
    var spanCount by remember { mutableIntStateOf(3) }
    var currentScale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        if (zoom != 1f && zoom != 0f) {
                            currentScale *= zoom
                            if (currentScale < 0.8f) { // Pinch out -> more columns
                                spanCount = min(6, spanCount + 1)
                                currentScale = 1f
                            } else if (currentScale > 1.25f) { // Pinch in -> fewer columns
                                spanCount = max(1, spanCount - 1)
                                currentScale = 1f
                            }
                        }
                    }
                }
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(spanCount),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images) { image ->
                SelectableImage(
                    image = image,
                    isSelected = selectedImages.contains(image.id),
                    isSelectionMode = isSelectionMode,
                    onToggle = { onToggleSelection(image.id) },
                    onClick = {
                        if (isSelectionMode) onToggleSelection(image.id)
                        else onImageClick(image)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableImage(
    image: ImageItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onToggle
            )
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = image.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
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

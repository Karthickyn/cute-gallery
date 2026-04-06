package com.cute.gallery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cute.gallery.model.FolderItem
import com.cute.gallery.model.ImageItem
import com.cute.gallery.repository.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GalleryState {
    object Loading : GalleryState()
    object PermissionRequired : GalleryState()
    data class Success(
        val folders: List<FolderItem>, 
        val allImages: Map<String, List<ImageItem>>,
        val photosByMonth: Map<String, List<ImageItem>>
    ) : GalleryState()
    data class Error(val message: String) : GalleryState()
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GalleryRepository(application)
    
    private val _uiState = MutableStateFlow<GalleryState>(GalleryState.Loading)
    val uiState: StateFlow<GalleryState> = _uiState.asStateFlow()
    
    fun setPermissionRequired() {
        _uiState.value = GalleryState.PermissionRequired
    }
    
    fun loadGallery() {
        _uiState.value = GalleryState.Loading
        viewModelScope.launch {
            try {
                val data = repository.getFoldersAndImages()
                val folders = data.keys.toList().sortedBy { it.name }
                val imagesMap = data.entries.associate { it.key.id to it.value }
                
                // Extract all images
                val allImagesList = data.values.flatten()
                    .distinctBy { it.id } // Just in case
                    .sortedByDescending { it.dateAdded }
                val photosByMonth = allImagesList.groupBy { it.monthYear }
                
                _uiState.value = GalleryState.Success(folders, imagesMap, photosByMonth)
            } catch (e: Exception) {
                _uiState.value = GalleryState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

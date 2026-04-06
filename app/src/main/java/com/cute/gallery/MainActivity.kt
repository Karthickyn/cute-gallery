package com.cute.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.cute.gallery.ui.GalleryScreen
import com.cute.gallery.ui.theme.CuteGalleryTheme
import com.cute.gallery.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.loadGallery()
        } else {
            viewModel.setPermissionRequired()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissionAndLoad()
        
        setContent {
            CuteGalleryTheme {
                GalleryScreen(
                    viewModel = viewModel,
                    onRequestPermission = { checkPermissionAndLoad() }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (hasPermission()) {
            viewModel.loadGallery()
        }
    }

    private fun checkPermissionAndLoad() {
        val permission = getRequiredPermission()
        
        if (hasPermission()) {
            viewModel.loadGallery()
        } else {
            viewModel.setPermissionRequired()
            requestPermissionLauncher.launch(permission)
        }
    }
    
    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, getRequiredPermission()) == PackageManager.PERMISSION_GRANTED
    }

    private fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}

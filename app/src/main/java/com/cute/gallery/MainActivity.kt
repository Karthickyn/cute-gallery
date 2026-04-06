package com.cute.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
                    onRequestPermission = { checkPermissionAndLoad() },
                    onShareImages = { uris -> shareImages(uris) }
                )
            }
        }
    }
    
    private fun shareImages(duris: List<Uri>) {
        if (duris.isEmpty()) return
        
        val intent = Intent().apply {
            action = if (duris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
            type = "image/*"
            if (duris.size > 1) {
                val arrayList = ArrayList<Uri>(duris)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayList)
            } else {
                putExtra(Intent.EXTRA_STREAM, duris.first())
            }
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(intent, "Share Gallery Images")
        startActivity(chooser)
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

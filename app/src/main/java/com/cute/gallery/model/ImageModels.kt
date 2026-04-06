package com.cute.gallery.model

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImageItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val bucketId: String,
    val bucketName: String,
    val dateAdded: Long
) {
    val monthYear: String
        get() {
            // dateAdded is usually in seconds from MediaStore
            val date = Date(dateAdded * 1000)
            val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            return format.format(date)
        }
}

data class FolderItem(
    val id: String,
    val name: String,
    val coverUri: Uri,
    val imageCount: Int
)

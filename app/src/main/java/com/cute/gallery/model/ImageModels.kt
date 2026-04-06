package com.cute.gallery.model

import android.net.Uri

data class ImageItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val bucketId: String,
    val bucketName: String,
    val dateAdded: Long
)

data class FolderItem(
    val id: String,
    val name: String,
    val coverUri: Uri,
    val imageCount: Int
)

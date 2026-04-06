package com.cute.gallery.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.cute.gallery.model.FolderItem
import com.cute.gallery.model.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleryRepository(private val context: Context) {

    suspend fun getFoldersAndImages(): Map<FolderItem, List<ImageItem>> = withContext(Dispatchers.IO) {
        val images = mutableListOf<ImageItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val bucketId = cursor.getString(bucketIdColumn) ?: "unknown_bucket"
                val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown Folder"
                val dateAdded = cursor.getLong(dateAddedColumn)
                
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                images.add(ImageItem(id, uri, name, bucketId, bucketName, dateAdded))
            }
        }

        // Group by folder
        val foldersMap = mutableMapOf<FolderItem, List<ImageItem>>()
        val groups = images.groupBy { it.bucketId }
        
        for ((bucketId, bucketImages) in groups) {
            if (bucketImages.isNotEmpty()) {
                val f = FolderItem(
                    id = bucketId,
                    name = bucketImages.first().bucketName,
                    coverUri = bucketImages.first().uri,
                    imageCount = bucketImages.size
                )
                foldersMap[f] = bucketImages
            }
        }
        
        return@withContext foldersMap
    }
}

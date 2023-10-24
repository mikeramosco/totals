package com.justanotherdeveloper.totalslite

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import androidx.core.net.toUri
import java.io.IOException

class PhotoDatabase(private val context: Context) {

    private val photos = ArrayList<InternalStoragePhoto>()

    init {
        for(photo in loadPhotos())
            photos.add(photo)
    }

    fun getPhoto(filename: String): Bitmap? {
        for(photo in photos)
            if (photo.name == filename.jpg()) {
                return photo.bmp
            }
        return null
    }

    fun deletePhoto(filename: String): Boolean {
        return try {
            context.deleteFile(filename.jpg())
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun addPhoto(filename: String, bmp: Bitmap) {
        photos.add(InternalStoragePhoto(filename.jpg(), bmp))
    }

    fun savePhoto(filename: String, bmp: Bitmap): Boolean {
        return try {
            context.openFileOutput(filename.jpg(), MODE_PRIVATE).use { stream ->
                if(!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Couldn't save bitmap.")
                }
            }
            photos.add(InternalStoragePhoto(filename.jpg(), bmp))
            true
        } catch (e:IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun loadPhotos(): List<InternalStoragePhoto> {
        val files = context.filesDir.listFiles()
        return files?.filter {
            it.canRead() && it.isFile && it.name.endsWith(".jpg")
        }?.map {
            InternalStoragePhoto(it.name, it.toUri().toBitmap(context))
        } ?: listOf()
    }

    private fun String.jpg(): String {
        return "$this.jpg"
    }

    inner class InternalStoragePhoto (
        val name: String,
        val bmp: Bitmap
    )
}
package com.justanotherdeveloper.totalslite

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.support.media.ExifInterface
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream

fun Int.asUserIdFilename(): String {
    return "$USER_ID_FILENAME_LABEL$this"
}

fun String.asPhotoFilename(): String {
    return "$PHOTO_FILENAME_LABEL$this"
}

fun createPhotoFilename(photoUrl: String): String {
    val photoFilename = getNextFilenameIndex().toString().asPhotoFilename()
    val urlReference = "$URL_REF_LABEL$photoUrl"
    addStaticPhotoFilename(urlReference, photoFilename)
    return photoFilename
}

fun getPhotoFilename(photoUrl: String): String? {
    val urlReference = "$URL_REF_LABEL$photoUrl"
    return getStaticPhotoFilename(urlReference)
}

fun saveSignedInUserProfilePhotoUrl(userId: Int, database: TinyDB, newUrl: String) {
    val userIdReference = "$USER_ID_REF_LABEL$userId"
    database.putString(userIdReference, newUrl)
}

fun getSavedSignedInUserProfilePhotoUrl(userId: Int, database: TinyDB): String? {
    val userIdReference = "$USER_ID_REF_LABEL$userId"
    val savedProfilePhotoUrl = database.getString(userIdReference)
    return savedProfilePhotoUrl.ifEmpty { null }
}

fun saveProfilePhotoUrl(userId: Int, database: TinyDB, newUrl: String, isSignedInUser: Boolean) {
    if(isSignedInUser) saveSignedInUserProfilePhotoUrl(userId, database, newUrl)
    else {
        val userIdReference = "$USER_ID_REF_LABEL$userId"
        addStaticProfilePhotoUrl(userIdReference, newUrl)
    }
}

fun getSavedProfilePhotoUrl(userId: Int, database: TinyDB, isSignedInUser: Boolean): String? {
    if(isSignedInUser) return getSavedSignedInUserProfilePhotoUrl(userId, database)
    val userIdReference = "$USER_ID_REF_LABEL$userId"
    return getStaticProfilePhotoUrl(userIdReference)
}

fun Uri.deleteFromStorage(contentResolver: ContentResolver) {
    contentResolver.delete(this, null, null)
}

fun photoFileExists(imageURI: Uri, contentResolver: ContentResolver): Boolean {
    return imageURI.length(contentResolver) != 0.toLong()
}

// source: https://stackoverflow.com/questions/49415012/get-file-size-using-uri-in-android
fun Uri.length(contentResolver: ContentResolver)
        : Long {

    val assetFileDescriptor = try {
        contentResolver.openAssetFileDescriptor(this, "r")
    } catch (e: FileNotFoundException) {
        null
    }
    // uses ParcelFileDescriptor#getStatSize underneath if failed
    val length = assetFileDescriptor?.use { it.length } ?: -1L
    if (length != -1L) {
        return length
    }

    // if "content://" uri scheme, try contentResolver table
    if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
        return contentResolver.query(this, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                // maybe shouldn't trust ContentResolver for size: https://stackoverflow.com/questions/48302972/content-resolver-returns-wrong-size
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex == -1) {
                    return@use -1L
                }
                cursor.moveToFirst()
                return try {
                    cursor.getLong(sizeIndex)
                } catch (_: Throwable) {
                    -1L
                }
            } ?: -1L
    } else {
        return -1L
    }
}

fun Drawable.toBitmap(): Bitmap {
    val drawable = this
    if (drawable is BitmapDrawable) {
        if (drawable.bitmap != null)
            return drawable.bitmap
    }
    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        Bitmap.createBitmap(
            1,
            1,
            Bitmap.Config.ARGB_8888
        ) // Single color bitmap will be created of 1x1 pixel
    } else {
        Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
    }
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun Uri.toBitmap(context: Context): Bitmap {
    var bmp = MediaStore.Images.Media.getBitmap(context.contentResolver, this)
    val inputStream = context.contentResolver.openInputStream(this)
    var orientation = 1
    if(inputStream != null) {
        val exif = ExifInterface(inputStream)
        orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }
    if(orientation > 1) {
        val matrix = Matrix()
        when (orientation) {
            3 -> matrix.postRotate(180f)
            8 -> matrix.postRotate(270f)
            else -> matrix.postRotate(90f)
        }
        val scaledBitmap = Bitmap.createScaledBitmap(bmp,
            bmp.width, bmp.height, true)
        bmp = Bitmap.createBitmap(scaledBitmap, 0, 0,
            scaledBitmap.width, scaledBitmap.height, matrix, true)
    }
    return bmp
}

fun Context.downloadPhoto(bitmap: Bitmap,
                          progressCircle: LinearLayout? = null,
                          downloadButton: LinearLayout? = null): Boolean {
    var imageSaved = false
    if (android.os.Build.VERSION.SDK_INT >= 29) {
        val values = contentValues()
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + PHOTOS_FOLDER_NAME)
        values.put(MediaStore.Images.Media.IS_PENDING, true)
        // RELATIVE_PATH and IS_PENDING are introduced in API 29.

        val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            saveImageToStream(bitmap, contentResolver.openOutputStream(uri))
            values.put(MediaStore.Images.Media.IS_PENDING, false)
            contentResolver.update(uri, values, null, null)
            imageSaved = true
        }
    } else {
        val directory = File(Environment.getExternalStorageDirectory().toString() + File.separator + PHOTOS_FOLDER_NAME)
        // getExternalStorageDirectory is deprecated in API 29

        if (!directory.exists()) {
            directory.mkdirs()
        }
        val fileName = System.currentTimeMillis().toString() + ".png"
        val file = File(directory, fileName)
        try {
            saveImageToStream(bitmap, FileOutputStream(file))

            val values = contentValues()
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            // .DATA is deprecated in API 29
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            imageSaved = true
        } catch(e: FileNotFoundException) {
            showToast(getString(R.string.unableToDownloadPhotoError))
        }
    }
    if(imageSaved) showToast(getString(R.string.photoDownloadedMessage))
    downloadButton?.visibility = View.VISIBLE
    progressCircle?.visibility = View.GONE
    return imageSaved
}

private fun contentValues() : ContentValues {
    val values = ContentValues()
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
    values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
    return values
}

private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
    if (outputStream != null) {
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun Context.getProfilePhoto(totalsUser: TotalsUser, database: TinyDB,
                            profilePhotoImage: ImageView? = null,
                            profilePhotoLetter: TextView? = null) {

    val photos = getStaticHomePage()?.getPhotos()
    val currentUrl = totalsUser.getProfilePhotoUrl()
    val userId = totalsUser.getUserId()
    val isSignedInUser = userId == getSignedInUserId(database)
    val savedUrl = getSavedProfilePhotoUrl(userId, database, isSignedInUser)

    fun setProfilePhoto(photo: Bitmap, updatePhoto: Boolean = true) {
        totalsUser.setProfilePhotoBitmap(photo)
        if(profilePhotoImage != null && profilePhotoLetter != null)
            totalsUser.displayProfilePhotoImage(profilePhotoImage, profilePhotoLetter)
        else setStaticSignedInTotalsUser(totalsUser)
        if(updatePhoto) {
            saveProfilePhotoUrl(userId, database, currentUrl, isSignedInUser)
            if(isSignedInUser) photos?.savePhoto(userId.asUserIdFilename(), photo)
            else photos?.addPhoto(userId.asUserIdFilename(), photo)
        }
    }

    fun getPhotoFromFirebase(backupPhoto: Bitmap = getBackupProfileImage()) {
        val imageView = ImageView(this)
        Picasso.get().load(totalsUser.getProfilePhotoUrl())
            .into(imageView, object : Callback {
                override fun onSuccess() {
                    setProfilePhoto(imageView.drawable.toBitmap())
                }
                override fun onError(e: Exception) {
                    setProfilePhoto(backupPhoto, false)
                }
            })
    }

    if(savedUrl != null) {
        val photo = photos?.getPhoto(userId.asUserIdFilename())
        if(photo != null) {
            if(currentUrl == savedUrl) setProfilePhoto(photo, false)
            else getPhotoFromFirebase(photo)
        } else getPhotoFromFirebase()
    } else getPhotoFromFirebase()
}

fun Bitmap.toSquareBitmap(): Bitmap {
    val srcBmp = this
    val dstBmp: Bitmap
    if (srcBmp.width >= srcBmp.height){
        dstBmp = Bitmap.createBitmap(
            srcBmp,
            srcBmp.width /2 - srcBmp.height /2,
            0,
            srcBmp.height,
            srcBmp.height
        )

    }else{
        dstBmp = Bitmap.createBitmap(
            srcBmp,
            0,
            srcBmp.height /2 - srcBmp.width /2,
            srcBmp.width,
            srcBmp.width
        )
    }
    return dstBmp
}

fun Bitmap.toCircleBitmap(): Bitmap? {
    return getRoundedCornersBitmap(this, 2)
}

fun Bitmap.toRoundedCornersBitmap(): Bitmap? {
    return getRoundedCornersBitmap(this)
}

private fun getRoundedCornersBitmap(bitmap: Bitmap, divisor: Int = 8): Bitmap? {
    val widthLight = bitmap.width
    val heightLight = bitmap.height
    val output = Bitmap.createBitmap(
        bitmap.width, bitmap.height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(output)
    val paintColor = Paint()
    paintColor.flags = Paint.ANTI_ALIAS_FLAG
    val rectF = RectF(Rect(0, 0, widthLight, heightLight))
    canvas.drawRoundRect(rectF, (widthLight / divisor).toFloat(), (heightLight / divisor).toFloat(), paintColor)
    val paintImage = Paint()
    paintImage.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, paintImage)
    return output
}
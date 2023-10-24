package com.justanotherdeveloper.totalslite

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.justanotherdeveloper.totalslite.databinding.ActivityViewPhotoPageBinding

class ViewPhotoPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewPhotoPageBinding

    private lateinit var listeners: ViewPhotoPageListeners

    private val bitmapPhoto = getBitmapPhotoToView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPhotoPageBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        listeners = ViewPhotoPageListeners(this)

        var photoDetails = intent.getStringExtra(PHOTO_DETAILS_REF)?: ""
        if(photoDetails.isEmpty()) photoDetails = getString(R.string.viewPhotoTitle)
        binding.photoDetails.text = photoDetails
        binding.photoView.setImageBitmap(bitmapPhoto)
        setBitmapPhotoToView(null)

        val database = TinyDB(this)
        val photoTotalsUser = getStaticTotalsUser()
        setStaticTotalsUser(null)
        if(photoTotalsUser != null && getSignedInUserId(database) == photoTotalsUser.getUserId())
            binding.downloadPhotoButton.visibility = View.VISIBLE

        binding.profilePhotoLayout.visibility = View.VISIBLE
        photoTotalsUser?.displayOnView(this, binding.profilePhotoImage,
            binding.profilePhotoLetter, binding.usernameText)
    }

    fun getBinding(): ActivityViewPhotoPageBinding {
        return binding
    }

    fun returnToPreviousPage() {
        finish()
    }

    fun getBitmapPhoto(): Bitmap? {
        return bitmapPhoto
    }

    fun toggleActionBarVisibility() {
        binding.viewPhotoPageActionBar.visibility =
            if(binding.viewPhotoPageActionBar.visibility == View.VISIBLE)
                View.GONE else View.VISIBLE
    }
}
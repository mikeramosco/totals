package com.justanotherdeveloper.totalslite

import android.view.View

class ViewPhotoPageListeners(private val activity: ViewPhotoPageActivity) {

    private val binding = activity.getBinding()

    private var photoDownloaded = false

    init {
        initOnClickListeners()
    }

    private fun initOnClickListeners() {
        binding.backButton.setOnClickListener {
            activity.returnToPreviousPage()
        }

        binding.downloadPhotoButton.setOnClickListener {
            if(photoDownloaded)
                activity.showToast(activity.getString(R.string.photoAlreadyDownloadedMessage))
            else downloadPhoto()
        }

        binding.photoView.setOnClickListener {
            activity.toggleActionBarVisibility()
        }
    }

    private fun downloadPhoto() {
        binding.downloadPhotoButton.visibility = View.GONE
        binding.progressCircle.visibility = View.VISIBLE
        binding.progressCircle.post {
            val bitmapPhoto = activity.getBitmapPhoto()
            if(bitmapPhoto != null) photoDownloaded = activity.downloadPhoto(
                bitmapPhoto, binding.progressCircle, binding.downloadPhotoButton)
            else activity.showToast(activity.getString(R.string.unableToDownloadPhotoError))
        }
    }
}
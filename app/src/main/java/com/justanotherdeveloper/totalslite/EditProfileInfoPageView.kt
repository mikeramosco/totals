package com.justanotherdeveloper.totalslite

import android.net.Uri
import android.view.View

class EditProfileInfoPageView(private val activity: EditProfileInfoPageActivity) {

    private val binding = activity.getBinding()

    init {
        val totalsUser = getStaticSignedInTotalsUser(activity, activity.getDatabase())
        if(totalsUser != null) {
            totalsUser.displayOnView(activity, binding.profilePhotoImage, binding.profilePhotoLetter)
            binding.nameField.setText(totalsUser.getName())
            binding.usernameField.setText(totalsUser.getUsername())
            binding.profileBioField.setText(totalsUser.getProfileBio())
            binding.profilePhotoLayout.visibility = View.VISIBLE
        }
    }

    fun setProfilePhoto(imageURI: Uri, photoTaken: Boolean) {
        val bitmap = imageURI.toBitmap(activity).toSquareBitmap()
        binding.profilePhotoLetter.visibility = View.GONE
        binding.profilePhotoImage.visibility = View.GONE
        binding.profilePhotoImage.setImageBitmap(bitmap.toCircleBitmap())
        binding.profilePhotoImage.visibility = View.VISIBLE
        activity.setNewProfilePhoto(bitmap)
        if(photoTaken) imageURI.deleteFromStorage(activity.contentResolver)
    }

    fun setUsernameTakenErrorTextVisible(setVisible: Boolean = true) {
        binding.usernameTakenErrorText.visibility =
            if(setVisible) View.VISIBLE else View.GONE
    }

    fun setProfileInfoUpdateInProgress() {
        binding.backButtonDisabled.visibility = View.VISIBLE
        binding.backButton.visibility = View.GONE
        binding.progressCircle.visibility = View.VISIBLE
        binding.saveButton.visibility = View.GONE
    }

    fun setProfileInfoUpdateCancelled(showErrorMessage: Boolean = true) {
        binding.backButtonDisabled.visibility = View.GONE
        binding.backButton.visibility = View.VISIBLE
        binding.progressCircle.visibility = View.GONE
        binding.saveButton.visibility = View.VISIBLE
        if(showErrorMessage) activity.showRequestUnavailableToast()
    }

}
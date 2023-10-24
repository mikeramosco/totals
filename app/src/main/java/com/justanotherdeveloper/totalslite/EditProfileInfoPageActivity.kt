package com.justanotherdeveloper.totalslite

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.justanotherdeveloper.totalslite.databinding.ActivityEditProfileInfoPageBinding

class EditProfileInfoPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileInfoPageBinding

    private lateinit var database: TinyDB

    private lateinit var accountEditor: TotalsAccountEditor

    private lateinit var view: EditProfileInfoPageView
    private lateinit var listeners: EditProfileInfoPageListeners

    private lateinit var newProfileInfo: TotalsUser

    private val viewPhotosRequestCode = 0
    private val openCameraRequestCode = 1
    private val cameraPermissionRequestCode = 2

    private var imageURI: Uri? = null

    private var otherProcessStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileInfoPageBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        database = TinyDB(this)

        accountEditor = TotalsAccountEditor(editProfilePage = this)

        view = EditProfileInfoPageView(this)
        listeners = EditProfileInfoPageListeners(this)

        newProfileInfo = getSignedInTotalsUser(database)?: return
    }

    fun getBinding(): ActivityEditProfileInfoPageBinding {
        return binding
    }

    fun getDatabase(): TinyDB {
        return database
    }

    fun getView(): EditProfileInfoPageView {
        return view
    }

    fun returnToPreviousPage(profileInfoUpdated: Boolean = false) {
        if(profileInfoUpdated) {
            val intentData = Intent()
            intentData.putExtra(PROFILE_INFO_UPDATED_REF, true)
            setResult(Activity.RESULT_OK, intentData)
        }
        finish()
    }

    fun setNewProfilePhoto(newProfilePhoto: Bitmap) {
        newProfileInfo.setProfilePhotoUrl("NEW")
        newProfileInfo.setProfilePhotoBitmap(newProfilePhoto, false)
    }

    private fun updateProfileInfo(currentProfileInfo: TotalsUser) {
        val newProfileInfoBitmap = newProfileInfo.getProfilePhotoBitmap()
        if (currentProfileInfo.hasNewProfilePhoto(newProfileInfo)
            && newProfileInfoBitmap != null) {
            accountEditor.uploadNewProfilePhoto(newProfileInfoBitmap,
                currentProfileInfo, newProfileInfo)
        } else {
            val currentProfilePhoto = getStaticSignedInTotalsUser(
                this, database)?.getProfilePhotoBitmap()
            newProfileInfo.setProfilePhotoBitmap(currentProfilePhoto)
            accountEditor.updateProfileInfo(currentProfileInfo, newProfileInfo)
        }
    }

    fun continueCheckingForNewProfileInfo(newUsername: String = "") {
        if(newUsername.isNotEmpty()) newProfileInfo.setUsername(newUsername)
        val currentNameText = binding.nameField.text.toString()
        val currentBioText = binding.profileBioField.text.toString()
        if(newNameEntered(currentNameText)) newProfileInfo.setName(currentNameText)
        if(newBioEntered(currentBioText)) newProfileInfo.setProfileBio(currentBioText)
        val currentProfileInfo = getSignedInTotalsUser(database)?: return
        if(currentProfileInfo.hasNewData(newProfileInfo))
            updateProfileInfo(currentProfileInfo) else returnToPreviousPage()
    }

    fun checkForNewProfileInfo() {
        view.setProfileInfoUpdateInProgress()
        binding.nameField.formatEntry()
        binding.usernameField.toLowerCase()
        binding.profileBioField.formatEntry()
        val currentUsernameText = binding.usernameField.text.toString()
        if (newUsernameEntered(currentUsernameText)) {
            if(userMapContains(currentUsernameText)) {
                view.setUsernameTakenErrorTextVisible()
                view.setProfileInfoUpdateCancelled(false)
            } else accountEditor.checkAvailability(currentUsernameText)
        } else continueCheckingForNewProfileInfo()
    }

    private fun userMapContains(username: String): Boolean {
        for(user in getStaticUsersMap().values.iterator())
            if(user.getUsername() == username) return true
        return false
    }

    private fun newUsernameEntered(currentUsernameText: String): Boolean {
        val currentUsername = getStaticSignedInTotalsUser(
            this, database)?.getUsername() ?: return false
        return currentUsernameText != currentUsername
    }

    private fun newNameEntered(currentNameText: String): Boolean {
        if(currentNameText.isEmpty()) return false
        val currentName = getStaticSignedInTotalsUser(
            this, database)?.getName() ?: return false
        return currentNameText != currentName
    }

    private fun newBioEntered(currentBioText: String): Boolean {
        val currentBio = getStaticSignedInTotalsUser(
            this, database)?.getProfileBio() ?: return false
        return currentBioText != currentBio
    }

    private fun openCamera() {
        if(otherProcessStarted()) return
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, PHOTO_TITLE)
        values.put(MediaStore.Images.Media.DESCRIPTION, PHOTO_DESC)
        imageURI = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val openCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI)
        startActivityForResult(openCameraIntent, openCameraRequestCode)
    }

    fun openPhotos() {
        val viewPhotosIntent = Intent()
        viewPhotosIntent.type = "image/*"
        viewPhotosIntent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(viewPhotosIntent, viewPhotosRequestCode)
    }

    fun requestCameraPermission() {
        when(checkSelfPermission(Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            PackageManager.PERMISSION_DENIED -> {
                val permission = arrayOf(
                    Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                requestPermissions(permission, cameraPermissionRequestCode)
            }
        }
    }

    fun removeProfilePhoto() {
        newProfileInfo.removeProfilePhoto()
        newProfileInfo.displayOnView(this,
            binding.profilePhotoImage, binding.profilePhotoLetter)
    }

    private fun showConfirmCloseDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_confirm_close_page, null)
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmCloseDialog = builder.create()
        confirmCloseDialog.setCancelable(true)

        confirmCloseDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val keepEditingButton = view.findViewById<LinearLayout>(R.id.keepEditingButton)
        initButtonAnimationListener(keepEditingButton)
        keepEditingButton.setOnClickListener {
            confirmCloseDialog.cancel()
        }

        val closeButton = view.findViewById<LinearLayout>(R.id.closeButton)
        closeButton.setOnClickListener {
            confirmCloseDialog.cancel()
            returnToPreviousPage()
        }

        confirmCloseDialog.setOnCancelListener {
            setOtherProcessStarted(false)
            confirmCloseDialog.dismiss()
        }

        confirmCloseDialog.show()
    }

    private fun profileChangesMade(): Boolean {
        val currentUsernameText = binding.usernameField.text.toString()
        val currentNameText = binding.nameField.text.toString()
        val currentBioText = binding.profileBioField.text.toString()
        return newUsernameEntered(currentUsernameText)
                || newNameEntered(currentNameText)
                || newBioEntered(currentBioText)
                || getStaticSignedInTotalsUser(
            this, database)?.hasNewProfilePhoto(newProfileInfo)?: false
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == cameraPermissionRequestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        setOtherProcessStarted(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(viewPhotosRequestCode == requestCode) {
            if(data != null && data.data != null) {
                imageURI = data.data!!
                view.setProfilePhoto(imageURI!!, false)
            }
        } else if(openCameraRequestCode == requestCode && imageURI != null) {
            if(photoFileExists(imageURI!!, contentResolver))
                view.setProfilePhoto(imageURI!!, true)
            else imageURI!!.deleteFromStorage(contentResolver)
        }
        imageURI = null
    }

    override fun onBackPressed() {
        if(otherProcessStarted()) return
        if(profileChangesMade()) showConfirmCloseDialog()
        else if(accountEditor.accountManagementInProgress()) goToHomeScreen()
        else super.onBackPressed()
    }

    fun otherProcessStarted(setTrue: Boolean = true): Boolean {
        return if(otherProcessStarted) true else {
            if(setTrue) setOtherProcessStarted()
            false
        }
    }

    fun setOtherProcessStarted(otherProcessStarted: Boolean = true) {
        this.otherProcessStarted = otherProcessStarted
    }
}
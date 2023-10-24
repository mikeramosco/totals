package com.justanotherdeveloper.totalslite

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.view.isVisible
import com.justanotherdeveloper.totalslite.databinding.ActivityCreatePostPageBinding

class CreatePostPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostPageBinding

    private lateinit var local: TinyDB

    private lateinit var listeners: CreatePostPageListeners
    private lateinit var view: CreatePostPageView
    private lateinit var dialogs: CreatePostPageDialogs

    private lateinit var original: Goal
    private lateinit var progress: GoalProgress

    private lateinit var uploader: GoalUploader

    private var amountCompleted = 0

    private val cameraPermissionRequestCode = 0
    private val openCameraRequestCode = 1

    private var imageURI: Uri? = null

    private var otherProcessStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        val staticGoal = getStaticGoal()
        if(staticGoal != null) {
            original = staticGoal
            progress = staticGoal.createGoalProgress()
            setStaticGoal(null)

            progress.setOriginalAmount(original.getAmount())

            amountCompleted = intent.getIntExtra(AMOUNT_COMPLETED_REF, 0)
            progress.setDueDate(getCalendar(intent.getLongExtra(DUE_DATE_REF, 0)))

            local = TinyDB(this)
            uploader = GoalUploader(createPostPage = this)

            listeners = CreatePostPageListeners(this)
            view = CreatePostPageView(this)
            dialogs = CreatePostPageDialogs(this)

            requestCameraPermission()
        } else returnToHomePage()
    }

    /** Syntax to init binding */
    private fun initBinding() {
        binding = ActivityCreatePostPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun openCamera() {
        try {
            if(otherProcessStarted()) return
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, PHOTO_TITLE)
            values.put(MediaStore.Images.Media.DESCRIPTION, PHOTO_DESC)
            imageURI = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            val openCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI)
            startActivityForResult(openCameraIntent, openCameraRequestCode)
        } catch (e: SecurityException) {
            showToast(getString(R.string.permissionsRequiredMessage))
            returnToHomePage()
        }
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

    fun openViewPhotoPage() {
        if(otherProcessStarted()) return
        setBitmapPhotoToView(progress.getBitmapPhoto())
        setStaticTotalsUser(getSignedInTotalsUser(TinyDB(this)))
        val intent = Intent(this, ViewPhotoPageActivity::class.java)
        intent.putExtra(PHOTO_DETAILS_REF, getPhotoDetails())
        startActivity(intent)
    }

    private fun getPhotoDetails(): String {
        if(progress.hasCaption()) return progress.getCaption()
        val goalAmount = binding.goalAmountField.text.toString().removeFrontZeros()
            .ifEmpty { progress.getAmount().toString() }
        return if(goalAmount == "0") progress.getTitle()
        else "$goalAmount ${progress.getTitle()}"
    }

    fun postProgress() {
        if(!withinRequestsLimit()) return
        val goalAmountVisible = binding.goalAmountField.isVisible
        val goalAmount = binding.goalAmountField.text.toString().removeFrontZeros()
        if(goalAmountVisible && goalAmount.isEmpty())
            showToast(getString(R.string.goalAmountRequiredMessage))
        else {
            setStaticUsersPostsUpdated(false)
            view.disableNavButtons()
            if(goalAmountVisible)
                progress.setAmount(goalAmount.toInt())

            uploader.postProgress()
        }
    }

    fun confirmProgressPostedSuccess() {
        showToast(getString(R.string.progressPostedMessage))
        if(staticUsersPostsUpdated()) returnToHomePage(true)
        else setStaticCreatePostPageForPostedProgress(this)
    }

    fun returnToHomePage(progressPosted: Boolean = false) {
        otherProcessStarted = true
        if(progressPosted) {
            val intentData = Intent()
            intentData.putExtra(PROGRESS_POSTED_REF, true)
            intentData.putExtra(SELECTED_LABEL_REF, progress.getLabels().last())
            setResult(Activity.RESULT_OK, intentData)
        }
        finish()
    }

    fun deleteNote(noteIndex: Int, noteView: View) {
        progress.getNotes().removeAt(noteIndex)
        view.removeNoteView(noteView)
        view.updateAddNoteButton()
    }

    fun deleteLink(linkIndex: Int, linkView: View) {
        progress.getLinks().removeAt(linkIndex)
        view.removeLinkView(linkView)
        view.updateAddLinkButton()
    }

    fun setAsRecentLabel(labelTextString: String) {
        progress.getLabels().remove(labelTextString)
        progress.getLabels().add(labelTextString)
    }

    fun manageLabel(toAdd: Boolean, labelTextString: String) {
        if(toAdd) progress.addLabel(labelTextString)
        else progress.getLabels().remove(labelTextString)
    }

    fun setCaption(caption: String) {
        progress.setCaption(caption)
        view.setCaption(caption)
    }

    fun postIsLastOfDueDate(): Boolean {
        if(original.getAmount() == 0) return true
        return progress.getAmount() >= getAmountRemaining()
    }

    /** Override Methods */

    override fun onResume() {
        super.onResume()
        setOtherProcessStarted(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setOtherProcessStarted(false)
        if(requestCode == openCameraRequestCode) {
            if(imageURI != null) {
                if(photoFileExists(imageURI!!, contentResolver)) {
                    view.setBackgroundPhoto(imageURI!!)
                    progress.setBitmapPhoto(imageURI!!.toBitmap(this))
                } else if(!view.photoSet()) returnToHomePage()
                imageURI!!.deleteFromStorage(contentResolver)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == cameraPermissionRequestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openCamera()
            else {
                showToast(getString(R.string.permissionsRequiredMessage))
                returnToHomePage()
            }
        }
    }

    override fun onBackPressed() {
        if(otherProcessStarted()) return
        if(uploader.postingInProgress()) goToHomeScreen()
        else dialogs.showConfirmClosePageDialog()
    }

    /** "Get" Methods */

    fun getGoalProgress(): GoalProgress {
        return progress
    }

    fun getBinding(): ActivityCreatePostPageBinding {
        return binding
    }

    fun getView(): CreatePostPageView {
        return view
    }

    fun getDialogs(): CreatePostPageDialogs {
        return dialogs
    }

    fun getListeners(): CreatePostPageListeners {
        return listeners
    }

    fun getAmountRemaining(): Int {
        return original.getAmount() - amountCompleted
    }

    fun getDatabase(): TinyDB {
        return local
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
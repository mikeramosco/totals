package com.justanotherdeveloper.totalslite

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.ArrayList

class EditProfileInfoPageListeners(private val activity: EditProfileInfoPageActivity) {

    private val binding = activity.getBinding()

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
        initTextFieldListener()
    }

    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.backButton)
        initButtonAnimationListener(binding.saveButton)
    }

    private fun initOnClickListeners() {
        binding.backButton.setOnClickListener {
            activity.onBackPressed()
        }

        binding.saveButton.setOnClickListener {
            activity.checkForNewProfileInfo()
        }

        binding.profilePhotoLayout.setOnClickListener {
            showChoosePhotoDialog()
        }
    }

    private fun showChoosePhotoDialog() {
        if(activity.otherProcessStarted()) return
        val choosePhotoDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(
            R.layout.bottomsheet_choose_photo, null)
        choosePhotoDialog.setContentView(view)

        val dialogOptions = ArrayList<LinearLayout>()
        dialogOptions.add(view.findViewById(R.id.fromCameraOption))
        dialogOptions.add(view.findViewById(R.id.fromGalleryOption))
        dialogOptions.add(view.findViewById(R.id.removePhotoOption))
        initDialogOptions(choosePhotoDialog, dialogOptions)

        if(!binding.profilePhotoImage.isVisible)
            dialogOptions[2].visibility = View.GONE

        fun clickOption(option: LinearLayout) {
            activity.setOtherProcessStarted(false)
            when(option) {
                dialogOptions[0] -> activity.requestCameraPermission()
                dialogOptions[1] -> activity.openPhotos()
                dialogOptions[2] -> activity.removeProfilePhoto()
            }

            choosePhotoDialog.cancel()
        }

        for(option in dialogOptions)
            option.setOnClickListener { clickOption(option) }

        choosePhotoDialog.setOnCancelListener {
            activity.setOtherProcessStarted(false)
            choosePhotoDialog.dismiss()
        }

        choosePhotoDialog.show()
    }

    private fun initTextFieldListener() {
        binding.usernameField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                activity.getView().setUsernameTakenErrorTextVisible(false)
            }
        })
    }

}
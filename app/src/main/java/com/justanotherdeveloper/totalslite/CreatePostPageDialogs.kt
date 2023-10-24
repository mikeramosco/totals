package com.justanotherdeveloper.totalslite

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.ArrayList

class CreatePostPageDialogs(private val activity: CreatePostPageActivity) {

    fun openAddCaptionDialog(currentCaption: String) {
        if(activity.otherProcessStarted()) return
        val addCaptionDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.bottomsheet_add_label, null)
        addCaptionDialog.setContentView(view)

        val labelField = view.findViewById<EditText>(R.id.newLabelField)
        val captionField = view.findViewById<EditText>(R.id.captionField)
        labelField.visibility = View.GONE
        captionField.visibility = View.VISIBLE

        val addButtonBackground = view.findViewById<ImageView>(R.id.addButtonBackground)
        val addButton = view.findViewById<LinearLayout>(R.id.addButton)
        val addButtonIcon = view.findViewById<ImageView>(R.id.addButtonIcon)

        addButton.visibility = View.VISIBLE
        addButtonBackground.setImageResource(R.drawable.ic_circle_filled_colored)
        addButtonIcon.setImageResource(R.drawable.ic_check)

        val dialogOptions = ArrayList<LinearLayout>()
        dialogOptions.add(addButton)
        initDialogOptions(addCaptionDialog, dialogOptions)

        fun addButtonClicked() {
            val caption = captionField.text.toString()
            activity.setCaption(caption)
            addCaptionDialog.cancel()
        }

        addButton.setOnClickListener {
            addButtonClicked()
        }

        captionField.setOnEditorActionListener { _, actionId, _ ->
            val donePressed = actionId == EditorInfo.IME_ACTION_DONE
            if(donePressed) addButtonClicked()
            donePressed
        }

        addCaptionDialog.setOnCancelListener {
            activity.setOtherProcessStarted(false)
            addCaptionDialog.dismiss()
        }

        captionField.setText(currentCaption)
        captionField.requestFocus()
        addCaptionDialog.show()
    }

    fun manageLabelsDialogClosed(currentLabelsString: String) {
        activity.setOtherProcessStarted(false)
        val newLabelsString = activity.getGoalProgress().getLabels().toString()
        if(newLabelsString != currentLabelsString) {
            updateGoalLabels(activity.getGoalProgress(), activity.getDatabase())
            activity.getView().updateLabels()
        }
    }

    fun addLinkDialogClosed() {
        activity.setOtherProcessStarted(false)
    }

    fun addLinkButtonClicked(linkField: EditText) {
        val linkText = linkField.text.toString()
        activity.getGoalProgress().getLinks().add(linkText)
        activity.getView().addLinkView(linkText)
        activity.getView().updateAddLinkButton()
    }

    fun showConfirmClosePageDialog() {
        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_close_page, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmClosePageDialog = builder.create()
        confirmClosePageDialog.setCancelable(true)

        confirmClosePageDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val detailsText = view.findViewById<TextView>(R.id.detailsText)
        detailsText.text = activity.getString(R.string.closeCreatePostPageDialogDetails)

        val keepEditingButton = view.findViewById<LinearLayout>(R.id.keepEditingButton)
        initButtonAnimationListener(keepEditingButton)
        keepEditingButton.setOnClickListener {
            confirmClosePageDialog.cancel()
        }

        val closeButton = view.findViewById<LinearLayout>(R.id.closeButton)
        closeButton.setOnClickListener {
            activity.returnToHomePage()
            confirmClosePageDialog.cancel()
        }

        confirmClosePageDialog.setOnCancelListener {
            activity.setOtherProcessStarted(false)
            confirmClosePageDialog.dismiss()
        }

        confirmClosePageDialog.show()
    }

    fun noteDialogClosed(noteField: EditText, noteView: View?, noteIndex: Int) {
        activity.setOtherProcessStarted(false)
        val noteText = noteField.text.toString()
        if(noteView == null) {
            if(noteText.isNotEmpty()) {
                activity.getGoalProgress().getNotes().add(noteText)
                activity.getView().addNoteView(noteText)
                activity.getView().updateAddNoteButton()
            }
        } else {
            if(noteText.isEmpty()) activity.deleteNote(noteIndex, noteView)
            else {
                activity.getGoalProgress().getNotes()[noteIndex] = noteText
                updateNoteView(noteView, noteText)
            }
        }
    }

}
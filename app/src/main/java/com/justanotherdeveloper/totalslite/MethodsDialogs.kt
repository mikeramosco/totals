package com.justanotherdeveloper.totalslite

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.iterator
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.ArrayList

fun Activity.showRemoveLabelDialog(goal: Goal, label: String, labelView: View,
                                   editGoalPageView: EditActivityPageView? = null,
                                   createPostPageView: CreatePostPageView? = null) {
    if(goal.getLabels().size == 1) {
        showUnableToRemoveLabelDialog()
        return
    }

    val view = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
    val builder = AlertDialog.Builder(this)
    builder.setCancelable(false)
    builder.setView(view)
    val confirmDeleteDialog = builder.create()
    confirmDeleteDialog.setCancelable(true)

    confirmDeleteDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

    val deleteButton = view.findViewById<LinearLayout>(R.id.deleteButton)
    initButtonAnimationListener(deleteButton)
    deleteButton.setOnClickListener {
        editGoalPageView?.removeLabel(label, labelView)
        createPostPageView?.removeLabel(label, labelView)
        confirmDeleteDialog.dismiss()
    }

    val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
    cancelButton.setOnClickListener {
        confirmDeleteDialog.dismiss()
    }

    confirmDeleteDialog.show()
}

private fun Activity.showUnableToRemoveLabelDialog() {
    val view = layoutInflater.inflate(R.layout.dialog_unable_to_remove_label_message, null)
    val builder = AlertDialog.Builder(this)
    builder.setCancelable(false)
    builder.setView(view)
    val unableToRemoveLabelDialog = builder.create()
    unableToRemoveLabelDialog.setCancelable(true)

    unableToRemoveLabelDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

    val okButton = view.findViewById<LinearLayout>(R.id.okButton)
    initButtonAnimationListener(okButton)
    okButton.setOnClickListener {
        unableToRemoveLabelDialog.dismiss()
    }

    unableToRemoveLabelDialog.show()
}

fun Activity.showManageLabelsDialog(goal: Goal, addLabelClicked: Boolean = false,
                                    editGoalPage: EditActivityPageActivity? = null,
                                    createPostPage: CreatePostPageActivity? = null) {
    val manageLabelsDialog = BottomSheetDialog(this)
    val view = layoutInflater.inflate(R.layout.bottomsheet_manage_goal_labels, null)
    manageLabelsDialog.setContentView(view)

    val currentLabelsString = goal.getLabels().toString()

    val testLabels = view.findViewById<LinearLayout>(R.id.testLabels)
    testLabels.visibility = View.GONE

    val addLabelOption = view.findViewById<LinearLayout>(R.id.addLabelOption)
    val labelsContainer = view.findViewById<LinearLayout>(R.id.labelsContainer)
    val labelsScrollView = view.findViewById<HorizontalScrollView>(R.id.labelsScrollView)

    val scrollViewButtons = ButtonAnimationDialogHorizontalScrollView(
        labelsScrollView, manageLabelsDialog)
    scrollViewButtons.addDialogOption(addLabelOption)
    addLabelOption.setOnClickListener {
        showAddLabelDialog(labelsContainer, scrollViewButtons, editGoalPage, createPostPage)
    }

    val goalLabels = goal.getLabels()

    for(label in goalLabels) {
        val labelView = createLabelView(label, true, labelsContainer,
            scrollViewButtons, editGoalPage, createPostPage)
        labelsContainer.addView(labelView, 0)
    }

    val unselectedLabels = ArrayList<String>()
    val unselectedViewsMap = HashMap<String, View>()
    for(label in getLabels(TinyDB(this)))
        if(!goalLabels.contains(label)) {
            val labelView = createLabelView(label, false,
                labelsContainer, scrollViewButtons, editGoalPage, createPostPage)
            unselectedLabels.add(label)
            unselectedViewsMap[label] = labelView
        }
    addAlphabetizedUnselectedLabels(labelsContainer, unselectedLabels, unselectedViewsMap)

    manageLabelsDialog.setOnCancelListener {
        editGoalPage?.getDialogs()?.manageLabelsDialogClosed(currentLabelsString)
        createPostPage?.getDialogs()?.manageLabelsDialogClosed(currentLabelsString)
    }

    manageLabelsDialog.show()

    if(addLabelClicked)
        showAddLabelDialog(labelsContainer, scrollViewButtons, editGoalPage, createPostPage)
}

private fun addAlphabetizedUnselectedLabels(labelsContainer: LinearLayout,
                                            unselectedLabels: ArrayList<String>,
                                            unselectedViewsMap: HashMap<String, View>) {
    unselectedLabels.sort()
    for(label in unselectedLabels) {
        val labelView = unselectedViewsMap[label]
        if(labelView != null) labelsContainer.addView(labelView)
    }
}

private fun Activity.showAddLabelDialog(labelsContainer: LinearLayout,
                                        scrollViewButtons: ButtonAnimationDialogHorizontalScrollView,
                                        editGoalPage: EditActivityPageActivity?,
                                        createPostPage: CreatePostPageActivity?) {
    val addLabelDialog = BottomSheetDialog(this)
    val view = layoutInflater.inflate(R.layout.bottomsheet_add_label, null)
    addLabelDialog.setContentView(view)

    val newLabelField = view.findViewById<EditText>(R.id.newLabelField)
    val addButtonBackground = view.findViewById<ImageView>(R.id.addButtonBackground)
    val addButton = view.findViewById<LinearLayout>(R.id.addButton)
    val addButtonIcon = view.findViewById<ImageView>(R.id.addButtonIcon)

    val dialogOptions = ArrayList<LinearLayout>()
    dialogOptions.add(addButton)
    initDialogOptions(addLabelDialog, dialogOptions)

    fun disableAddButton() {
        addButton.visibility = View.GONE
        addButtonBackground.setImageResource(R.drawable.ic_circle_filled_disabled_dialog)
        addButtonIcon.setImageResource(R.drawable.ic_add_gray_dark)
    }

    fun enableAddButton() {
        addButton.visibility = View.VISIBLE
        addButtonBackground.setImageResource(R.drawable.ic_circle_filled_colored)
        addButtonIcon.setImageResource(R.drawable.ic_add)
    }

    disableAddButton()
    newLabelField.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int,
                                       count: Int, after: Int) { }
        override fun onTextChanged(s: CharSequence, start: Int,
                                   before: Int, count: Int) {
            val newLabel = newLabelField.text.toString()
            if(newLabel.isEmpty())
                disableAddButton() else enableAddButton()
        }
    })

    fun addButtonClicked() {
        addLabelDialog.cancel()
        val addedLabel = newLabelField.text.toString()
        var labelExists = false
        for(labelView in labelsContainer.iterator()) {
            val labelText = labelView.findViewById<TextView>(R.id.labelText)
            if(labelText != null) {
                val labelTextString = labelText.text.toString()
                if(addedLabel.equals(labelTextString, true)) {
                    labelExists = true
                    labelsContainer.removeView(labelView)
                    labelsContainer.addView(labelView, 0)
                    val labelIcon = labelView.findViewById<ImageView>(R.id.labelIcon)
                    selectLabel(labelText, labelIcon, editGoalPage, createPostPage)
                    editGoalPage?.setAsRecentLabel(labelTextString)
                    createPostPage?.setAsRecentLabel(labelTextString)
                    break
                }
            }
        }

        if(labelExists) return
        val labelView = createLabelView(addedLabel, true,
            labelsContainer, scrollViewButtons, editGoalPage, createPostPage)
        labelsContainer.addView(labelView, 0)
    }

    addButton.setOnClickListener {
        addButtonClicked()
    }

    newLabelField.setOnEditorActionListener { _, actionId, _ ->
        val donePressed = actionId == EditorInfo.IME_ACTION_DONE
        if(donePressed && newLabelField.text.isNotEmpty()) addButtonClicked()
        donePressed
    }

    newLabelField.requestFocus()
    addLabelDialog.show()
}



private fun Activity.selectLabel(labelText: TextView, labelIcon: ImageView,
                                 editGoalPage: EditActivityPageActivity?,
                                 createPostPage: CreatePostPageActivity?) {
    labelText.setTextColor(getValuesColor(R.color.color_theme))
    labelIcon.setImageResource(R.drawable.ic_check_theme)

    editGoalPage?.manageLabel(true, labelText.text.toString())
    createPostPage?.manageLabel(true, labelText.text.toString())
}

private fun Activity.deselectLabel(labelText: TextView, labelIcon: ImageView,
                                   editGoalPage: EditActivityPageActivity?,
                                   createPostPage: CreatePostPageActivity?) {
    val goal = editGoalPage?.getGoal()?: createPostPage?.getGoalProgress()?: return
    if(goal.getLabels().size == 1) return
    labelText.setTextColor(getValuesColor(R.color.white))
    labelIcon.setImageResource(R.drawable.ic_label)

    editGoalPage?.manageLabel(false, labelText.text.toString())
    createPostPage?.manageLabel(false, labelText.text.toString())
}

private fun Activity.createLabelView(label: String, isSelected: Boolean, labelsContainer: LinearLayout,
                                     scrollViewButtons: ButtonAnimationDialogHorizontalScrollView,
                                     editGoalPage: EditActivityPageActivity?,
                                     createPostPage: CreatePostPageActivity?): View {
    val labelView = layoutInflater.inflate(R.layout.view_label_dialog, null)
    val labelButton = labelView.findViewById<LinearLayout>(R.id.labelButton)
    val labelText = labelView.findViewById<TextView>(R.id.labelText)
    val labelIcon = labelView.findViewById<ImageView>(R.id.labelIcon)

    labelText.text = label
    if(isSelected) selectLabel(labelText, labelIcon, editGoalPage, createPostPage)

    labelButton.setOnClickListener {
        val goal = editGoalPage?.getGoal()?: createPostPage?.getGoalProgress()
        if(goal != null) {
            if (goal.getLabels().contains(label))
                deselectLabel(labelText, labelIcon, editGoalPage, createPostPage)
            else selectLabel(labelText, labelIcon, editGoalPage, createPostPage)
        }
    }

    scrollViewButtons.addDialogOption(labelButton)

    return labelView

//    if(isSelected) labelsContainer.addView(labelView, 0)
//    else labelsContainer.addView(labelView)
}

fun Activity.openAddLinkDialog(editGoalPageDialogs: EditActivityPageDialogs? = null,
                               createPostPageDialogs: CreatePostPageDialogs? = null,
                               seeAttachmentsPage: SeeAttachmentsActivity? = null) {
    val addLinkDialog = BottomSheetDialog(this)
    val view = layoutInflater.inflate(R.layout.bottomsheet_add_label, null)
    addLinkDialog.setContentView(view)

    val labelField = view.findViewById<EditText>(R.id.newLabelField)
    val linkField = view.findViewById<EditText>(R.id.linkField)
    labelField.visibility = View.GONE
    linkField.visibility = View.VISIBLE

    val addButtonBackground = view.findViewById<ImageView>(R.id.addButtonBackground)
    val addButton = view.findViewById<LinearLayout>(R.id.addButton)
    val addButtonIcon = view.findViewById<ImageView>(R.id.addButtonIcon)

    val dialogOptions = ArrayList<LinearLayout>()
    dialogOptions.add(addButton)
    initDialogOptions(addLinkDialog, dialogOptions)

    fun disableAddButton() {
        addButton.visibility = View.GONE
        addButtonBackground.setImageResource(R.drawable.ic_circle_filled_disabled_dialog)
        addButtonIcon.setImageResource(R.drawable.ic_add_gray_dark)
    }

    fun enableAddButton() {
        addButton.visibility = View.VISIBLE
        addButtonBackground.setImageResource(R.drawable.ic_circle_filled_colored)
        addButtonIcon.setImageResource(R.drawable.ic_add)
    }

    disableAddButton()
    linkField.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int,
                                       count: Int, after: Int) { }
        override fun onTextChanged(s: CharSequence, start: Int,
                                   before: Int, count: Int) {
            val newLabel = linkField.text.toString()
            if(newLabel.isEmpty())
                disableAddButton() else enableAddButton()
        }
    })

    fun addButtonClicked() {
        editGoalPageDialogs?.addLinkButtonClicked(linkField)
        createPostPageDialogs?.addLinkButtonClicked(linkField)
        seeAttachmentsPage?.addLinkButtonClicked(linkField)
        addLinkDialog.cancel()
    }

    addButton.setOnClickListener {
        addButtonClicked()
    }

    linkField.setOnEditorActionListener { _, actionId, _ ->
        val donePressed = actionId == EditorInfo.IME_ACTION_DONE
        if(donePressed && linkField.text.isNotEmpty()) addButtonClicked()
        donePressed
    }

    addLinkDialog.setOnCancelListener {
        editGoalPageDialogs?.addLinkDialogClosed()
        createPostPageDialogs?.addLinkDialogClosed()
        seeAttachmentsPage?.addLinkDialogClosed()
        addLinkDialog.dismiss()
    }

    linkField.requestFocus()
    addLinkDialog.show()
}

fun Activity.showDeleteNoteDialog(noteIndex: Int, noteView: View,
                                  editGoalPage: EditActivityPageActivity? = null,
                                  createPostPage: CreatePostPageActivity? = null,
                                  seeAttachmentsPage: SeeAttachmentsActivity? = null) {
    val view = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
    val builder = AlertDialog.Builder(this)
    builder.setCancelable(false)
    builder.setView(view)
    val confirmDeleteDialog = builder.create()
    confirmDeleteDialog.setCancelable(true)

    confirmDeleteDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

    val promptText = view.findViewById<TextView>(R.id.promptText)
    promptText.text = getString(R.string.deleteNotePrompt)

    val deleteText = view.findViewById<TextView>(R.id.deleteText)
    deleteText.text = getString(R.string.option_delete)

    val deleteButton = view.findViewById<LinearLayout>(R.id.deleteButton)
    initButtonAnimationListener(deleteButton)
    deleteButton.setOnClickListener {
        editGoalPage?.deleteNote(noteIndex, noteView)
        createPostPage?.deleteNote(noteIndex, noteView)
        seeAttachmentsPage?.deleteNote(noteIndex, noteView)
        confirmDeleteDialog.dismiss()
    }

    val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
    cancelButton.setOnClickListener {
        confirmDeleteDialog.dismiss()
    }

    confirmDeleteDialog.show()
}

fun Activity.openNoteDialog(goal: Goal, noteIndex: Int = SENTINEL, noteView: View? = null,
                            editGoalPageDialogs: EditActivityPageDialogs? = null,
                            createPostPageDialogs: CreatePostPageDialogs? = null,
                            seeAttachmentsPage: SeeAttachmentsActivity? = null,
                            toManageAttachments: Boolean = true) {
    val view = layoutInflater.inflate(R.layout.dialog_view_note, null)
    val builder = AlertDialog.Builder(this)
    builder.setCancelable(false)
    builder.setView(view)
    val noteDialog = builder.create()
    noteDialog.setCancelable(true)

    noteDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

    val noteField = view.findViewById<EditText>(R.id.noteField)
    if(noteIndex != SENTINEL) noteField.setText(goal.getNotes()[noteIndex])

    val saveButton = view.findViewById<LinearLayout>(R.id.saveButton)
    saveButton.setOnClickListener { noteDialog.cancel() }
    initButtonAnimationListener(saveButton)

    noteField.setOnEditorActionListener { _, actionId, _ ->
        val donePressed = actionId == EditorInfo.IME_ACTION_DONE
        if(donePressed) noteDialog.cancel()
        donePressed
    }

    noteDialog.setOnCancelListener {
        editGoalPageDialogs?.noteDialogClosed(noteField, noteView, noteIndex)
        createPostPageDialogs?.noteDialogClosed(noteField, noteView, noteIndex)
        seeAttachmentsPage?.noteDialogClosed(noteField, noteView, noteIndex)
    }

    if(!toManageAttachments)
        noteField.isFocusable = false
    else if(noteView == null)
        noteField.requestFocus()
    noteDialog.show()
}

fun Activity.showDeleteLinkDialog(linkIndex: Int, linkView: View,
                                  editGoalPage: EditActivityPageActivity? = null,
                                  createPostPage: CreatePostPageActivity? = null,
                                  seeAttachmentsPage: SeeAttachmentsActivity? = null) {
    val view = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
    val builder = AlertDialog.Builder(this)
    builder.setCancelable(false)
    builder.setView(view)
    val confirmDeleteDialog = builder.create()
    confirmDeleteDialog.setCancelable(true)

    confirmDeleteDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

    val promptText = view.findViewById<TextView>(R.id.promptText)
    promptText.text = getString(R.string.deleteLinkPrompt)

    val deleteText = view.findViewById<TextView>(R.id.deleteText)
    deleteText.text = getString(R.string.option_delete)

    val deleteButton = view.findViewById<LinearLayout>(R.id.deleteButton)
    initButtonAnimationListener(deleteButton)
    deleteButton.setOnClickListener {
        editGoalPage?.deleteLink(linkIndex, linkView)
        createPostPage?.deleteLink(linkIndex, linkView)
        seeAttachmentsPage?.deleteLink(linkIndex, linkView)
        confirmDeleteDialog.dismiss()
    }

    val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
    cancelButton.setOnClickListener {
        confirmDeleteDialog.dismiss()
    }

    confirmDeleteDialog.show()
}

fun openAddIncompletePostCaptionDialog(activity: Activity,
                                       addACaptionOption: TextView,
                                       captionText: TextView,
                                       goalPage: MePageFragment? = null,
                                       incompletePost: GoalProgress? = null) {
    val addAddDialog = BottomSheetDialog(activity)
    val view = activity.layoutInflater.inflate(R.layout.bottomsheet_add_label, null)
    addAddDialog.setContentView(view)

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
    initDialogOptions(addAddDialog, dialogOptions)

    fun setCaption(caption: String) {
        captionText.text = caption
        if(caption.isEmpty()) {
            addACaptionOption.visibility = View.VISIBLE
            captionText.visibility = View.GONE
        } else {
            addACaptionOption.visibility = View.GONE
            captionText.visibility = View.VISIBLE
        }
    }

    fun addButtonClicked() {
        val caption = captionField.text.toString()
        goalPage?.getIncompletePost()?.setCaption(caption)
        incompletePost?.setCaption(caption)
        setCaption(caption)
        addAddDialog.cancel()
    }

    addButton.setOnClickListener {
        addButtonClicked()
    }

    captionField.setOnEditorActionListener { _, actionId, _ ->
        val donePressed = actionId == EditorInfo.IME_ACTION_DONE
        if(donePressed) addButtonClicked()
        donePressed
    }

    val currentCaption = goalPage?.getIncompletePost()?.getCaption()
        ?: incompletePost?.getCaption()?: ""
    captionField.setText(currentCaption)
    captionField.requestFocus()
    addAddDialog.show()
}
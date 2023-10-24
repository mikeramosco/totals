package com.justanotherdeveloper.totalslite

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.justanotherdeveloper.totalslite.databinding.ActivitySelectLabelBinding

class SelectLabelActivity : AppCompatActivity() {

    /** 'binding' allows reference calls to activity views */
    private lateinit var binding: ActivitySelectLabelBinding

    private lateinit var listeners: SelectLabelListeners

    private lateinit var local: TinyDB

    private lateinit var uploader: GoalUploader

    private var labelsWithNotifs = ArrayList<String>()
    private var toUpdatePost = false
    private var original: GoalProgress? = null
    private var post: GoalProgress? = null
    private var selectedCheckboxIcon: ImageView? = null

    private var otherProcessStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        local = TinyDB(this)
        listeners = SelectLabelListeners(this)

        toUpdatePost = intent.getBooleanExtra(TO_UPDATE_POST_REF, false)
        if(toUpdatePost) {
            binding.saveButton.visibility = View.VISIBLE
            binding.selectLabelTitleText.text = getString(R.string.manageLabelsTitle)
            addLabelViewsToManage()
        } else addLabelViewsToSelect()
        if(toUpdateFirebase())
            uploader = GoalUploader(selectLabelPage = this)
    }

    fun saveButtonClicked() {
        val post = post
        if(post == null || noChangesMade() || !toUpdateFirebase())
            returnToPreviousPage()
        else {
            if(!withinRequestsLimit()) return
            binding.saveButton.visibility = View.GONE
            binding.backButton.visibility = View.GONE
            binding.backButtonDisabled.visibility = View.VISIBLE
            binding.progressCircle.visibility = View.VISIBLE
            uploader.updatePostLabels(post)
        }
    }

    fun confirmLabelsUpdatedSuccess() {
        showToast(getString(R.string.labelsUpdatedMessage))
        returnToPreviousPage()
    }

    fun notifyUpdateFailure() {
        binding.saveButton.visibility = View.VISIBLE
        binding.backButton.visibility = View.VISIBLE
        binding.backButtonDisabled.visibility = View.GONE
        binding.progressCircle.visibility = View.GONE
        showRequestUnavailableToast()
    }

    private fun returnToPreviousPage() {
        setStaticProgress(post)
        if(!noChangesMade()) {
            val intentData = Intent()
            intentData.putExtra(TO_UPDATE_POST_REF, true)
            setResult(Activity.RESULT_OK, intentData)
        }
        finish()
    }

    private fun initBinding() {
        binding = ActivitySelectLabelBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun selectLabel(selectedLabel: String, checkbox: ImageView) {
        selectedCheckboxIcon?.setImageResource(R.drawable.ic_radio_button_unchecked)
        checkbox.setImageResource(R.drawable.ic_radio_button_checked)
        val intentData = Intent()
        intentData.putExtra(FOR_PROGRESS_PAGE_REF, forProgressPage())
        intentData.putExtra(SELECTED_LABEL_REF, selectedLabel)
        setResult(Activity.RESULT_OK, intentData)
        finish()
    }

    private fun forProgressPage(): Boolean {
        return intent.getBooleanExtra(FOR_PROGRESS_PAGE_REF, false)
    }

    private fun deselectLabelForPost(label: String, labelText: TextView,
                                     labelIcon: ImageView, checkbox: ImageView) {
        val labels = post?.getLabels()
        if(labels == null || labels.size == 1) return
        labelText.setTextColor(getValuesColor(R.color.white))
        labelIcon.setImageResource(R.drawable.ic_label)
        checkbox.setImageResource(R.drawable.ic_check_box_outline_blank_white)

        post?.getLabels()?.remove(label)
    }

    private fun selectLabelForPost(label: String, labelText: TextView,
                                   labelIcon: ImageView, checkbox: ImageView) {
        labelText.setTextColor(getValuesColor(R.color.color_theme))
        labelIcon.setImageResource(R.drawable.ic_label_theme)
        checkbox.setImageResource(R.drawable.ic_check_box_checked_theme)

        post?.addLabel(label)
    }

    private fun createManageLabelView(label: String, isSelected: Boolean = false): View {
        val labelView = layoutInflater.inflate(R.layout.view_label_manage, null)
        val labelText = labelView.findViewById<TextView>(R.id.labelText)
        labelText.text = label

        val labelButton = labelView.findViewById<LinearLayout>(R.id.labelButton)
        val labelIcon = labelView.findViewById<ImageView>(R.id.labelIcon)
        val checkbox = labelView.findViewById<ImageView>(R.id.checkbox)
        listeners.getScrollViewButtonAnimation().addButton(labelButton)
        labelButton.setOnClickListener {
            val labels = post?.getLabels()
            if (labels != null && labels.contains(label))
                deselectLabelForPost(label, labelText, labelIcon, checkbox)
            else selectLabelForPost(label, labelText, labelIcon, checkbox)
        }

        if(isSelected) {
            labelText.setTextColor(getValuesColor(R.color.color_theme))
            labelIcon.setImageResource(R.drawable.ic_label_theme)
            checkbox.setImageResource(R.drawable.ic_check_box_checked_theme)
        }

        return labelView
    }

    private fun addLabelViewsToManage() {
        val progress = getStaticProgress()
        if(progress == null) { finish(); return }
        setStaticProgress(null)
        original = progress.createProgressClone()
        post = progress.createProgressClone()

        for(label in progress.getLabels()) {
            val labelView = createManageLabelView(label, true)
            binding.labelsContents.addView(labelView, 0)
        }

        val unselectedLabels = ArrayList<String>()
        val unselectedViewsMap = HashMap<String, View>()
        for(label in getLabels())
            if(!progress.getLabels().contains(label)) {
                val labelView = createManageLabelView(label)
                unselectedLabels.add(label)
                unselectedViewsMap[label] = labelView
            }
        addAlphabetizedUnselectedLabels(unselectedLabels, unselectedViewsMap)
    }

    private fun addLabelViewsToSelect() {
        val selectedLabel = intent.getStringExtra(SELECTED_LABEL_REF) ?: ""

        val intentLabelsWithNotifs = intent.getStringArrayListExtra(LABELS_WITH_NOTIFS_REF)
        if(intentLabelsWithNotifs != null) labelsWithNotifs = intentLabelsWithNotifs

        val allGoalsView = layoutInflater.inflate(R.layout.view_label_option, null)
        val allGoalsViewSelected = selectedLabel.isEmpty()

        val forProgressPage = forProgressPage()
        if(!forProgressPage) {
            val allGoalsText = allGoalsView.findViewById<TextView>(R.id.labelText)
            val allGoalsString = getString(R.string.allGoalsFilterOption)
            allGoalsText.text = allGoalsString

            val labelIcon = allGoalsView.findViewById<ImageView>(R.id.labelIcon)
            labelIcon.setImageResource(R.drawable.ic_all)

            val checkbox = allGoalsView.findViewById<ImageView>(R.id.checkbox)
            val allGoalsButton = allGoalsView.findViewById<LinearLayout>(R.id.labelButton)
            listeners.getScrollViewButtonAnimation().addButton(allGoalsButton)
            allGoalsButton.setOnClickListener { selectLabel("", checkbox) }

            if(labelsWithNotifs.isNotEmpty() || getStaticNewLikesMap().isNotEmpty()) {
                val labelNotifCircle = allGoalsView.findViewById<ImageView>(R.id.labelNotifCircle)
                labelNotifCircle.visibility = View.VISIBLE
            }

            if (allGoalsViewSelected) {
                selectedCheckboxIcon = checkbox
                checkbox.setImageResource(R.drawable.ic_radio_button_checked)
                binding.labelsContents.addView(allGoalsView)
            }
        }

        val unselectedLabels = ArrayList<String>()
        val unselectedViewsMap = HashMap<String, View>()
        for(label in getLabels()) {
            val labelView = layoutInflater.inflate(R.layout.view_label_option, null)
            val labelText = labelView.findViewById<TextView>(R.id.labelText)
            labelText.text = label

            val checkbox = labelView.findViewById<ImageView>(R.id.checkbox)
            val labelButton = labelView.findViewById<LinearLayout>(R.id.labelButton)
            listeners.getScrollViewButtonAnimation().addButton(labelButton)
            labelButton.setOnClickListener { selectLabel(label, checkbox) }

            if(!forProgressPage && (labelsWithNotifs.contains(label) || labelHasNewLikes(label))) {
                val labelNotifCircle = labelView.findViewById<ImageView>(R.id.labelNotifCircle)
                labelNotifCircle.visibility = View.VISIBLE
            }

            if(selectedLabel == label) {
                selectedCheckboxIcon = checkbox
                checkbox.setImageResource(R.drawable.ic_radio_button_checked)
                binding.labelsContents.addView(labelView)
            } else {
                unselectedLabels.add(label)
                unselectedViewsMap[label] = labelView
            }
        }
        if(!allGoalsViewSelected) binding.labelsContents.addView(allGoalsView)
        addAlphabetizedUnselectedLabels(unselectedLabels, unselectedViewsMap)
    }

    private fun labelHasNewLikes(label: String): Boolean {
        for(post in getStaticNewLikesMap().keys.iterator()) {
            if(post.getLabels().contains(label)) return true
        }
        return false
    }

    private fun addAlphabetizedUnselectedLabels(unselectedLabels: ArrayList<String>,
                                                unselectedViewsMap: HashMap<String, View>) {
        unselectedLabels.sort()
        for(label in unselectedLabels) {
            val labelView = unselectedViewsMap[label]
            if(labelView != null) binding.labelsContents.addView(labelView)
        }
    }

    private fun getLabels(): ArrayList<String> {
        val labels = local.getListString(GOAL_LABELS_REF)?: ArrayList()
        val defaultLabel = getString(R.string.myGoalsProfileTitle)
        if(labels.isEmpty()) labels.add(defaultLabel)
        return labels
    }

    private fun showConfirmClosePageDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_confirm_close_page, null)
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmClosePageDialog = builder.create()
        confirmClosePageDialog.setCancelable(true)

        confirmClosePageDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val detailsText = view.findViewById<TextView>(R.id.detailsText)
        detailsText.text = getString(R.string.closeManageLabelsPageDialogDetails)

        val keepEditingButton = view.findViewById<LinearLayout>(R.id.keepEditingButton)
        initButtonAnimationListener(keepEditingButton)
        keepEditingButton.setOnClickListener {
            confirmClosePageDialog.cancel()
        }

        val closeButton = view.findViewById<LinearLayout>(R.id.closeButton)
        closeButton.setOnClickListener {
            finish()
            confirmClosePageDialog.cancel()
        }

        confirmClosePageDialog.setOnCancelListener {
            setOtherProcessStarted(false)
            confirmClosePageDialog.dismiss()
        }

        confirmClosePageDialog.show()
    }

    private fun toUpdateFirebase(): Boolean {
        return intent.getBooleanExtra(UPDATE_FIREBASE_REF, false)
    }

    private fun noChangesMade(): Boolean {
        val originalLabels = original?.getLabels()?.toString()
        val postLabels = post?.getLabels()?.toString()
        return originalLabels == null || postLabels == null || originalLabels == postLabels
    }

    override fun onResume() {
        super.onResume()
        setOtherProcessStarted(false)
    }

    override fun onBackPressed() {
        if(toUpdatePost) {
            if (noChangesMade()) super.onBackPressed()
            else if(uploader.postingInProgress()) goToHomeScreen()
            else showConfirmClosePageDialog()
        } else super.onBackPressed()
    }

    fun getBinding(): ActivitySelectLabelBinding {
        return binding
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
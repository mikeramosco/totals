package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.core.view.iterator

class CreatePostPageListeners(private val activity: CreatePostPageActivity) {

    private val binding = activity.getBinding()

    private lateinit var scrollViewButtons: ButtonAnimationScrollView

    init {
        initButtonAnimationListeners()
        initButtonAnimationListenersInScrollView()
        initOnClickListeners()
        initHorizontalScrollViewListener()
    }

    fun getScrollViewAnimation(): ButtonAnimationScrollView {
        return scrollViewButtons
    }

    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.backButton)
        initButtonAnimationListener(binding.saveButton)
    }

    private fun initButtonAnimationListenersInScrollView() {
        scrollViewButtons = ButtonAnimationScrollView(
            binding.editActivityPageScroll, binding.editActivityPageScrollContents)
        scrollViewButtons.addButton(binding.addLabelButton)
    }

    private fun initOnClickListeners() {
        binding.backButton.setOnClickListener {
            activity.onBackPressed()
        }

        binding.saveButton.setOnClickListener {
            activity.postProgress()
        }

        binding.retakePhotoOption.setOnClickListener {
            activity.requestCameraPermission()
        }

        binding.clickableLayout.setOnClickListener {
            activity.openViewPhotoPage()
        }

        binding.addNoteButton.setOnClickListener {
            if(!activity.otherProcessStarted()) {
                activity.openNoteDialog(activity.getGoalProgress(),
                    createPostPageDialogs = activity.getDialogs())
                activity.getView().clearFocus()
            }
        }

        binding.addLinkButton.setOnClickListener {
            if(!activity.otherProcessStarted()) {
                activity.openAddLinkDialog(createPostPageDialogs = activity.getDialogs())
                activity.getView().clearFocus()
            }
        }

        binding.addACaptionLayout.setOnClickListener {
            activity.getDialogs().openAddCaptionDialog(binding.captionText.text.toString())
        }

        binding.addLabelButton.setOnClickListener {
            if(!activity.otherProcessStarted())
                activity.showManageLabelsDialog(activity.getGoalProgress(),
                    true, createPostPage = activity)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initHorizontalScrollViewListener() {
        binding.goalTagsScrollView.setOnTouchListener { _, motionEvent ->
            when(motionEvent.action) {
                MotionEvent.ACTION_MOVE -> {
                    releaseGoalTags()
                }
            }
            false
        }
    }

    private fun releaseGoalTags() {
        for(view in binding.labelsContainer.iterator()) {
            val labelButton = view.findViewById<LinearLayout>(R.id.labelButton)
            if(labelButton != null)
                scrollViewButtons.release(labelButton)
            else scrollViewButtons.release(view)
        }
    }

}
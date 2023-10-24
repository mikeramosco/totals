package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.core.view.iterator

class MePageListeners(private val activity: HomePageActivity,
                      private val fragment: MePageFragment) {

    private val binding = fragment.getFragmentBinding()

    private lateinit var scrollViewButtons: ButtonAnimationScrollView

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
        initButtonAnimationListenersInScrollView()
        initHorizontalScrollViewListener()
    }

    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.totalsButton)
        initButtonAnimationListener(binding.likeNotifsButton)
        initButtonAnimationListener(binding.addButton)
    }

    private fun initOnClickListeners() {
        binding.totalsButton.setOnClickListener {
            fragment.openProgressPage()
        }

        binding.likeNotifsButton.setOnClickListener {
            fragment.openLikeNotifsPage()
        }

        binding.addButton.setOnClickListener {
            fragment.openAddGoalPage()
        }

        binding.activitiesFilterLayout.setOnClickListener {
            fragment.openSelectLabelPage()
        }
    }

    private fun initButtonAnimationListenersInScrollView() {
        scrollViewButtons = ButtonAnimationScrollView(
            binding.goalPageScrollView, binding.goalPageContents)
    }

    fun getScrollViewButtonAnimation(): ButtonAnimationScrollView {
        return scrollViewButtons
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initHorizontalScrollViewListener() {
        binding.activeGoalsScrollView.setOnTouchListener { _, motionEvent ->
            when(motionEvent.action) {
                MotionEvent.ACTION_MOVE -> {
                    releaseActiveGoalButtons()
                }
            }
            false
        }
    }

    private fun releaseActiveGoalButtons() {
        for(view in binding.activeGoalsContainer.iterator()) {
            val activeGoalButton = view.findViewById<LinearLayout>(R.id.activeGoalButton)
            val moreButton = view.findViewById<LinearLayout>(R.id.moreButton)
            if(activeGoalButton != null && moreButton != null) {
                scrollViewButtons.release(activeGoalButton)
                scrollViewButtons.release(moreButton)
            }
            else scrollViewButtons.release(view)
        }
    }
}
package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class ButtonAnimationDialogHorizontalScrollView(
    private val scrollView: HorizontalScrollView,
    private val dialog: BottomSheetDialog) {

    private val dialogOptions = ArrayList<LinearLayout>()

    private val optionPressed = ArrayList<Boolean>()
    private val optionHighlighted = ArrayList<Boolean>()

    init {
        initDialogHorizontalScrollOptions()
    }

    fun addDialogOption(option: LinearLayout) {
        dialogOptions.add(option)
        optionPressed.add(false)
        optionHighlighted.add(false)
        initScrollViewOptionAnimationListener(option)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initScrollViewOptionAnimationListener(option: View) {
        val handler = Handler()
        var viewBounds = Rect()
        option.setOnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    setOptionPressed(option, isPressed = true)
                    viewBounds = Rect(v.left, v.top, v.right, v.bottom)
                    handler.postDelayed({
                        if(optionPressed(option)) {
                            setOptionPressed(option, isHighlighted = true)
                            animateButton(option, true)
                        }
                    }, TRANSITION_DELAY)
                }
                MotionEvent.ACTION_UP -> {
                    if(optionPressed(option) &&
                        viewBounds.contains(v.left + event.x.toInt(), v.top + event.y.toInt())) {
                        setOptionPressed(option, isPressed = false, isHighlighted = false)
                        animateButton(option, false)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if(optionPressed(option) &&
                        !viewBounds.contains(v.left + event.x.toInt(), v.top + event.y.toInt())) {
                        setOptionPressed(option, isPressed = false)

                        if(optionHighlighted(option)) {
                            setOptionPressed(option, isHighlighted = false)
                            animateButton(option, false)
                        }
                    }
                }
            }
            false
        }
    }

    private fun setOptionPressed(option: View, isPressed: Boolean? = null, isHighlighted: Boolean? = null) {
        for((i, view) in dialogOptions.withIndex()) {
            if(view == option) {
                if(isPressed != null) optionPressed[i] = isPressed
                if(isHighlighted != null) optionHighlighted[i] = isHighlighted
                break
            }
        }
    }

    private fun optionPressed(option: View): Boolean {
        for((i, view) in dialogOptions.iterator().withIndex())
            if(view == option) return optionPressed[i]
        return false
    }

    private fun optionHighlighted(option: View): Boolean {
        for((i, view) in dialogOptions.iterator().withIndex())
            if(view == option) return optionHighlighted[i]
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initDialogHorizontalScrollOptions() {

        fun releaseOptions() {
            for((i, view) in dialogOptions.withIndex()) {
                if(optionHighlighted[i]) {
                    animateButton(view, false)
                    optionHighlighted[i] = false
                }

                if(optionPressed[i])
                    optionPressed[i] = false
            }
        }

        dialog.behavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING)
                    releaseOptions()
                if(newState == BottomSheetBehavior.STATE_HIDDEN)
                    dialog.cancel()
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) { }
        })

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            releaseOptions()
        }

        scrollView.setOnTouchListener { _, motionEvent ->
            when(motionEvent.action) {
                MotionEvent.ACTION_MOVE -> {
                    releaseOptions()
                }
            }
            false
        }
    }
}
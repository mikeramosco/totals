package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

@SuppressLint("ClickableViewAccessibility")
class ButtonAnimationScrollView(private val scrollView: ScrollView,
                                private val scrollViewContents: LinearLayout) {

    private val buttons = ArrayList<View>()
    private val buttonIsPressedMap = HashMap<View, Boolean>()
    private val buttonIsHighlightedMap = HashMap<View, Boolean>()

    init {
        initScrollView()
    }

    fun release(view: View) {
        buttonIsPressedMap[view] = false
        val buttonIsHighlighted = buttonIsHighlightedMap[view]?: false
        if(buttonIsHighlighted) {
            animateButton(view, false)
            buttonIsHighlightedMap[view] = false
        }
    }

    private fun releaseAll() {
        fun releaseButton(view: View) {
            val viewIsHighlighted = buttonIsHighlightedMap[view]
            if(viewIsHighlighted != null && viewIsHighlighted) {
                buttonIsHighlightedMap[view] = false
                animateButton(view, false)
            }

            val viewIsPressed = buttonIsPressedMap[view]
            if(viewIsPressed != null && viewIsPressed)
                buttonIsPressedMap[view] = false
        }


        for(button in buttons) releaseButton(button)
    }

    private fun initScrollView() {
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            releaseAll()
        }

        scrollView.setOnTouchListener { _, motionEvent ->
            when(motionEvent.action) {
                MotionEvent.ACTION_MOVE -> calibrateScrollView()
                MotionEvent.ACTION_UP -> releaseAll()
            }
            false
        }
    }

    private fun calibrateScrollView() {
        val scrollViewHeight = scrollViewContents.height - scrollView.height
        if(scrollView.scrollY == 0) scrollView.scrollY = 1
        else if(scrollView.scrollY == scrollViewHeight)
            scrollView.scrollY = scrollViewHeight - 1
    }

    fun addButton(view: View) {
        buttons.add(view)
        buttonIsPressedMap[view] = false
        buttonIsHighlightedMap[view] = false
        initScrollViewButtonAnimationListener(view)
    }

    private fun initScrollViewButtonAnimationListener(button: View) {
        val handler = Handler()
        var viewBounds = Rect()
        button.setOnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    buttonIsPressedMap[button] = true
                    viewBounds = Rect(v.left, v.top, v.right, v.bottom)
                    handler.postDelayed({
                        val taskViewIsPressed = buttonIsPressedMap[button]
                        if(taskViewIsPressed != null && taskViewIsPressed) {
                            buttonIsHighlightedMap[button] = true
                            animateButton(button, true)
                        }
                    }, TRANSITION_DELAY)
                }
                MotionEvent.ACTION_UP -> {
                    val taskViewIsPressed = buttonIsPressedMap[button]
                    if(taskViewIsPressed != null && taskViewIsPressed &&
                        viewBounds.contains(v.left + event.x.toInt(), v.top + event.y.toInt())) {
                        buttonIsHighlightedMap[button] = false
                        buttonIsPressedMap[button] = false
                        animateButton(button, false)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val taskViewIsPressed = buttonIsPressedMap[button]
                    if(taskViewIsPressed != null && taskViewIsPressed &&
                        !viewBounds.contains(v.left + event.x.toInt(), v.top + event.y.toInt())) {
                        buttonIsPressedMap[button] = false

                        val taskViewIsHighlighted = buttonIsHighlightedMap[button]
                        if(taskViewIsHighlighted != null && taskViewIsHighlighted) {
                            buttonIsHighlightedMap[button] = false
                            animateButton(button, false)
                        }
                    }
                }
            }
            false
        }
    }
}
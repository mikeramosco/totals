package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.drawable.TransitionDrawable
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

@SuppressLint("ClickableViewAccessibility")
fun initButtonAnimationListener(button: View, isLongPressable: Boolean = false) {
    var viewBounds = Rect()
    var buttonPressed = false
    val handler = Handler()

    button.setOnTouchListener { v, event ->
        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                buttonPressed = true
                viewBounds = Rect(v.left, v.top, v.right, v.bottom)
                animateButton(button, true)
                if(isLongPressable) {
                    handler.postDelayed({
                        if(buttonPressed) {
                            animateButton(button, false)
                            buttonPressed = false
                        }
                    }, 500)
                }
            }
            MotionEvent.ACTION_UP -> {
                if(buttonPressed &&
                    viewBounds.contains(v.left + event.x.toInt(), v.top + event.y.toInt())) {
                    animateButton(button, false)
                    buttonPressed = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if(buttonPressed &&
                    !viewBounds.contains(v.left + event.x.toInt(), v.top + event.y.toInt())) {
                    animateButton(button, false)
                    buttonPressed = false
                }
            }
        }
        false
    }
}

fun animateButton(button: View, isPressed: Boolean, showFadeOut: Boolean = true) {

    val transition = button.background as TransitionDrawable

    if(isPressed)
        transition.startTransition(FADE_IN_DURATION)
    else {
        if(showFadeOut) transition.startTransition(0)
        transition.reverseTransition(FADE_OUT_DURATION)
    }
}

@SuppressLint("ClickableViewAccessibility")
fun initDialogOptions(dialog: BottomSheetDialog? = null,
                      dialogOptions: ArrayList<LinearLayout>) {
    val dialogOptionPressed = ArrayList<Boolean>()
    for(i in 0 until dialogOptions.size)
        dialogOptionPressed.add(false)

    fun setOptionPressed(option: LinearLayout, isPressed: Boolean) {
        dialogOptionPressed[dialogOptions.indexOf(option)] = isPressed
    }

    fun optionIsPressed(option: LinearLayout): Boolean {
        return dialogOptionPressed[dialogOptions.indexOf(option)]
    }

    fun releaseOption(option: LinearLayout, showFadeOut: Boolean = true) {
        animateButton(option, false, showFadeOut)
        setOptionPressed(option, false)
    }

    dialog?.behavior?.setBottomSheetCallback(object :
        BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                for(option in dialogOptions)
                    if(optionIsPressed(option))
                        releaseOption(option, false)
            }
            if(newState == BottomSheetBehavior.STATE_HIDDEN) {
                dialog.cancel()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) { }
    })

    fun initAnimatedOption(option: LinearLayout) {
        var viewBounds = Rect()

        fun touchIsInViewBounds(v: View, event: MotionEvent): Boolean {
            return viewBounds.contains(v.left + event.x.toInt(), v.top + event.y.toInt())
        }

        option.setOnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    setOptionPressed(option, true)
                    viewBounds = Rect(v.left, v.top, v.right, v.bottom)
                    animateButton(option, true)
                }
                MotionEvent.ACTION_UP -> {
                    if(optionIsPressed(option) && touchIsInViewBounds(v, event))
                        releaseOption(option)
                }
                MotionEvent.ACTION_MOVE -> {
                    if(optionIsPressed(option) && !touchIsInViewBounds(v, event))
                        releaseOption(option)
                }
            }
            false
        }
    }

    for(option in dialogOptions)
        initAnimatedOption(option)
}
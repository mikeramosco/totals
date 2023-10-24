package com.justanotherdeveloper.totalslite

import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View

class SendProfileListeners(private val activity: SendProfileActivity) {

    private val binding = activity.getBinding()

    private lateinit var scrollViewButtons: ButtonAnimationScrollView

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
        initButtonAnimationListenersInScrollView()
        initSearchFieldListener()
    }

    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.backButton)
        initButtonAnimationListener(binding.sendButton)
    }

    private fun initOnClickListeners() {
        binding.backButton.setOnClickListener {
            activity.onBackPressed()
        }

        binding.sendButton.setOnClickListener {
            activity.sendButtonClicked()
        }
    }

    private fun initButtonAnimationListenersInScrollView() {
        scrollViewButtons = ButtonAnimationScrollView(
            binding.usersScrollView, binding.usersContainer)
    }

    fun getScrollViewButtonAnimation(): ButtonAnimationScrollView {
        return scrollViewButtons
    }

    private fun initSearchFieldListener() {
        val handler = Handler()
        binding.searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                val searchedText = binding.searchField.text.toString().lowercase()
                if(searchedText.length <= 1) {
                    activity.getView().applySearch("")
                    binding.searchProgressCircle.visibility = View.GONE
                    return
                }
                binding.searchProgressCircle.visibility = View.VISIBLE
                handler.postDelayed({
                    val updatedSearchedText = binding.searchField.text.toString().lowercase()
                    if(searchedText == updatedSearchedText) {
                        activity.getView().applySearch(searchedText)
                        binding.searchProgressCircle.visibility = View.GONE
                    }
                }, SEARCH_DELAY)
            }
        })
    }
}
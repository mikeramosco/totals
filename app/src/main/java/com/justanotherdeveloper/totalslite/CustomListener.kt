package com.justanotherdeveloper.totalslite

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

open class CustomListener(context: Context) {

    private var counter = 0
    private val editText = EditText(context)

    init {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                onListenerTriggered()
            }
        })
    }

    fun triggerListener() {
        editText.setText((counter++).toString())
        if(counter == 100) counter = 0
    }

    open fun onListenerTriggered() {}
}
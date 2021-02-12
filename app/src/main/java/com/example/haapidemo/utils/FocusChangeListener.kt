package com.example.haapidemo.utils

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

class FocusChangeListener: View.OnFocusChangeListener {

    private fun hideKeyboard(view: View) {
        val inputMethodManager: InputMethodManager = ContextCompat.getSystemService(
            view.context,
            InputMethodManager::class.java
        )!!
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) {
            hideKeyboard(v)
        }
    }
}
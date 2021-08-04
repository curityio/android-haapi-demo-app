/*
 *  Copyright (C) 2021 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.haapidemo.uicomponents

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.Constant
import io.curity.haapidemo.R
import java.lang.ref.WeakReference

open class LinearVerticalLayoutViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    val linearLayout: LinearLayout = itemView.findViewById(R.id.linear_layout)

    companion object {
        fun inflatedView(layoutInflater: LayoutInflater, parentView: ViewGroup): View {
            return layoutInflater.inflate(R.layout.linear_layout_item, parentView, false)
        }
    }
}

class SelectorViewHolder(itemView: View): LinearVerticalLayoutViewHolder(itemView), ViewStopLoadable {

    private val progressButton: ProgressButton by lazy { ProgressButton(itemView.context, null, R.style.SecondaryProgressButton) }

    init {
        super.linearLayout.addView(progressButton)
    }

    fun bind(label: String, imageResourceId: Int = 0, clickHandler: (WeakReference<ViewStopLoadable>) -> Unit) {
        progressButton.setText(label)
        if (imageResourceId != 0) {
            progressButton.setImage(imageResourceId)
        }
        progressButton.setOnClickListener {
            Log.i(Constant.TAG, "Progress Button clicked")
            progressButton.setLoading(true)
            progressButton.isClickable = false
            it.isClickable = false
            it.isEnabled = false
            clickHandler(WeakReference(this))
        }
    }

    override fun stopLoading() {
        Log.i(Constant.TAG, "Stop loading was called")
        progressButton.setLoading(false)
        progressButton.isEnabled = true
    }

    companion object {
        fun from(parentView: ViewGroup): SelectorViewHolder {
            val layoutInflater = LayoutInflater.from(parentView.context)
            val view = inflatedView(layoutInflater, parentView)
            return SelectorViewHolder(view)
        }
    }
}

interface ViewStopLoadable {
    fun stopLoading()
}
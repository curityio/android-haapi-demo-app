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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.R

class LoadingActionViewHolder private constructor(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    private val textView: TextView = itemView.findViewById(R.id.text_view)

    fun bind(text: CharSequence) {
        textView.text = text
        setLoading(false)
    }

    fun setLoading(isLoading: Boolean) {
        textView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        progressBar.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
    }

    companion object {
        fun from(parentView: ViewGroup): LoadingActionViewHolder {
            val layoutInflater = LayoutInflater.from(parentView.context)
            val view = layoutInflater.inflate(R.layout.loading_action_view_item, parentView, false)

            return LoadingActionViewHolder(view)
        }
    }
}
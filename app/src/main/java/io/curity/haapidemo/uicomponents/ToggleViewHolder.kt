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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import io.curity.haapidemo.R

class ToggleViewHolder private constructor(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val switchView: SwitchMaterial = itemView.findViewById(R.id.toggle)

    fun bind(
        label: CharSequence,
        isToggled: Boolean,
        didToggle: () -> Unit)
    {
        switchView.text = label
        switchView.isChecked = isToggled

        switchView.setOnCheckedChangeListener { _ , _ ->
            didToggle()
        }
    }

    companion object {
        fun from(parentView: ViewGroup): ToggleViewHolder {
            val layoutInflater = LayoutInflater.from(parentView.context)
            val view = layoutInflater.inflate(R.layout.toggle_view_item, parentView, false)

            return ToggleViewHolder(view)
        }
    }

}
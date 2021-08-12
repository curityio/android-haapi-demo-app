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

package io.curity.haapidemo.ui.settings.profile

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.curity.haapidemo.R
import io.curity.haapidemo.uicomponents.CheckboxViewHolder

class ScopesAdapter(private val checkHandler: (Int) -> Unit): ListAdapter<ProfileItem.Checkbox, CheckboxViewHolder>(
    CONFIG_COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckboxViewHolder {
        return CheckboxViewHolder.from(parent, leftMargin = parent.context.resources.getDimension(R.dimen.spacing).toInt())
    }

    override fun onBindViewHolder(holder: CheckboxViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(text = item.text, isChecked = item.isChecked, isClickable = true) {
            checkHandler(position)
        }
    }

    companion object {
        private val CONFIG_COMPARATOR = object: DiffUtil.ItemCallback<ProfileItem.Checkbox>() {
            override fun areItemsTheSame(oldItem: ProfileItem.Checkbox, newItem: ProfileItem.Checkbox): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ProfileItem.Checkbox, newItem: ProfileItem.Checkbox): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}
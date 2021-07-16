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
package io.curity.haapidemo.ui.settings

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.curity.haapidemo.flow.HaapiFlowConfiguration

class ProfilesAdapter(private val clickHandler: (HaapiFlowConfiguration, Int) -> Unit): ListAdapter<HaapiFlowConfiguration, ProfileViewHolder>(CONFIG_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        return ProfileViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val configItem = getItem(position)
        holder.bind(configItem)
        holder.itemView.setOnClickListener { clickHandler(configItem, position) }
    }

    companion object {
        private val CONFIG_COMPARATOR = object: DiffUtil.ItemCallback<HaapiFlowConfiguration>() {
            override fun areItemsTheSame(oldItem: HaapiFlowConfiguration, newItem: HaapiFlowConfiguration): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: HaapiFlowConfiguration, newItem: HaapiFlowConfiguration): Boolean {
                return oldItem.name == newItem.name
            }
        }
    }
}
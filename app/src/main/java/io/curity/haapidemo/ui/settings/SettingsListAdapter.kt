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
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.Configuration
import io.curity.haapidemo.ui.settings.profile.ProfileViewHolder
import io.curity.haapidemo.uicomponents.SettingsSectionViewHolder

class SettingsListAdapter(private val clickHandler: (Configuration) -> Unit): ListAdapter<SettingsItem, RecyclerView.ViewHolder>(CONFIG_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            SettingsType.Header.ordinal -> SettingsSectionViewHolder.from(parent)
            SettingsType.Configuration.ordinal -> ProfileViewHolder.from(parent)

            else -> throw ClassCastException("No class for viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SettingsSectionViewHolder -> {
                val headerItem = getItem(position) as SettingsItem.Header
                holder.bind(headerItem.title)
            }
            is ProfileViewHolder -> {
                val profileItem = getItem(position) as SettingsItem.Configuration
                holder.bind(profileItem.configuration)
                holder.itemView.setOnClickListener {
                    clickHandler(profileItem.configuration)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is SettingsItem.Header -> SettingsType.Header.ordinal
            is SettingsItem.Configuration -> SettingsType.Configuration.ordinal
        }
    }

    companion object {
        private val CONFIG_COMPARATOR = object: DiffUtil.ItemCallback<SettingsItem>() {
            override fun areItemsTheSame(oldItem: SettingsItem, newItem: SettingsItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: SettingsItem, newItem: SettingsItem): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }

    private enum class SettingsType {
        Header,
        Configuration
    }
}

sealed class SettingsItem {
    abstract val id: Long

    data class Header(val title: String): SettingsItem() {
        override val id: Long = title.hashCode().toLong()
    }

    data class Configuration(val configuration: io.curity.haapidemo.Configuration): SettingsItem() {
        override val id: Long = configuration.hashCode().toLong()
    }
}
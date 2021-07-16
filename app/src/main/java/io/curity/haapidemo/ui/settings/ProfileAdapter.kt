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
import io.curity.haapidemo.uicomponents.SectionViewHolder
import io.curity.haapidemo.uicomponents.TextViewHolder
import io.curity.haapidemo.uicomponents.ToggleViewHolder

class ProfileAdapter(
    private val clickHandler: (ProfileItem.Content, Int) -> Unit,
    private val toggleHandler: (Int) -> Unit
): ListAdapter<ProfileItem, RecyclerView.ViewHolder>(CONFIG_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            ItemType.Header.ordinal -> SectionViewHolder.from(parent)
            ItemType.Content.ordinal -> TextViewHolder.from(parent)
            ItemType.Toggle.ordinal -> ToggleViewHolder.from(parent)
            else -> throw ClassCastException("No class for viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SectionViewHolder -> {
                val headerItem = getItem(position) as ProfileItem.Header
                holder.bind(headerItem.title)
            }
            is TextViewHolder -> {
                val contentItem = getItem(position) as ProfileItem.Content
                holder.bind(
                    header = contentItem.header,
                    text = contentItem.text
                )
                holder.itemView.setOnClickListener { clickHandler(contentItem, position) }
            }
            is ToggleViewHolder -> {
                val toggleItem = getItem(position) as ProfileItem.Toggle
                holder.bind(toggleItem.label, toggleItem.isToggled) {
                    toggleHandler(position)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is ProfileItem.Header -> ItemType.Header.ordinal
            is ProfileItem.Content -> ItemType.Content.ordinal
            is ProfileItem.Toggle -> ItemType.Toggle.ordinal
        }
    }

    companion object {
        private val CONFIG_COMPARATOR = object: DiffUtil.ItemCallback<ProfileItem>() {
            override fun areItemsTheSame(oldItem: ProfileItem, newItem: ProfileItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ProfileItem, newItem: ProfileItem): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }

    private enum class ItemType {
        Header,
        Content,
        Toggle
    }

}

sealed class ProfileItem {

    abstract val id: Long

    data class Header(val title: String): ProfileItem() {
        override val id: Long = title.hashCode().toLong()
    }

    data class Content(val header: String, val text: String): ProfileItem() {
        override val id: Long = header.hashCode().toLong()
    }

    data class Toggle(val label: String, val isToggled: Boolean): ProfileItem() {
        override val id: Long = label.hashCode().toLong()
    }

}
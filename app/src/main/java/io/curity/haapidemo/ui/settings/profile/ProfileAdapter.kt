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
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.uicomponents.*

class ProfileAdapter(
    private val clickHandler: (ProfileItem, Int) -> Unit,
    private val toggleHandler: (Int) -> Unit
): ListAdapter<ProfileItem, RecyclerView.ViewHolder>(CONFIG_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            ItemType.Header.ordinal -> SectionViewHolder.from(parent)
            ItemType.Content.ordinal -> TextViewHolder.from(parent)
            ItemType.Toggle.ordinal -> ToggleViewHolder.from(parent)
            ItemType.LoadingAction.ordinal -> LoadingActionViewHolder.from(parent)
            ItemType.Checkbox.ordinal -> CheckboxViewHolder.from(parent)
            ItemType.Recycler.ordinal -> RecyclerViewHolder.from(parent)
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
            is LoadingActionViewHolder -> {
                val loadingAction = getItem(position) as ProfileItem.LoadingAction
                holder.bind(loadingAction.text)
                holder.itemView.setOnClickListener {
                    clickHandler(loadingAction, position)
                    holder.setLoading(true)
                }
            }
            is CheckboxViewHolder -> {
                val item = getItem(position) as ProfileItem.Checkbox
                holder.bind(
                    text = item.text,
                    isChecked = item.isChecked,
                    didToggle =  { toggleHandler(position) }
                )
            }
            is RecyclerViewHolder -> {
                val item = getItem(position) as ProfileItem.Recycler
                holder.bind(item.adapter)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is ProfileItem.Header -> ItemType.Header.ordinal
            is ProfileItem.Content -> ItemType.Content.ordinal
            is ProfileItem.Toggle -> ItemType.Toggle.ordinal
            is ProfileItem.LoadingAction -> ItemType.LoadingAction.ordinal
            is ProfileItem.Checkbox -> ItemType.Checkbox.ordinal
            is ProfileItem.Recycler -> ItemType.Recycler.ordinal
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
        Toggle,
        LoadingAction,
        Checkbox,
        Recycler
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

    data class LoadingAction(val text: String, val dateLong: Long): ProfileItem() {
        override val id: Long = text.hashCode().toLong() + dateLong
    }

    data class Checkbox(val text: String, var isChecked: Boolean): ProfileItem() {
        override val id: Long = text.hashCode().toLong() + isChecked.hashCode().toLong()
    }

    data class Recycler(val adapter: ListAdapter<Checkbox, CheckboxViewHolder>): ProfileItem() {
        override val id: Long = adapter.hashCode().toLong()
    }
}
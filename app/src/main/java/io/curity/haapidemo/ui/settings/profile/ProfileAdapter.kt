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
import io.curity.haapidemo.R
import io.curity.haapidemo.uicomponents.*

class ProfileAdapter(
    private val clickHandler: (ProfileItem, Int) -> Unit,
    private val toggleHandler: (Int) -> Unit
): ListAdapter<ProfileItem, RecyclerView.ViewHolder>(CONFIG_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            ProfileItem.Type.Header.ordinal -> SettingsSectionViewHolder.from(parent)
            ProfileItem.Type.Content.ordinal -> TextViewHolder.from(parent)
            ProfileItem.Type.Toggle.ordinal -> ToggleViewHolder.from(parent)
            ProfileItem.Type.LoadingAction.ordinal -> LoadingActionViewHolder.from(parent)
            ProfileItem.Type.Checkbox.ordinal -> CheckboxViewHolder.from(parent, leftMargin = parent.context.resources.getDimension(R.dimen.padding).toInt())
            ProfileItem.Type.Recycler.ordinal -> RecyclerViewHolder.from(parent)
            else -> throw ClassCastException("No class for viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SettingsSectionViewHolder -> {
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
                    isClickable = true,
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
        return getItem(position).type.ordinal
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
}

sealed class ProfileItem {

    enum class Type {
        Header,
        Content,
        Toggle,
        LoadingAction,
        Checkbox,
        Recycler
    }
    abstract val id: Long
    abstract val type: Type

    data class Header(val title: String): ProfileItem() {
        override val id: Long = title.hashCode().toLong()
        override val type: Type = Type.Header
    }

    data class Content(val header: String, val text: String): ProfileItem() {
        override val id: Long = header.hashCode().toLong()
        override val type: Type = Type.Content
    }

    data class Toggle(val label: String, val isToggled: Boolean): ProfileItem() {
        override val id: Long = label.hashCode().toLong()
        override val type: Type = Type.Toggle
    }

    data class LoadingAction(val text: String, val dateLong: Long): ProfileItem() {
        override val id: Long = text.hashCode().toLong() + dateLong
        override val type: Type = Type.LoadingAction
    }

    data class Checkbox(val text: String, var isChecked: Boolean): ProfileItem() {
        override val id: Long = text.hashCode().toLong() + isChecked.hashCode().toLong()
        override val type: Type = Type.Checkbox
    }

    data class Recycler(val adapter: ListAdapter<Checkbox, CheckboxViewHolder>): ProfileItem() {
        override val id: Long = adapter.hashCode().toLong()
        override val type: Type = Type.Recycler
    }
}
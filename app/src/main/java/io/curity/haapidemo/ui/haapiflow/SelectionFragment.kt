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

package io.curity.haapidemo.ui.haapiflow

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.curity.haapidemo.R
import io.curity.haapidemo.uicomponents.ProgressButton
import io.curity.haapidemo.uicomponents.SelectorViewHolder
import io.curity.haapidemo.uicomponents.ViewStopLoadable
import kotlinx.android.parcel.Parcelize
import java.lang.ref.WeakReference

class SelectionFragment: Fragment() {
    private lateinit var selectionRecyclerView: RecyclerView
    private lateinit var linksLayout: LinearLayout
    private val adapter = SelectionAdapter(clickHandler = { index, weakView -> select(index, weakView) })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_selection, container, false)

        selectionRecyclerView = root.findViewById(R.id.selection_recycler)
        selectionRecyclerView.adapter = adapter

        linksLayout = root.findViewById(R.id.linear_layout_links)

        val model = requireArguments().getParcelable<SelectionFragmentModel>(EXTRA_MODEL) ?: throw IllegalStateException("Expecting a SelectionFragmentModel")

        adapter.submitList(
            model.titles
        )

        linksLayout.removeAllViews()
        if (model.links.isEmpty()) {
            linksLayout.visibility = View.GONE
        } else {
            linksLayout.visibility = View.VISIBLE
        }
        model.links.forEach { value ->
            val button = ProgressButton(requireContext(), null, R.style.LinkProgressButton).apply {
                this.setText(value)
                this.setOnClickListener {
                    requireActivity().supportFragmentManager.setFragmentResult(
                        GenericHaapiFragment.REQUEST_KEY_LINK,
                        bundleOf(GenericHaapiFragment.RESULT_INDEX to value)
                    )
                }
            }
            linksLayout.addView(button)
        }

        return root
    }

    private fun select(index: Int, weakView: WeakReference<ViewStopLoadable>) {
        weakView.get()?.stopLoading()

        requireActivity().supportFragmentManager.setFragmentResult(
            GenericHaapiFragment.REQUEST_KEY_SELECT,
            bundleOf(GenericHaapiFragment.RESULT_INDEX to index)
        )
    }

    private class SelectionAdapter(
        private val clickHandler: (Int, WeakReference<ViewStopLoadable>) -> Unit
    ): ListAdapter<String, SelectorViewHolder>(CONFIG_COMPARATOR) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectorViewHolder {
            return SelectorViewHolder.from(parent)
        }

        override fun onBindViewHolder(holder: SelectorViewHolder, position: Int) {
            val item = getItem(position)
            holder.bind(item) {
                clickHandler(position, it)
            }
        }

        companion object {
            private val CONFIG_COMPARATOR = object: DiffUtil.ItemCallback<String>() {
                override fun areItemsTheSame(
                    oldItem: String,
                    newItem: String
                ): Boolean {
                    return oldItem === newItem
                }

                override fun areContentsTheSame(
                    oldItem: String,
                    newItem: String
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }

    @Parcelize
    data class SelectionFragmentModel(
        val titles: List<String>,
        val links: List<String>
    ): Parcelable

    companion object {
        private const val EXTRA_MODEL = "io.curity.haapidemo.selectionFragment.extra_model"

        fun newInstance(selectionFragmentModel: SelectionFragmentModel): SelectionFragment {
            val fragment = SelectionFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_MODEL, selectionFragmentModel)
            }
            return fragment
        }
    }
}
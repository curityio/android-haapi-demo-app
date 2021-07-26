/*
 *  Copyright 2021 Curity AB
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.curity.haapidemo.ProfileActivity
import io.curity.haapidemo.R
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsListFragment : Fragment() {

    private lateinit var settingsListViewModel: SettingsListViewModel
    private val adapter by lazy { SettingsListAdapter(clickHandler = { config -> select(config)})}

    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_list, container, false)

        recyclerView = root.findViewById(R.id.recyclerView)
        addButton = root.findViewById(R.id.floatingActionButton)

        settingsListViewModel = ViewModelProvider(requireActivity()).get(SettingsListViewModel::class.java)

        val helper = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                settingsListViewModel.removeConfigurationAt(viewHolder.adapterPosition)
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return if (settingsListViewModel.settingsItemCanBeSwippedAt(viewHolder.adapterPosition)) {
                    super.getSwipeDirs(recyclerView, viewHolder)
                } else {
                    0
                }
            }
        }
        val onTouchItemListener = ItemTouchHelper(helper)
        onTouchItemListener.attachToRecyclerView(recyclerView)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter

        settingsListViewModel.models.observe(viewLifecycleOwner) { newList ->
            lifecycleScope.launch (Dispatchers.Main) {
                adapter.submitList(newList)
            }
        }

        addButton.setOnClickListener {
            lifecycleScope.launch {
                val newConfiguration = settingsListViewModel.addNewConfiguration()
                lifecycleScope.launch(Dispatchers.Main) {
                    select(newConfiguration)
                }
            }
        }
    }

    private fun select(config: HaapiFlowConfiguration) {
        val newIntent = ProfileActivity.newIntent(
            context = requireContext(),
            haapiConfiguration = config,
            index = settingsListViewModel.indexOfConfiguration(config),
            isActiveConfiguration = settingsListViewModel.isActiveConfiguration(config)
        )
        startActivity(newIntent)
    }
}
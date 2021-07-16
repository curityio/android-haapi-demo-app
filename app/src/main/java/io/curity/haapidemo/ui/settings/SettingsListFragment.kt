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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.curity.haapidemo.ProfileActivity
import io.curity.haapidemo.R
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsListFragment : Fragment() {

    private lateinit var settingsListViewModel: SettingsListViewModel
    private val adapter by lazy { ProfilesAdapter(clickHandler = { config, index -> select(config, index)})}

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

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter

        settingsListViewModel.configurations.observe(viewLifecycleOwner) {
            lifecycleScope.launch(Dispatchers.Main) {
                adapter.submitList(it)
            }
        }

        addButton.setOnClickListener {
            lifecycleScope.launch {
                settingsListViewModel.addNewConfiguration()
            }
        }
    }

    private fun select(config: HaapiFlowConfiguration, atIndex: Int) {
        val newIntent = ProfileActivity.newIntent(requireContext(), config, atIndex)
        startActivity(newIntent)
    }
}
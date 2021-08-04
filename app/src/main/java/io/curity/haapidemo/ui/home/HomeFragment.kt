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
package io.curity.haapidemo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.curity.haapidemo.FlowActivity
import io.curity.haapidemo.R
import io.curity.haapidemo.ShowcaseActivity
import io.curity.haapidemo.flow.HaapiFlowConfiguration
import io.curity.haapidemo.uicomponents.ProgressButton

class HomeFragment : Fragment() {

    private lateinit var activeHaapiConfigViewModel: ActiveHaapiConfigViewModel
    private var haapiFlowConfiguration: HaapiFlowConfiguration? = null

    private lateinit var button: ProgressButton
    private lateinit var imageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        button = root.findViewById(R.id.button)
        imageView = root.findViewById(R.id.image_view)
        activeHaapiConfigViewModel = ViewModelProvider(requireActivity()).get(ActiveHaapiConfigViewModel::class.java)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activeHaapiConfigViewModel.haapiFlowConfiguration.observe(viewLifecycleOwner) { config ->
            haapiFlowConfiguration = config
        }

        button.setOnClickListener {
            val config = haapiFlowConfiguration
            if (config != null) {
                val intent = FlowActivity.newIntent(requireContext(), config)
                startActivity(intent)
            }
        }

        imageView.setOnLongClickListener {
            startActivity(ShowcaseActivity.newIntent(requireContext()))
            true
        }
    }
}
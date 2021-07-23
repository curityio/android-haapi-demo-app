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

package io.curity.haapidemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.appbar.CollapsingToolbarLayout
import androidx.appcompat.app.AppCompatActivity
import io.curity.haapidemo.uicomponents.DisclosureContent
import io.curity.haapidemo.uicomponents.DisclosureView

class ShowcaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_showcase)
        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = title

        val disclosureView: DisclosureView = findViewById(R.id.disclosureView)
        disclosureView.let {
            it.setContentText("Hello World")

            val contents: MutableList<DisclosureContent> = mutableListOf()
            contents.add(DisclosureContent("A label 1", "A description 1"))
            contents.add(DisclosureContent("A label 2", "A description 2"))

            it.setDisclosureContents(contents)
        }

    }

    companion object {

        fun newIntent(context: Context): Intent {
            return Intent(context, ShowcaseActivity::class.java)
        }
    }
}
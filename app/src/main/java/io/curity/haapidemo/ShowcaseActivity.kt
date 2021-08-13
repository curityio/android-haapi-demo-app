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
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import androidx.appcompat.app.AppCompatActivity
import io.curity.haapidemo.uicomponents.*

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

        val progressButton: ProgressButton = findViewById(R.id.progress_button)
        progressButton.setOnClickListener {
            demoLoadProgressButton(it as ProgressButton)
        }

        val secondaryProgressButton: ProgressButton = findViewById(R.id.progress_button_2)
        secondaryProgressButton.setOnClickListener {
            demoLoadProgressButton(it as ProgressButton)
        }

        val linearLayout: LinearLayout = findViewById(R.id.linear_layout)
        val problemView = ProblemView(this)
        problemView.setTitle("Test")
        val problemBundles: MutableList<ProblemView.ProblemBundle> = mutableListOf()
        problemBundles.add(
            ProblemView.ProblemBundle(
                text = "Error",
                messageStyle = MessageStyle.Info()
            )
        )

        problemView.setProblemBundles(problemBundles)
        linearLayout.addView(problemView)
    }

    private fun demoLoadProgressButton(button: ProgressButton) {
        button.setLoading(true)
        Handler(Looper.getMainLooper()).postDelayed({
            button.setLoading(false)
        }, 3000)
    }

    companion object {

        fun newIntent(context: Context): Intent {
            return Intent(context, ShowcaseActivity::class.java)
        }
    }
}
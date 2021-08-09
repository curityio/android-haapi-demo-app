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
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import io.curity.haapidemo.ui.home.ActiveHaapiConfigViewModel
import io.curity.haapidemo.ui.home.ActiveHaapiConfigViewModelFactory
import io.curity.haapidemo.ui.settings.HaapiFlowConfigurationRepository
import io.curity.haapidemo.ui.settings.SettingsListViewModel
import io.curity.haapidemo.ui.settings.SettingsListViewModelFactory
import java.security.MessageDigest

val Context.configurationDataStore by preferencesDataStore("io.curity.haapidemo.datastore.configuration")

class MainActivity : AppCompatActivity() {

    private fun logAppInfo()  {
        // You can use this to get the signature that should be registered at the Curity Identity Server
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val packageInfo =
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            packageInfo.signingInfo.apkContentsSigners.map { SHA256(it.toByteArray()) }.toString()
        } else {
            "Your Android is below API 28. Please check the README.md"
        }
        Log.d("PackageName", packageName)
        Log.d("AppInfo", "APK signatures $signatures")
    }

    private fun SHA256(bytes: ByteArray): String =
        Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes), Base64.DEFAULT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logAppInfo()

        // Register ViewModel
        ViewModelProvider(this,
            SettingsListViewModelFactory(HaapiFlowConfigurationRepository(dataStore = configurationDataStore))
        ).get(SettingsListViewModel::class.java)

        ViewModelProvider(this,
            ActiveHaapiConfigViewModelFactory(HaapiFlowConfigurationRepository(dataStore = configurationDataStore))
        ).get(ActiveHaapiConfigViewModel::class.java)

        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)
    }
}

class Constant {
    companion object {
        const val TAG = "DEBUG_HAAPI"
        const val TAG_FRAGMENT_LIFECYCLE = "DEBUG_HAAPI_FRAGMENT"
    }
}
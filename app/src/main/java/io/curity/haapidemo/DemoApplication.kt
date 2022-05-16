package io.curity.haapidemo

import android.app.Application
import io.curity.haapidemo.utils.HaapiAccessorRepository

class DemoApplication : Application() {
    val haapiAccessorRepository = HaapiAccessorRepository()
}
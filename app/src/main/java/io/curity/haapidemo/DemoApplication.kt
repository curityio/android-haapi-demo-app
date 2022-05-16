package io.curity.haapidemo

import android.app.Application
import io.curity.haapidemo.utils.HaapiAccessorRepository
import se.curity.identityserver.haapi.android.sdk.HaapiAccessor

class DemoApplication : Application() {
    private val haapiAccessorRepository = HaapiAccessorRepository()

    suspend fun loadAccessor(configuration: Configuration, force: Boolean = false): HaapiAccessor {
        return haapiAccessorRepository.load(configuration, applicationContext, force)
    }

    fun closeAccessor() {
        return haapiAccessorRepository.close()
    }
}
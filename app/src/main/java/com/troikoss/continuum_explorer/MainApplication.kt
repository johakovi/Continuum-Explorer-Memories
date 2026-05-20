package com.troikoss.continuum_explorer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.troikoss.continuum_explorer.managers.ShizukuManager
import com.troikoss.continuum_explorer.providers.UniversalFileFetcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        ShizukuManager.init(packageName)

        // Start binding the Shizuku service as early as possible
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch {
            if (ShizukuManager.isAvailable() && ShizukuManager.hasPermission()) {
                ShizukuManager.getService()
            }
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(UniversalFileFetcher.Factory()) }
        .respectCacheHeaders(enable = false)
        .build()
}

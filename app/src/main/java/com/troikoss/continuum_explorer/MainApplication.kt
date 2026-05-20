package com.troikoss.continuum_explorer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.troikoss.continuum_explorer.managers.ShizukuManager
import com.troikoss.continuum_explorer.providers.UniversalFileFetcher

class MainApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        ShizukuManager.init(packageName)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(UniversalFileFetcher.Factory()) }
        .respectCacheHeaders(enable = false)
        .build()
}

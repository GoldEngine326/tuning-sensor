package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class TuningApp : Application(), ImageLoaderFactory {
  override fun newImageLoader(): ImageLoader {
    return ImageLoader.Builder(this)
      .components {
        add(com.example.util.DataUriFetcher.Factory())
      }
      .memoryCache {
        MemoryCache.Builder(this)
          .maxSizePercent(0.25) // 25% of app memory for fast caching
          .strongReferencesEnabled(true)
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve("image_cache"))
          .maxSizeBytes(100L * 1024 * 1024) // 100 MB disk cache
          .build()
      }
      .respectCacheHeaders(false)
      .crossfade(true)
      .build()
  }
}

package com.koi.thepiece.core.image

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.io.File

object AppImageLoader {
    fun build(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient {
                OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250MB
                    .build()
            }
            // If your server cache headers aren’t consistent, this helps keep disk cache effective:
            .respectCacheHeaders(false)
            .build()
    }
}

package com.koi.thepiece.core.image

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Provides a centralized Coil ImageLoader configuration for the application.
 *
 * This object configures:
 * - Network behavior via OkHttp
 * - In-memory image caching
 * - Disk-based image caching
 *
 * The goal is to optimize performance, reduce repeated network calls,
 * and ensure smooth image rendering in the Catalogue and Card Detail views.
 */
object AppImageLoader {

    /**
     * Builds and returns a configured ImageLoader instance.
     *
     * @param context Application context used for cache initialization.
     * @return Configured ImageLoader instance.
     */
    fun build(context: Context): ImageLoader {
        return ImageLoader.Builder(context)

            /**
             * Configures the OkHttp client used by Coil for network image requests.
             *
             * retryOnConnectionFailure(true) allows automatic retry in case of
             * transient network issues.
             */
            .okHttpClient {
                OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .build()
            }

            /**
             * Configures in-memory image caching.
             *
             * maxSizePercent(0.25) allocates up to 25% of available app memory
             * for caching images to improve scroll performance and reduce reloads.
             */
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }

            /**
             * Configures disk-based caching for downloaded images.
             *
             * Images are stored under the app's cache directory in "image_cache".
             * Maximum disk usage is capped at 250MB.
             */
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250MB
                    .build()
            }

            /**
             * Disables strict respect for server cache headers.
             *
             * This is useful when backend cache headers are inconsistent,
             * ensuring disk caching remains effective for frequently accessed images.
             */
            .respectCacheHeaders(false)

            .build()
    }
}
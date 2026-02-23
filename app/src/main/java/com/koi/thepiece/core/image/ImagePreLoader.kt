package com.koi.thepiece.core.image

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest

/**
 * Utility object responsible for preloading card images.
 *
 * This improves perceived performance in the Catalogue by
 * loading images into memory/disk cache before they are displayed.
 * Preloading reduces visible loading delays during fast scrolling.
 */
object ImagePreloader {

    /**
     * Preloads a list of image URLs into Coil's cache.
     *
     * @param context Application context used to access the ImageLoader.
     * @param urls List of image URLs to preload.
     * @param limit Maximum number of images to preload in one call (default = 30).
     *
     * Implementation details:
     * - Filters out blank URLs.
     * - Limits the number of requests to avoid excessive memory/network usage.
     * - Enqueues asynchronous image requests without rendering them to UI.
     */
    fun preload(context: Context, urls: List<String>, limit: Int = 30) {

        urls.asSequence()
            .filter { it.isNotBlank() }     // Ignore empty or invalid URLs
            .take(limit)                   // Prevent excessive preloading
            .forEach { url ->

                val req = ImageRequest.Builder(context)
                    .data(url)             // Target image URL
                    .allowHardware(true)   // Enables hardware acceleration when supported
                    .build()

                // Enqueue request into Coil’s ImageLoader cache
                context.imageLoader.enqueue(req)
            }
    }
}
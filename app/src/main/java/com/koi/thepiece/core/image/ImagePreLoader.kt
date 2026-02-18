package com.koi.thepiece.core.image

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest

object ImagePreloader {
    fun preload(context: Context, urls: List<String>, limit: Int = 30) {
        urls.asSequence().filter { it.isNotBlank() }.take(limit).forEach { url ->
            val req = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(true)
                .build()
            context.imageLoader.enqueue(req)
        }
    }
}

package com.livetvpro.app.utils

import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import coil.Coil
import coil.load
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.transform.CircleCropTransformation

object GlideExtensions {

    fun loadImage(
        imageView: ImageView,
        url: String?,
        placeholderResId: Int? = null,
        errorResId: Int? = null,
        isCircular: Boolean = false
    ) {
        if (url.isNullOrEmpty()) {
            if (placeholderResId != null) imageView.setImageResource(placeholderResId)
            return
        }

        if (imageView.tag == url) return
        imageView.tag = url

        val cacheKey = MemoryCache.Key(url)
        val cached = Coil.imageLoader(imageView.context).memoryCache?.get(cacheKey)
        if (cached != null) {
            val drawable = BitmapDrawable(imageView.resources, cached.bitmap)
            if (isCircular) {
                imageView.load(drawable) {
                    memoryCachePolicy(CachePolicy.DISABLED)
                    diskCachePolicy(CachePolicy.DISABLED)
                    transformations(CircleCropTransformation())
                }
            } else {
                imageView.setImageDrawable(drawable)
            }
            return
        }

        imageView.load(url) {
            diskCachePolicy(CachePolicy.ENABLED)
            memoryCachePolicy(CachePolicy.ENABLED)
            if (placeholderResId != null) placeholder(placeholderResId)
            if (errorResId != null) error(errorResId)
            if (isCircular) transformations(CircleCropTransformation())
        }
    }
}

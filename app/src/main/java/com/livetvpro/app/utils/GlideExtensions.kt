package com.livetvpro.app.utils

import android.widget.ImageView
import coil.load
import coil.request.CachePolicy
import coil.size.Size
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
            else imageView.setImageDrawable(null)
            return
        }

        imageView.load(url) {
            diskCachePolicy(CachePolicy.ENABLED)
            memoryCachePolicy(CachePolicy.ENABLED)
            crossfade(false)
            size(Size.ORIGINAL)
            if (placeholderResId != null) placeholder(placeholderResId)
            if (errorResId != null) error(errorResId)
            if (isCircular) transformations(CircleCropTransformation())
        }
    }
}

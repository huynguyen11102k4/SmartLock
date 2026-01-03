package com.example.smartlock.extension
import android.widget.ImageView
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest

fun ImageView.loadSvg(svgString: String) {
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(SvgDecoder.Factory())
        }
        .build()

    val request = ImageRequest.Builder(context)
        .data(svgString.toByteArray())
        .target(this)
        .build()

    imageLoader.enqueue(request)
}
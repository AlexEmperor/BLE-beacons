package com.example.bleapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.caverock.androidsvg.SVG

/**
 * Загружает план (PNG или SVG) из assets как Bitmap, ограничивая размер
 * максимальной стороной [maxDim] во избежание OOM на больших файлах.
 */
fun loadPlanBitmap(context: Context, assetPath: String, isSvg: Boolean, maxDim: Int = 2048): Bitmap? {
    return try {
        if (isSvg) loadSvg(context, assetPath, maxDim) else loadPng(context, assetPath, maxDim)
    } catch (t: Throwable) {
        null
    }
}

private fun loadPng(context: Context, assetPath: String, maxDim: Int): Bitmap? {
    val am = context.assets
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    am.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) return null

    var sample = 1
    while (w / sample > maxDim || h / sample > maxDim) sample *= 2

    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return am.open(assetPath).use { BitmapFactory.decodeStream(it, null, opts) }
}

private fun loadSvg(context: Context, assetPath: String, maxDim: Int): Bitmap? {
    val svg = context.assets.open(assetPath).use { SVG.getFromInputStream(it) }
    val docW = svg.documentWidth.let { if (it > 0) it else maxDim.toFloat() }
    val docH = svg.documentHeight.let { if (it > 0) it else maxDim.toFloat() }
    val scale = (maxDim.toFloat() / maxOf(docW, docH)).coerceAtMost(1f)
    val outW = (docW * scale).toInt().coerceAtLeast(1)
    val outH = (docH * scale).toInt().coerceAtLeast(1)

    svg.documentWidth = outW.toFloat()
    svg.documentHeight = outH.toFloat()

    val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)
    svg.renderToCanvas(canvas)
    return bmp
}

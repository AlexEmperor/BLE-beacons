package com.example.bleapp.ui.map

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Intellect3DView(assetPath: String, floorIndex: Int, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.domStorageEnabled = true
                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                tag = -1
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        val target = (view.tag as? Int) ?: 0
                        view.evaluateJavascript("window.setFloor && window.setFloor($target);", null)
                    }
                }
                loadUrl("file:///android_asset/$assetPath")
            }
        },
        update = { view ->
            view.tag = floorIndex
            view.evaluateJavascript("window.setFloor && window.setFloor($floorIndex);", null)
        }
    )
}

package com.example.bleapp.ui.map

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.bleapp.data.Beacon
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MoscowMapScreen(
    beacons: List<Beacon>,
    centerLat: Double,
    centerLon: Double
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember(context) {
        Configuration.getInstance().load(
            context.applicationContext,
            PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        )
        Configuration.getInstance().userAgentValue = context.packageName

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            controller.setZoom(11.0)
            controller.setCenter(GeoPoint(centerLat, centerLon))
        }
    }

    val myLocationOverlay = remember(mapView) {
        MyLocationNewOverlay(GpsMyLocationProvider(context.applicationContext), mapView).apply {
            enableMyLocation()
            runOnFirstFix {
                mapView.post {
                    myLocation?.let {
                        mapView.controller.animateTo(it)
                        mapView.controller.setZoom(15.0)
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    myLocationOverlay.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    myLocationOverlay.disableMyLocation()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            myLocationOverlay.disableMyLocation()
            mapView.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { map ->
                map.overlays.removeAll { it is Marker }
                if (!map.overlays.contains(myLocationOverlay)) {
                    map.overlays.add(myLocationOverlay)
                }

                beacons.forEach { beacon ->
                    val lat = beacon.latitude.toDouble()
                    val lon = beacon.longitude.toDouble()
                    if (lat == 0.0 && lon == 0.0) return@forEach
                    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return@forEach

                    val marker = Marker(map).apply {
                        position = GeoPoint(lat, lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = beacon.id
                        snippet = "MAC: ${beacon.mac}\nRSSI: ${beacon.rssi} dBm"
                        icon = buildBeaconIcon(map.resources, AndroidColor.parseColor("#00E5FF"))
                    }
                    map.overlays.add(marker)
                }
                map.invalidate()
            }
        )

        IconButton(
            onClick = {
                val fix = myLocationOverlay.myLocation
                if (fix != null) {
                    mapView.controller.animateTo(fix)
                    mapView.controller.setZoom(17.0)
                } else {
                    myLocationOverlay.enableMyLocation()
                    myLocationOverlay.runOnFirstFix {
                        mapView.post {
                            myLocationOverlay.myLocation?.let {
                                mapView.controller.animateTo(it)
                                mapView.controller.setZoom(17.0)
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xE6121826))
        ) {
            MyLocationCrosshair(color = Color(0xFF00E5FF))
        }
    }
}

@Composable
private fun MyLocationCrosshair(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val stroke = size.minDimension * 0.10f
        val r = size.minDimension * 0.32f

        drawCircle(color, radius = r, center = Offset(cx, cy), style = Stroke(width = stroke))
        drawCircle(color, radius = stroke * 1.4f, center = Offset(cx, cy))

        val tickOut = size.minDimension * 0.50f
        val tickIn = size.minDimension * 0.40f
        drawLine(color, Offset(cx, cy - tickOut), Offset(cx, cy - tickIn), strokeWidth = stroke)
        drawLine(color, Offset(cx, cy + tickIn), Offset(cx, cy + tickOut), strokeWidth = stroke)
        drawLine(color, Offset(cx - tickOut, cy), Offset(cx - tickIn, cy), strokeWidth = stroke)
        drawLine(color, Offset(cx + tickIn, cy), Offset(cx + tickOut, cy), strokeWidth = stroke)
    }
}

/**
 * Иконка маяка для osmdroid: маленькая антенна, излучающая две дуги-волны.
 * Рисуем в bitmap нужного размера, чтобы оно одинаково выглядело на всех DPI.
 */
private fun buildBeaconIcon(resources: Resources, color: Int): Drawable {
    val density = resources.displayMetrics.density
    val sizePx = (40 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = (color and 0x00FFFFFF) or 0x33000000
    }
    canvas.drawCircle(cx, cy, sizePx * 0.46f, haloPaint)

    val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        this.color = color
    }

    fun drawWaves(centerY: Float) {
        val r1 = sizePx * 0.18f
        val r2 = sizePx * 0.30f
        arcPaint.strokeWidth = sizePx * 0.06f
        val rect1 = RectF(cx - r1, centerY - r1, cx + r1, centerY + r1)
        val rect2 = RectF(cx - r2, centerY - r2, cx + r2, centerY + r2)
        canvas.drawArc(rect1, 200f, 140f, false, arcPaint)
        canvas.drawArc(rect2, 205f, 130f, false, arcPaint)
    }
    drawWaves(cy + sizePx * 0.10f)

    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = AndroidColor.WHITE
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }
    canvas.drawCircle(cx, cy + sizePx * 0.10f, sizePx * 0.10f, whitePaint)
    canvas.drawCircle(cx, cy + sizePx * 0.10f, sizePx * 0.075f, dotPaint)

    return BitmapDrawable(resources, bitmap)
}

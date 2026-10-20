package com.unaj.appmoviles.obratrack.core.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.unaj.appmoviles.obratrack.core.ui.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

@Composable
fun MapPreviewCard(
    latitud: Double?,
    longitud: Double?,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.ui_map_title_default),
    bottomActions: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if ((latitud != null) && (longitud != null)) {
                val mapView = remember {
                    Configuration.getInstance().apply {
                        userAgentValue = "ObraTrack/1.0 (" + context.packageName + ")"
                        osmdroidBasePath = File(context.cacheDir, "osmdroid")
                        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
                    }
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(false)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        isTilesScaledToDpi = true
                        controller.setZoom(15.0)

                        // Do not swallow touches; allow parent scrollable containers to scroll smoothly
                        setOnTouchListener { _, _ -> false }

                        val copyrightOverlay = CopyrightOverlay(context)
                        overlays.add(copyrightOverlay)
                    }
                }

                DisposableEffect(lifecycleOwner, mapView) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> mapView.onResume()
                            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        mapView.onDetach()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AndroidView(
                        factory = { mapView },
                        update = { map ->
                            val geoPoint = GeoPoint(latitud, longitud)
                            map.controller.setCenter(geoPoint)

                            map.overlays.removeAll { it is Marker }
                            val marker = Marker(map)
                            marker.position = geoPoint
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.infoWindow = null
                            ContextCompat.getDrawable(context, R.drawable.ic_map_pin_custom)?.let { drawable ->
                                marker.icon = drawable
                            }
                            map.overlays.add(marker)

                            map.invalidate()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }

                InfoRow(
                    icon = Icons.Default.LocationOn,
                    text = stringResource(R.string.ui_map_coordinates_format, latitud, longitud)
                )

                FilledTonalButton(
                    onClick = {
                        val geoUri = Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud(${Uri.encode(title)})")
                        context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_map_open_in_google_maps))
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.ui_map_no_location),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (bottomActions != null) {
                Spacer(modifier = Modifier.height(4.dp))
                bottomActions()
            }
        }
    }
}

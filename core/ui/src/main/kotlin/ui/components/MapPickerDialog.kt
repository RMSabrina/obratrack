package com.unaj.appmoviles.obratrack.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.unaj.appmoviles.obratrack.core.ui.R
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerDialog(
    initialLatitud: Double?,
    initialLongitud: Double?,
    onDismissRequest: () -> Unit,
    onLocationSelected: (latitud: Double, longitud: Double) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.ui_map_picker_title),
    defaultCenterLatitud: Double = -34.7825,
    defaultCenterLongitud: Double = -58.2686,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedGeoPoint by remember {
        mutableStateOf(
            if (initialLatitud != null && initialLongitud != null) {
                GeoPoint(initialLatitud, initialLongitud)
            } else {
                null
            }
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding(),
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.ui_map_picker_cancel)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedGeoPoint?.let { point ->
                            Text(
                                text = stringResource(
                                    R.string.ui_map_coordinates_format,
                                    point.latitude,
                                    point.longitude
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } ?: Text(
                            text = stringResource(R.string.ui_map_picker_instructions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onDismissRequest,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.ui_map_picker_cancel))
                            }
                            Button(
                                onClick = {
                                    selectedGeoPoint?.let { point ->
                                        onLocationSelected(point.latitude, point.longitude)
                                    }
                                },
                                enabled = selectedGeoPoint != null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.ui_map_picker_confirm))
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val mapView = remember {
                    Configuration.getInstance().apply {
                        userAgentValue = "ObraTrack/1.0 (" + context.packageName + ")"
                        osmdroidBasePath = File(context.cacheDir, "osmdroid")
                        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
                    }
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
                        isTilesScaledToDpi = true

                        val initialCenter = selectedGeoPoint ?: GeoPoint(defaultCenterLatitud, defaultCenterLongitud)
                        controller.setZoom(if (selectedGeoPoint != null) 16.0 else 14.0)
                        controller.setCenter(initialCenter)

                        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                if (p != null) {
                                    selectedGeoPoint = p
                                }
                                return true
                            }

                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                return false
                            }
                        })
                        overlays.add(eventsOverlay)

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

                AndroidView(
                    factory = { mapView },
                    update = { map ->
                        map.overlays.removeAll { it is Marker }
                        selectedGeoPoint?.let { point ->
                            val marker = Marker(map)
                            marker.position = point
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.infoWindow = null
                            ContextCompat.getDrawable(context, R.drawable.ic_map_pin_custom)?.let { drawable ->
                                marker.icon = drawable
                            }
                            map.overlays.add(marker)
                        }
                        map.invalidate()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.ui_map_picker_instructions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

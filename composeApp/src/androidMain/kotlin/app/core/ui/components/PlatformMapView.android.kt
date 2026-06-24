package app.core.ui.components

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.core.model.MapMarkerUiModel
import app.core.ui.theme.WindklarTheme
import app.core.ui.theme.toHexRgb
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import windklar.composeapp.generated.resources.Res

@Composable
actual fun PlatformMapView(
    centerLat: Double,
    centerLon: Double,
    zoomLevel: Float,
    markers: List<MapMarkerUiModel>,
    selectedParkId: String?,
    onMapMoved: (lat: Double, lon: Double, zoom: Float) -> Unit,
    onMapMovedWithBounds: ((
        lat: Double,
        lon: Double,
        zoom: Float,
        swLat: Double,
        swLon: Double,
        neLat: Double,
        neLon: Double,
    ) -> Unit)?,
    onParkClicked: (String) -> Unit,
    onClusterClicked: (lat: Double, lon: Double) -> Unit,
    onPlacementPinDragged: ((lat: Double, lon: Double) -> Unit)?,
    modifier: Modifier
) {
    var isPageLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    var leafletCss by remember { mutableStateOf<String?>(null) }
    var leafletJs by remember { mutableStateOf<String?>(null) }

    val currentOnMapMoved = rememberUpdatedState(onMapMoved)
    val currentOnMapMovedWithBounds = rememberUpdatedState(onMapMovedWithBounds)
    val currentOnParkClicked = rememberUpdatedState(onParkClicked)
    val currentOnClusterClicked = rememberUpdatedState(onClusterClicked)
    val currentOnPlacementPinDragged = rememberUpdatedState(onPlacementPinDragged)

    LaunchedEffect(Unit) {
        try {
            val css = Res.readBytes("files/leaflet/leaflet.css").decodeToString()
            val js = Res.readBytes("files/leaflet/leaflet.js").decodeToString()
            println("PlatformMapView: Loaded Leaflet CSS (${css.length} chars) and JS (${js.length} chars)")
            leafletCss = css
            leafletJs = js
        } catch (e: Exception) {
            println("PlatformMapView ERROR: Failed to load local Leaflet assets!")
            e.printStackTrace()
        }
    }

    val colors = WindklarTheme.colors
    val bg = colors.screenBackground.toHexRgb()
    val primary = colors.primaryGreen.toHexRgb()
    val white = colors.cardBackground.toHexRgb()
    val error = colors.errorRed.toHexRgb()
    val teal = colors.turbineTeal.toHexRgb()

    val htmlContent = remember(leafletCss, leafletJs) {
        val css = leafletCss ?: ""
        val js = leafletJs ?: ""
        val centerLatDefault = 51.1657
        val centerLonDefault = 10.4515
        val zoomDefault = 6.0f
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                $css
                
                body, html {
                    margin: 0; padding: 0; width: 100%; height: 100%;
                    background: $bg;
                }
                #map {
                    width: 100%; height: 100%;
                }
                #offline-message {
                    display: none;
                    padding: 20px;
                    text-align: center;
                    font-family: sans-serif;
                    color: $primary;
                    margin-top: 100px;
                }
                .leaflet-control-zoom { display: none !important; }
                .leaflet-control-attribution { font-size: 8px !important; }
                .windklar-cluster {
                    width: 30px;
                    height: 30px;
                    border-radius: 999px;
                    background: $primary;
                    border: 2px solid $white;
                    color: $white;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-family: sans-serif;
                    font-weight: 700;
                    font-size: 11px;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.25);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <div id="offline-message">
                <h3>Karte kann nicht geladen werden</h3>
                <p>Bitte &Uuml;berpr&uuml;fen Sie Ihre Internetverbindung.</p>
            </div>
            <script>
                $js
            </script>
            <script>
                var map;
                var markersGroup;
                
                try {
                    map = L.map('map', {
                        zoomControl: false,
                        maxZoom: 18,
                        minZoom: 5,
                        preferCanvas: true
                    }).setView([$centerLatDefault, $centerLonDefault], $zoomDefault);

                    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
                        referrerPolicy: 'origin'
                    }).addTo(map);

                    markersGroup = L.layerGroup().addTo(map);

                    function notifyMove() {
                        var center = map.getCenter();
                        var zoom = map.getZoom();
                        var bounds = map.getBounds();
                        var sw = bounds.getSouthWest();
                        var ne = bounds.getNorthEast();
                        if (window.AndroidBridge && window.AndroidBridge.onMapMoved) {
                            window.AndroidBridge.onMapMoved(center.lat, center.lng, zoom, sw.lat, sw.lng, ne.lat, ne.lng);
                        }
                    }

                    map.on('moveend', notifyMove);
                    
                    // Immediately try loading markers since Leaflet is ready
                    updateParksFromAndroid();
                } catch (e) {
                    console.error("Leaflet initialization failed", e);
                    document.getElementById('map').style.display = 'none';
                    document.getElementById('offline-message').style.display = 'block';
                }

                function setCenter(lat, lon, zoom) {
                    if (!map) return;
                    map.invalidateSize();
                    var currentCenter = map.getCenter();
                    var currentZoom = map.getZoom();
                    if (Math.abs(currentCenter.lat - lat) > 0.0001 || 
                        Math.abs(currentCenter.lng - lon) > 0.0001 || 
                        Math.abs(currentZoom - zoom) > 0.1) {
                        map.setView([lat, lon], zoom);
                    }
                }

                function updateParks(markersJson, selectedId) {
                    if (!map) return;
                    map.invalidateSize();
                    if (!markersGroup) return;
                    
                    var markers = JSON.parse(markersJson);
                    var leafletMarkers = [];
                    
                    markers.forEach(function(item) {
                        if (item.kind === 'Cluster') {
                            var label = formatClusterLabel(item.count);
                            var clusterMarker = L.marker([item.latitude, item.longitude], {
                                icon: L.divIcon({
                                    className: '',
                                    html: '<div class="windklar-cluster">' + label + '</div>',
                                    iconSize: [30, 30],
                                    iconAnchor: [15, 15]
                                })
                            });
                            clusterMarker.on('click', function() {
                                if (window.AndroidBridge && window.AndroidBridge.onClusterClicked) {
                                    window.AndroidBridge.onClusterClicked(item.latitude, item.longitude);
                                }
                            });
                            leafletMarkers.push(clusterMarker);
                        } else if (item.kind === 'Turbine') {
                            var isSelected = item.parkId === selectedId;
                            var turbineIcon = L.divIcon({
                                className: '',
                                html: '<div style="width: 14px; height: 14px; background: ' + (isSelected ? '$error' : '$teal') + '; border: 1.5px solid $white; border-radius: 50%; box-shadow: 0 1px 4px rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center;"><div style="width: 4px; height: 4px; background: $white; border-radius: 50%;"></div></div>',
                                iconSize: [14, 14],
                                iconAnchor: [7, 7]
                            });
                            var turbineMarker = L.marker([item.latitude, item.longitude], { icon: turbineIcon });
                            turbineMarker.on('click', function() {
                                if (window.AndroidBridge && window.AndroidBridge.onParkClicked) {
                                    window.AndroidBridge.onParkClicked(item.parkId || item.id);
                                }
                            });
                            leafletMarkers.push(turbineMarker);
                        } else if (item.kind === 'PlacementPin') {
                            var placementIcon = L.divIcon({
                                className: '',
                                html: '<div style="width: 34px; height: 42px; display: flex; align-items: flex-start; justify-content: center; filter: drop-shadow(0 3px 5px rgba(0,0,0,0.35));"><svg width="34" height="42" viewBox="0 0 34 42" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M17 40C17 40 31 24.9 31 14.8C31 6.6 24.7 1 17 1C9.3 1 3 6.6 3 14.8C3 24.9 17 40 17 40Z" fill="$error" stroke="$white" stroke-width="2"/><circle cx="17" cy="15" r="5.8" fill="$white"/></svg></div>',
                                iconSize: [34, 42],
                                iconAnchor: [17, 40]
                            });
                            var placementMarker = L.marker([item.latitude, item.longitude], {
                                draggable: true,
                                icon: placementIcon
                            });
                            placementMarker.on('dragend', function(e) {
                                var latlng = placementMarker.getLatLng();
                                if (window.AndroidBridge && window.AndroidBridge.onPlacementPinDragged) {
                                    window.AndroidBridge.onPlacementPinDragged(latlng.lat, latlng.lng);
                                }
                            });
                            leafletMarkers.push(placementMarker);
                        } else {
                            var isSelected = item.parkId === selectedId;
                            var parkMarker = L.circleMarker([item.latitude, item.longitude], {
                                radius: isSelected ? 8 : 4,
                                fillColor: isSelected ? '$error' : '$primary',
                                color: '$white',
                                weight: 2,
                                fillOpacity: 1.0,
                                opacity: 1.0
                            });
                            
                            parkMarker.on('click', function() {
                                if (window.AndroidBridge && window.AndroidBridge.onParkClicked) {
                                    window.AndroidBridge.onParkClicked(item.parkId || item.id);
                                }
                            });
                            leafletMarkers.push(parkMarker);
                        }
                    });
                    
                    map.removeLayer(markersGroup);
                    markersGroup = L.layerGroup(leafletMarkers).addTo(map);
                }

                function formatClusterLabel(count) {
                    if (count >= 1000) {
                        return Math.round(count / 1000) + 'k';
                    }
                    return String(count);
                }

                function updateParksFromAndroid() {
                    if (window.AndroidBridge && window.AndroidBridge.getParksJson) {
                        var json = window.AndroidBridge.getParksJson();
                        var selectedId = window.AndroidBridge.getSelectedParkId();
                        updateParks(json, selectedId);
                    }
                }

                window.onload = function() {
                    if (map) {
                        map.invalidateSize();
                    }
                    if (window.AndroidBridge && window.AndroidBridge.onMapReady) {
                        window.AndroidBridge.onMapReady();
                    }
                };

                // Keep Leaflet sizing in sync with Compose measuring layout passes
                var resizeObserver = new ResizeObserver(function() {
                    if (map) {
                        map.invalidateSize();
                    }
                });
                resizeObserver.observe(document.getElementById('map'));
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    val jsonString = remember(markers) {
        val arr = buildJsonArray {
            markers.forEach { marker ->
                add(buildJsonObject {
                    put("id", marker.id)
                    put("latitude", marker.latitude)
                    put("longitude", marker.longitude)
                    put("kind", marker.kind.name)
                    put("count", marker.count)
                    put("parkId", marker.parkId ?: "")
                })
            }
        }
        arr.toString()
    }

    val currentParksJson = rememberUpdatedState(jsonString)
    val currentSelectedParkId = rememberUpdatedState(selectedParkId ?: "")

    // Effect to update map center and zoom level (camera target)
    LaunchedEffect(webViewRef, isPageLoaded, centerLat, centerLon, zoomLevel) {
        val webView = webViewRef ?: return@LaunchedEffect
        if (isPageLoaded) {
            webView.evaluateJavascript("setCenter($centerLat, $centerLon, $zoomLevel)", null)
        }
    }

    // Effect to update park markers
    LaunchedEffect(webViewRef, isPageLoaded, jsonString, selectedParkId) {
        val webView = webViewRef ?: return@LaunchedEffect
        if (isPageLoaded) {
            webView.evaluateJavascript("updateParksFromAndroid()", null)
        }
    }

    if (leafletCss != null && leafletJs != null) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "${settings.userAgentString} WindKlar/1.0 product.lifecycle.windenergy"
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            mainHandler.postDelayed({
                                isPageLoaded = true
                            }, 500)
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            val msg = consoleMessage?.message() ?: ""
                            val line = consoleMessage?.lineNumber() ?: 0
                            val source = consoleMessage?.sourceId() ?: ""
                            println("WebView Console: $msg (at $source:$line)")
                            return true
                        }
                    }

                    val bridge = AndroidMapBridge(
                        parksJsonProvider = { currentParksJson.value },
                        selectedParkIdProvider = { currentSelectedParkId.value },
                        onMapMovedWithBoundsCallback = { lat, lon, zoom, swLat, swLon, neLat, neLon ->
                            currentOnMapMovedWithBounds.value?.invoke(lat, lon, zoom, swLat, swLon, neLat, neLon)
                                ?: currentOnMapMoved.value(lat, lon, zoom)
                        },
                        onParkClickedCallback = { id -> currentOnParkClicked.value(id) },
                        onClusterClickedCallback = { lat, lon -> currentOnClusterClicked.value(lat, lon) },
                        onPlacementPinDraggedCallback = { lat, lon -> currentOnPlacementPinDragged.value?.invoke(lat, lon) },
                        onMapReadyCallback = { isPageLoaded = true },
                        mainHandler = mainHandler
                    )
                    addJavascriptInterface(bridge, "AndroidBridge")
                    
                    webViewRef = this
                    loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = {
                // All updates are handled reactively in LaunchedEffect blocks to avoid redundant evaluations
            },
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = WindklarTheme.colors.primaryGreen)
        }
    }
}

class AndroidMapBridge(
    private val parksJsonProvider: () -> String,
    private val selectedParkIdProvider: () -> String,
    private val onMapMovedWithBoundsCallback: (
        lat: Double,
        lon: Double,
        zoom: Float,
        swLat: Double,
        swLon: Double,
        neLat: Double,
        neLon: Double,
    ) -> Unit,
    private val onParkClickedCallback: (String) -> Unit,
    private val onClusterClickedCallback: (lat: Double, lon: Double) -> Unit,
    private val onPlacementPinDraggedCallback: (lat: Double, lon: Double) -> Unit,
    private val onMapReadyCallback: () -> Unit,
    private val mainHandler: android.os.Handler
) {
    @JavascriptInterface
    fun getParksJson(): String = parksJsonProvider()

    @JavascriptInterface
    fun getSelectedParkId(): String = selectedParkIdProvider()

    @JavascriptInterface
    fun onMapMoved(
        lat: Double,
        lon: Double,
        zoom: Float,
        swLat: Double,
        swLon: Double,
        neLat: Double,
        neLon: Double,
    ) {
        mainHandler.post { onMapMovedWithBoundsCallback(lat, lon, zoom, swLat, swLon, neLat, neLon) }
    }

    @JavascriptInterface
    fun onParkClicked(id: String) {
        mainHandler.post { onParkClickedCallback(id) }
    }

    @JavascriptInterface
    fun onClusterClicked(lat: Double, lon: Double) {
        mainHandler.post { onClusterClickedCallback(lat, lon) }
    }

    @JavascriptInterface
    fun onPlacementPinDragged(lat: Double, lon: Double) {
        mainHandler.post { onPlacementPinDraggedCallback(lat, lon) }
    }

    @JavascriptInterface
    fun onMapReady() {
        mainHandler.post { onMapReadyCallback() }
    }
}

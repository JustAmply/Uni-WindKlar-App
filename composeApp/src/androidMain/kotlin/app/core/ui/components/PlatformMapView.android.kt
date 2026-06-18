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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import app.core.model.WindPark
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import windklar.composeapp.generated.resources.Res

@Composable
actual fun PlatformMapView(
    centerLat: Double,
    centerLon: Double,
    zoomLevel: Float,
    parks: List<WindPark>,
    selectedParkId: String?,
    onMapMoved: (lat: Double, lon: Double, zoom: Float) -> Unit,
    onParkClicked: (String) -> Unit,
    modifier: Modifier
) {
    var isPageLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    var leafletCss by remember { mutableStateOf<String?>(null) }
    var leafletJs by remember { mutableStateOf<String?>(null) }

    val currentOnMapMoved = rememberUpdatedState(onMapMoved)
    val currentOnParkClicked = rememberUpdatedState(onParkClicked)

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
                    background: #F8FAF7;
                }
                #map {
                    width: 100%; height: 100%;
                }
                #offline-message {
                    display: none;
                    padding: 20px;
                    text-align: center;
                    font-family: sans-serif;
                    color: #2D5A2D;
                    margin-top: 100px;
                }
                .leaflet-control-zoom { display: none !important; }
                .leaflet-control-attribution { font-size: 8px !important; }
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

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    }).addTo(map);

                    markersGroup = L.layerGroup().addTo(map);

                    function notifyMove() {
                        var center = map.getCenter();
                        var zoom = map.getZoom();
                        if (window.AndroidBridge && window.AndroidBridge.onMapMoved) {
                            window.AndroidBridge.onMapMoved(center.lat, center.lng, zoom);
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

                function updateParks(parksJson, selectedId) {
                    if (!map) return;
                    map.invalidateSize();
                    if (!markersGroup) return;
                    
                    var parks = JSON.parse(parksJson);
                    var markers = [];
                    
                    parks.forEach(function(park) {
                        var isSelected = park.id === selectedId;
                        var marker = L.circleMarker([park.latitude, park.longitude], {
                            radius: isSelected ? 8 : 4,
                            fillColor: isSelected ? '#D32F2F' : '#2D5A2D',
                            color: '#FFFFFF',
                            weight: 2,
                            fillOpacity: 1.0,
                            opacity: 1.0
                        });
                        
                        marker.on('click', function() {
                            if (window.AndroidBridge && window.AndroidBridge.onParkClicked) {
                                window.AndroidBridge.onParkClicked(park.id);
                            }
                        });
                        markers.push(marker);
                    });
                    
                    map.removeLayer(markersGroup);
                    markersGroup = L.layerGroup(markers).addTo(map);
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

    val jsonString = remember(parks) {
        val arr = buildJsonArray {
            parks.forEach { park ->
                add(buildJsonObject {
                    put("id", park.id)
                    put("latitude", park.latitude)
                    put("longitude", park.longitude)
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
                        onMapMovedCallback = { lat, lon, zoom -> currentOnMapMoved.value(lat, lon, zoom) },
                        onParkClickedCallback = { id -> currentOnParkClicked.value(id) },
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
            CircularProgressIndicator(color = Color(0xFF2D5A2D))
        }
    }
}

class AndroidMapBridge(
    private val parksJsonProvider: () -> String,
    private val selectedParkIdProvider: () -> String,
    private val onMapMovedCallback: (lat: Double, lon: Double, zoom: Float) -> Unit,
    private val onParkClickedCallback: (String) -> Unit,
    private val onMapReadyCallback: () -> Unit,
    private val mainHandler: android.os.Handler
) {
    @JavascriptInterface
    fun getParksJson(): String = parksJsonProvider()

    @JavascriptInterface
    fun getSelectedParkId(): String = selectedParkIdProvider()

    @JavascriptInterface
    fun onMapMoved(lat: Double, lon: Double, zoom: Float) {
        mainHandler.post { onMapMovedCallback(lat, lon, zoom) }
    }

    @JavascriptInterface
    fun onParkClicked(id: String) {
        mainHandler.post { onParkClickedCallback(id) }
    }

    @JavascriptInterface
    fun onMapReady() {
        mainHandler.post { onMapReadyCallback() }
    }
}


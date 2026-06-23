package app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import app.core.model.MapMarkerUiModel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.CoreGraphics.CGRectZero
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigation
import platform.darwin.NSObject
import windklar.composeapp.generated.resources.Res

private const val WindklarMapUserAgent = "WindKlar/1.0 (product.lifecycle.windenergy; iOS)"

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapView(
    centerLat: Double,
    centerLon: Double,
    zoomLevel: Float,
    markers: List<MapMarkerUiModel>,
    selectedParkId: String?,
    onMapMoved: (lat: Double, lon: Double, zoom: Float) -> Unit,
    onParkClicked: (String) -> Unit,
    onClusterClicked: (lat: Double, lon: Double) -> Unit,
    onPlacementPinDragged: ((lat: Double, lon: Double) -> Unit)?,
    modifier: Modifier
) {
    var isPageLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WKWebView?>(null) }
    val currentOnMapMoved = rememberUpdatedState(onMapMoved)
    val currentOnParkClicked = rememberUpdatedState(onParkClicked)
    val currentOnClusterClicked = rememberUpdatedState(onClusterClicked)
    val currentOnPlacementPinDragged = rememberUpdatedState(onPlacementPinDragged)

    var leafletCss by remember { mutableStateOf<String?>(null) }
    var leafletJs by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            leafletCss = Res.readBytes("files/leaflet/leaflet.css").decodeToString()
            leafletJs = Res.readBytes("files/leaflet/leaflet.js").decodeToString()
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
                .windklar-cluster {
                    width: 30px;
                    height: 30px;
                    border-radius: 999px;
                    background: #2D5A2D;
                    border: 2px solid #FFFFFF;
                    color: #FFFFFF;
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
                        var data = { type: 'move', lat: center.lat, lon: center.lng, zoom: zoom };
                        if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosBridge) {
                            window.webkit.messageHandlers.iosBridge.postMessage(JSON.stringify(data));
                        }
                    }

                    map.on('moveend', notifyMove);
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
                                var data = { type: 'cluster', lat: item.latitude, lon: item.longitude };
                                if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosBridge) {
                                    window.webkit.messageHandlers.iosBridge.postMessage(JSON.stringify(data));
                                }
                            });
                            leafletMarkers.push(clusterMarker);
                        } else if (item.kind === 'Turbine') {
                            var isSelected = item.parkId === selectedId;
                            var turbineIcon = L.divIcon({
                                className: '',
                                html: '<div style="width: 14px; height: 14px; background: ' + (isSelected ? '#D32F2F' : '#009688') + '; border: 1.5px solid #FFFFFF; border-radius: 50%; box-shadow: 0 1px 4px rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center;"><div style="width: 4px; height: 4px; background: white; border-radius: 50%;"></div></div>',
                                iconSize: [14, 14],
                                iconAnchor: [7, 7]
                            });
                            var turbineMarker = L.marker([item.latitude, item.longitude], { icon: turbineIcon });
                            turbineMarker.on('click', function() {
                                var data = { type: 'click', parkId: item.parkId || item.id };
                                if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosBridge) {
                                    window.webkit.messageHandlers.iosBridge.postMessage(JSON.stringify(data));
                                }
                            });
                            leafletMarkers.push(turbineMarker);
                        } else if (item.kind === 'PlacementPin') {
                            var placementIcon = L.divIcon({
                                className: '',
                                html: '<div style="width: 34px; height: 42px; display: flex; align-items: flex-start; justify-content: center; filter: drop-shadow(0 3px 5px rgba(0,0,0,0.35));"><svg width="34" height="42" viewBox="0 0 34 42" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M17 40C17 40 31 24.9 31 14.8C31 6.6 24.7 1 17 1C9.3 1 3 6.6 3 14.8C3 24.9 17 40 17 40Z" fill="#D32F2F" stroke="#FFFFFF" stroke-width="2"/><circle cx="17" cy="15" r="5.8" fill="#FFFFFF"/></svg></div>',
                                iconSize: [34, 42],
                                iconAnchor: [17, 40]
                            });
                            var placementMarker = L.marker([item.latitude, item.longitude], {
                                draggable: true,
                                icon: placementIcon
                            });
                            placementMarker.on('dragend', function(e) {
                                var latlng = placementMarker.getLatLng();
                                var data = { type: 'dragend', lat: latlng.lat, lon: latlng.lng };
                                if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosBridge) {
                                    window.webkit.messageHandlers.iosBridge.postMessage(JSON.stringify(data));
                                }
                            });
                            leafletMarkers.push(placementMarker);
                        } else {
                            var isSelected = item.parkId === selectedId;
                            var parkMarker = L.circleMarker([item.latitude, item.longitude], {
                                radius: isSelected ? 8 : 4,
                                fillColor: isSelected ? '#D32F2F' : '#2D5A2D',
                                color: '#FFFFFF',
                                weight: 2,
                                fillOpacity: 1.0,
                                opacity: 1.0
                            });
                            
                            parkMarker.on('click', function() {
                                var data = { type: 'click', parkId: item.parkId || item.id };
                                if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosBridge) {
                                    window.webkit.messageHandlers.iosBridge.postMessage(JSON.stringify(data));
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

                window.onload = function() {
                    if (map) {
                        map.invalidateSize();
                    }
                    if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosBridge) {
                        window.webkit.messageHandlers.iosBridge.postMessage(JSON.stringify({ type: 'ready' }));
                    }
                };
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

    val configuration = remember {
        WKWebViewConfiguration().apply {
            val handler = object : NSObject(), WKScriptMessageHandlerProtocol {
                override fun userContentController(
                    userContentController: platform.WebKit.WKUserContentController,
                    didReceiveScriptMessage: WKScriptMessage
                ) {
                    try {
                        val body = didReceiveScriptMessage.body as? String ?: return
                        val json = Json.parseToJsonElement(body)
                        val obj = json as? kotlinx.serialization.json.JsonObject ?: return
                        val type = obj["type"]?.toString()?.removeSurrounding("\"")
                        if (type == "ready") {
                            isPageLoaded = true
                        } else if (type == "move") {
                            val lat = obj["lat"]?.toString()?.toDoubleOrNull() ?: return
                            val lon = obj["lon"]?.toString()?.toDoubleOrNull() ?: return
                            val zoom = obj["zoom"]?.toString()?.toFloatOrNull() ?: return
                            onMapMoved(lat, lon, zoom)
                        } else if (type == "click") {
                            val parkId = obj["parkId"]?.toString()?.removeSurrounding("\"") ?: return
                            onParkClicked(parkId)
                        } else if (type == "cluster") {
                            val lat = obj["lat"]?.toString()?.toDoubleOrNull() ?: return
                            val lon = obj["lon"]?.toString()?.toDoubleOrNull() ?: return
                            onClusterClicked(lat, lon)
                        } else if (type == "dragend") {
                            val lat = obj["lat"]?.toString()?.toDoubleOrNull() ?: return
                            val lon = obj["lon"]?.toString()?.toDoubleOrNull() ?: return
                            val callback = currentOnPlacementPinDragged.value
                            if (callback != null) {
                                callback(lat, lon)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            userContentController.addScriptMessageHandler(handler, "iosBridge")
            applicationNameForUserAgent = WindklarMapUserAgent
        }
    }

    val navigationDelegate = remember {
        object : NSObject(), WKNavigationDelegateProtocol {
            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                // Fallback in case window.onload doesn't trigger or gets blocked
                platform.darwin.dispatch_after(
                    platform.darwin.dispatch_time(platform.darwin.DISPATCH_TIME_NOW, 500_000_000 /* 500ms in ns */),
                    platform.darwin.dispatch_get_main_queue()
                ) {
                    isPageLoaded = true
                }
            }
        }
    }

    // Effect to update map center and zoom level (camera target)
    LaunchedEffect(webViewRef, isPageLoaded, centerLat, centerLon, zoomLevel) {
        val webView = webViewRef ?: return@LaunchedEffect
        if (isPageLoaded) {
            webView.evaluateJavaScript("setCenter($centerLat, $centerLon, $zoomLevel)", null)
        }
    }

    // Effect to update park markers
    LaunchedEffect(webViewRef, isPageLoaded, jsonString, selectedParkId) {
        val webView = webViewRef ?: return@LaunchedEffect
        if (isPageLoaded) {
            val escapedJson = jsonString.replace("'", "\\'")
            val escapedSelectedId = (selectedParkId ?: "").replace("'", "\\'")
            webView.evaluateJavaScript("updateParks('$escapedJson', '$escapedSelectedId')", null)
        }
    }

    if (leafletCss != null && leafletJs != null) {
        UIKitView(
            factory = {
                WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
                    customUserAgent = WindklarMapUserAgent
                    this.navigationDelegate = navigationDelegate
                    webViewRef = this
                    loadHTMLString(htmlContent, baseURL = null)
                }
            },
            update = {
                // Managed reactively in LaunchedEffect blocks
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

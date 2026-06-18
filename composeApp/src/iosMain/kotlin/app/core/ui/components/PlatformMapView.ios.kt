package app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import app.core.model.WindPark
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigation
import platform.darwin.NSObject
import windklar.composeapp.generated.resources.Res

@OptIn(ExperimentalForeignApi::class)
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
    var webViewRef by remember { mutableStateOf<WKWebView?>(null) }

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
                            var data = { type: 'click', parkId: park.id };
                            if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosBridge) {
                                window.webkit.messageHandlers.iosBridge.postMessage(JSON.stringify(data));
                            }
                        });
                        markers.push(marker);
                    });
                    
                    map.removeLayer(markersGroup);
                    markersGroup = L.layerGroup(markers).addTo(map);
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
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            userContentController.addScriptMessageHandler(handler, "iosBridge")
        }
    }

    val navigationDelegate = remember {
        object : NSObject(), WKNavigationDelegateProtocol {
            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation) {
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
                    this.navigationDelegate = navigationDelegate
                    webViewRef = this
                    loadHTMLString(htmlContent, baseURL = NSURL.URLWithString("https://openstreetmap.org"))
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

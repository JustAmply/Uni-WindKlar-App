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
import androidx.compose.ui.interop.UIKitView
import app.core.model.MapMarkerUiModel
import app.core.ui.theme.WindklarTheme
import app.core.ui.theme.toHexRgb
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
import platform.WebKit.WKWebsiteDataStore
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
    var webViewRef by remember { mutableStateOf<WKWebView?>(null) }
    val currentOnMapMoved = rememberUpdatedState(onMapMoved)
    val currentOnMapMovedWithBounds = rememberUpdatedState(onMapMovedWithBounds)
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

    val colors = WindklarTheme.colors
    val bg = colors.screenBackground.toHexRgb()
    val primary = colors.primaryGreen.toHexRgb()
    val white = colors.cardBackground.toHexRgb()
    val error = colors.errorRed.toHexRgb()
    val teal = colors.turbineTeal.toHexRgb()

    val htmlContent = remember(leafletCss, leafletJs) {
        val css = leafletCss ?: ""
        val js = leafletJs ?: ""
        val centerLatDefault = centerLat
        val centerLonDefault = centerLon
        val zoomDefault = zoomLevel
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
                    position: absolute;
                    left: 16px;
                    right: 16px;
                    bottom: 16px;
                    z-index: 1000;
                    padding: 12px 14px;
                    font-family: sans-serif;
                    color: $primary;
                    background: rgba(255, 255, 255, 0.94);
                    border: 1px solid rgba(38, 110, 80, 0.24);
                    border-radius: 12px;
                    box-shadow: 0 3px 12px rgba(0,0,0,0.18);
                    pointer-events: none;
                }
                #offline-message h3 {
                    margin: 0 0 4px 0;
                    font-size: 14px;
                    line-height: 18px;
                }
                #offline-message p {
                    margin: 0;
                    font-size: 12px;
                    line-height: 16px;
                }
                .leaflet-control-zoom { display: none !important; }
                .leaflet-control-attribution {
                    font-size: 8px !important;
                    pointer-events: none;
                }
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
                .windklar-turbine {
                    width: 22px;
                    height: 22px;
                    color: $teal;
                    background: rgba(255, 255, 255, 0.96);
                    border: 1.5px solid $white;
                    border-radius: 999px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    box-shadow: 0 2px 7px rgba(0,0,0,0.28);
                    box-sizing: border-box;
                }
                .windklar-turbine.selected {
                    color: $error;
                    transform: scale(1.12);
                    box-shadow: 0 3px 9px rgba(0,0,0,0.34);
                }
                .windklar-turbine svg {
                    width: 18px;
                    height: 18px;
                    display: block;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <div id="offline-message">
                <h3>Basiskarte offline nicht verf&uuml;gbar</h3>
                <p>Windparkdaten, Suche und gespeicherte Eintr&auml;ge bleiben lokal nutzbar.</p>
            </div>
            <script>
                $js
            </script>
            <script>
                var map;
                var markersGroup;
                var baseTileLayer;
                var pendingBaseTiles = 0;
                var loadedBaseTiles = 0;
                var failedBaseTiles = 0;

                function notifyMove() {
                    if (!map) return;
                    var center = map.getCenter();
                    var zoom = map.getZoom();
                    var bounds = map.getBounds();
                    var sw = bounds.getSouthWest();
                    var ne = bounds.getNorthEast();
                    var data = {
                        type: 'move',
                        lat: center.lat,
                        lon: center.lng,
                        zoom: zoom,
                        swLat: sw.lat,
                        swLon: sw.lng,
                        neLat: ne.lat,
                        neLon: ne.lng
                    };
                    if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosBridge) {
                        window.webkit.messageHandlers.iosBridge.postMessage(JSON.stringify(data));
                    }
                }

                function showOfflineNotice() {
                    var message = document.getElementById('offline-message');
                    if (message) {
                        message.style.display = 'block';
                    }
                }

                function hideOfflineNotice() {
                    var message = document.getElementById('offline-message');
                    if (message) {
                        message.style.display = 'none';
                    }
                }

                function updateBaseMapAvailability() {
                    if (loadedBaseTiles > 0) {
                        hideOfflineNotice();
                    } else if (pendingBaseTiles === 0 && failedBaseTiles > 0) {
                        showOfflineNotice();
                    }
                }

                function onBaseTileLoadStart() {
                    if (pendingBaseTiles === 0) {
                        loadedBaseTiles = 0;
                        failedBaseTiles = 0;
                    }
                    pendingBaseTiles += 1;
                }

                function onBaseTileLoaded() {
                    pendingBaseTiles = Math.max(0, pendingBaseTiles - 1);
                    loadedBaseTiles += 1;
                    updateBaseMapAvailability();
                }

                function onBaseTileFailed() {
                    pendingBaseTiles = Math.max(0, pendingBaseTiles - 1);
                    failedBaseTiles += 1;
                    updateBaseMapAvailability();
                }

                function createBaseTileLayer() {
                    baseTileLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; OpenStreetMap contributors',
                        referrerPolicy: 'origin',
                        updateWhenIdle: true,
                        keepBuffer: 1
                    });
                    baseTileLayer.on('tileloadstart', onBaseTileLoadStart);
                    baseTileLayer.on('tileload', onBaseTileLoaded);
                    baseTileLayer.on('tileerror', onBaseTileFailed);
                    baseTileLayer.on('load', updateBaseMapAvailability);
                    return baseTileLayer;
                }

                try {
                    map = L.map('map', {
                        zoomControl: false,
                        attributionControl: true,
                        maxZoom: 18,
                        minZoom: 5,
                        preferCanvas: true
                    }).setView([$centerLatDefault, $centerLonDefault], $zoomDefault);

                    map.attributionControl.setPrefix(false);
                    createBaseTileLayer().addTo(map);

                    markersGroup = L.layerGroup().addTo(map);

                    map.on('moveend', notifyMove);
                    setTimeout(notifyMove, 0);
                } catch (e) {
                    console.error("Leaflet initialization failed", e);
                    showOfflineNotice();
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

                function turbineIconHtml(isSelected) {
                    var selectedClass = isSelected ? ' selected' : '';
                    return '<div class="windklar-turbine' + selectedClass + '" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg"><path d="M12 10.8V21"/><path d="M9.4 21H14.6"/><circle cx="12" cy="9" r="1.7" fill="currentColor" stroke="none"/><path d="M12 7.3V3.2"/><path d="M13.5 9.8L17.6 12.2"/><path d="M10.5 9.8L6.4 12.2"/></svg></div>';
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
                            var isSelected = selectedId && item.parkId === selectedId;
                            var turbineIcon = L.divIcon({
                                className: '',
                                html: turbineIconHtml(isSelected),
                                iconSize: [22, 22],
                                iconAnchor: [11, 11]
                            });
                            var turbineMarker = L.marker([item.latitude, item.longitude], {
                                icon: turbineIcon,
                                zIndexOffset: isSelected ? 700 : 200
                            });
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
                                fillColor: isSelected ? '$error' : '$primary',
                                color: '$white',
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
                        setTimeout(notifyMove, 0);
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
                            val swLat = obj["swLat"]?.toString()?.toDoubleOrNull()
                            val swLon = obj["swLon"]?.toString()?.toDoubleOrNull()
                            val neLat = obj["neLat"]?.toString()?.toDoubleOrNull()
                            val neLon = obj["neLon"]?.toString()?.toDoubleOrNull()
                            val boundsCallback = currentOnMapMovedWithBounds.value
                            if (
                                boundsCallback != null &&
                                swLat != null &&
                                swLon != null &&
                                neLat != null &&
                                neLon != null
                            ) {
                                boundsCallback(lat, lon, zoom, swLat, swLon, neLat, neLon)
                            } else {
                                currentOnMapMoved.value(lat, lon, zoom)
                            }
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
            websiteDataStore = WKWebsiteDataStore.defaultDataStore()
        }
    }

    val navigationDelegate = remember {
        object : NSObject(), WKNavigationDelegateProtocol {
            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                // Fallback in case window.onload doesn't trigger or gets blocked.
                isPageLoaded = true
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
            CircularProgressIndicator(color = WindklarTheme.colors.primaryGreen)
        }
    }
}

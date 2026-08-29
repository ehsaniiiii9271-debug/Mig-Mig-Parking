package com.aistudio.fitmirror.auth2.ui

import android.webkit.JavascriptInterface

class MapBridge(
    private val onMapClick: (lat: Double, lng: Double) -> Unit,
    private val onMarkerClick: (id: String) -> Unit = { _ -> }
) {
    @JavascriptInterface
    fun onMapClick(lat: Double, lng: Double) = onMapClick.invoke(lat, lng)

    @JavascriptInterface
    fun onMarkerClick(id: String) = onMarkerClick.invoke(id)
}
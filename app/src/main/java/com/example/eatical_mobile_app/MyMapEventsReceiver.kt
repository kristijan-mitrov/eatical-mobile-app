package com.example.eatical_mobile_app

import android.content.Context
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MyMapEventsReceiver(var map: MapView, var context: Context) : MapEventsReceiver {
    var point: GeoPoint? = null
    var previousMarker: Marker? = null
    var marker: Marker? = null
    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        point = p
        marker = Marker(map)
        if (p != null) {
            marker!!.position.setCoords(
                p.latitude,
                p.longitude
            )
            if (previousMarker != null)
                map.overlays.remove(previousMarker)
            previousMarker = marker
        }
        marker!!.icon = context.getResources().getDrawable(R.drawable.map_pin, null)
        marker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
        map.invalidate()
        return true
    }

    fun setDeviceLocationMarker(p: GeoPoint){
        point = p
        marker = Marker(map)
        marker!!.position.setCoords(p.latitude, p.longitude)
        marker!!.icon = context.getResources().getDrawable(R.drawable.map_pin, null)
        marker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
        map.invalidate()
        previousMarker = marker
        println("point: " + p.toString())
        println("MARKER SET")
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        println("Long press on " + p.toString())
        return true
    }
}
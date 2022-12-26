package com.example.eatical_mobile_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.example.eatical_mobile_app.databinding.ActivityMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay

class MapActivity : AppCompatActivity() {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var binding: ActivityMapBinding
    private lateinit var map: MapView
    lateinit var mapEventsReceiver: MyMapEventsReceiver
    private lateinit var mapEventsOverlay: MapEventsOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        Configuration.getInstance().load(this, getDefaultSharedPreferences(this));
        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.controller.setZoom(14.0)
        map.controller.setCenter(GeoPoint(46.5547, 15.6459))
        mapEventsReceiver = MyMapEventsReceiver(map, this)
        mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        map.overlays.add(mapEventsOverlay)

        if(latitude != 0.0 && longitude != 0.0)
            mapEventsReceiver.setDeviceLocationMarker(GeoPoint(latitude, longitude))

        binding.doneButton.setOnClickListener{
            val intent = Intent(this, MapActivity::class.java)
            if(mapEventsReceiver.point != null) {
                intent.putExtra("coordinates", mapEventsReceiver.point as Parcelable)
                setResult(RESULT_OK, intent)
            }else{
                setResult(RESULT_CANCELED, intent)
            }
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        map.onPause()  //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        var i = 0
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i])
            i++
        }
        if (permissionsToRequest.size > 0) {
            this.let {
                ActivityCompat.requestPermissions(
                    it,
                    permissionsToRequest.toTypedArray(),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
            }
        }
    }
}
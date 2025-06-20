package com.example.triptogether

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class TripMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_map)

        // Initialize the map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Called when the map is ready to be used
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Get the location data from the intent
        val lat = intent.getDoubleExtra("tripLat", 0.0)
        val lng = intent.getDoubleExtra("tripLng", 0.0)
        val countryName = intent.getStringExtra("country") ?: "Trip location"

        // Create a LatLng object and show a pin on the map
        val latLng = LatLng(lat, lng)
        googleMap.addMarker(MarkerOptions().position(latLng).title(countryName))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 5f))
    }
}

package `in`.mayank.awslocation


import `in`.mayank.awslocation.CompanionString.Companion.DEFAULT_INTERVAL_IN_MILLISECONDS
import `in`.mayank.awslocation.CompanionString.Companion.DEFAULT_MAX_WAIT_TIME
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {
    private lateinit var map: MapboxMap
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationEngine: LocationEngine
    private lateinit var callback: LocationChangeListeningCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, BuildConfig.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)
        map_view.onCreate(savedInstanceState)
        map_view.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        callback = LocationChangeListeningCallback()
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            enableLocationComponent(it)
        }
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(this, loadedMapStyle)
                .useDefaultLocationEngine(false)
                .build()
            map.locationComponent.apply {
                activateLocationComponent(locationComponentActivationOptions)
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                isLocationComponentEnabled = true                       // Enable to make component visible
                cameraMode = CameraMode.TRACKING                        // Set the component's camera mode
                renderMode = RenderMode.COMPASS                         // Set the component's render mode

            }
            initLocationEngine()
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        val request = LocationEngineRequest
            .Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
            .build()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationEngine.requestLocationUpdates(request, callback, mainLooper)
        locationEngine.getLastLocation(callback)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private inner class LocationChangeListeningCallback : LocationEngineCallback<LocationEngineResult> {

        override fun onSuccess(result: LocationEngineResult?) {
            result?.lastLocation ?: return //BECAREFULL HERE, IF NAME LOCATION UPDATE DONT USER -> val resLoc = result.lastLocation ?: return
            if (result.lastLocation != null){
                val lat = result.lastLocation?.latitude!!
                val lng = result.lastLocation?.longitude!!
                val latLng = LatLng(lat, lng)
                getAddress(latLng)


                if (result.lastLocation != null) {
                    map.locationComponent.forceLocationUpdate(result.lastLocation)
                    val position = CameraPosition.Builder()
                        .target(latLng)
                        .zoom(13.0) //disable this for not follow zoom
                        .tilt(10.0)
                        .build()
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(position))
                   // Toast.makeText(this@MainActivity, "Location update : $latLng", Toast.LENGTH_SHORT).show()
                }
            }

        }

        override fun onFailure(exception: Exception) {}
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, "Permission not granted!!", Toast.LENGTH_SHORT).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            map.getStyle {
                enableLocationComponent(it)
            }
        } else {
            Toast.makeText(this, "Permission not granted!! app will be EXIT", Toast.LENGTH_LONG).show()
            Handler().postDelayed({
                finish()
            }, 3000)
        }
    }

    override fun onStart() {
        super.onStart()
        map_view.onStart()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onStop() {
        super.onStop()
        map_view.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Prevent leaks
        locationEngine.removeLocationUpdates(callback)
        map_view.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_view.onSaveInstanceState(outState)
    }

    @SuppressLint("SetTextI18n")
    fun getAddress(latLngG: LatLng){
        val addresses: List<Address>
        val geocoder = Geocoder(this, Locale.getDefault())

        addresses = geocoder.getFromLocation(latLngG.latitude, latLngG.longitude, 1)
        val address: String = addresses[0].getAddressLine(0)

        tvLocation.text = "Address: $address, \nLat: ${latLngG.latitude} and Long: ${latLngG.longitude}"
    }

}

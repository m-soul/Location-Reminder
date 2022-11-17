package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.LocationManager

import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil

import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled

import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private val TAG = RemindersActivity::class.java.simpleName

    private val locationRequest =
        LocationRequest.Builder(10000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
    private val myRequestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        {
            if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                enableMyLocation()
                updateMapUiWithUserLocation()
            }
            if (it[Manifest.permission.ACCESS_FINE_LOCATION] == false) {
                _viewModel.showSnackBar.value =
                    "The app needs access to the location to be able to direct map camera to your location"
            }

        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this



        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
//        TODO: add the map setup implementation

//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
        binding.confirm.setOnClickListener { onLocationSelected() }


        return binding.root
    }


    private fun onLocationSelected() {
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

            true
        }
        R.id.hybrid_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        setMapStyle(mMap)
        enableMyLocation()
        if (isLocationEnabled()) {
            updateMapUiWithUserLocation()
        } else {
            if (isPermissionGranted()) {
                checkDeviceLocation()
            }
        }
        mMap.setOnMyLocationButtonClickListener { updateMapUiTrue() }

        mMap.setOnMapLongClickListener { latLng ->
            mMap.clear()
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Dropped Pin")
                    .snippet(snippet)
            )?.showInfoWindow()
            _viewModel.longitude.value = latLng.longitude
            _viewModel.latitude.value = latLng.latitude
            _viewModel.reminderSelectedLocationStr.value = "Dropped Pin"
            binding.confirm.visibility = View.VISIBLE
        }
        mMap.setOnPoiClickListener { poi ->
            mMap.clear()
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                poi.latLng.latitude,
                poi.latLng.longitude
            )
            val poiMarker = mMap.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .snippet(snippet)
            )
            poiMarker?.showInfoWindow()
            _viewModel.longitude.value = poi.latLng.longitude
            _viewModel.latitude.value = poi.latLng.latitude
            _viewModel.reminderSelectedLocationStr.value = poi.name
            binding.confirm.visibility = View.VISIBLE
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateMapUiWithUserLocation() {
        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        if (foreGroundLocationGranted()) {
            if (isLocationEnabled()) {
                val currentLocationRequest =
                    CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build()
                val currentLocation =
                    fusedLocationClient.getCurrentLocation(currentLocationRequest, null)
                currentLocation.addOnCompleteListener()
                {
                    mMap.isMyLocationEnabled = true
                    val lat = it.result.latitude
                    val lng = it.result.longitude
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
                }
            } else {
                checkDeviceLocation()
            }


        }
    }

    private fun updateMapUiTrue(): Boolean {
        updateMapUiWithUserLocation()
        return true
    }

    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            mMap.setMyLocationEnabled(true)
        } else {
            requestMyForegroundLocationPermission()

        }
    }

    private fun requestMyForegroundLocationPermission() {
        if (foreGroundLocationGranted()) {
            return
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            _viewModel.showSnackBar.value =
                "The app needs access to the location to be able to direct map camera to your location. you can do this from the device settings"
        }
        myRequestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun foreGroundLocationGranted(): Boolean {

        return (ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun isLocationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager =
                context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isLocationEnabled
        } else {
            val locationMode = Settings.Secure.getInt(
                context?.contentResolver, Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            );
            (locationMode != Settings.Secure.LOCATION_MODE_OFF)
        }
    }

    @SuppressLint("MissingPermission")
    fun checkDeviceLocation(): Boolean {
        if (isLocationEnabled()) {
            return true
        } else {
            var done = false
            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
            val settingsClient = LocationServices.getSettingsClient(requireActivity())
            val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        startIntentSenderForResult(
                            exception.resolution.intentSender,
                            2,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.d(
                            "Location Request",
                            "Error getting location settings resolution: " + sendEx.message
                        )
                    }
                } else {
                    _viewModel.showSnackBar.value = "Location services are not enabled"
                }
            }
            locationSettingsResponseTask.addOnCompleteListener {
                if (it.isSuccessful) {
                    done = true
                }
            }
            return done
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                updateMapUiWithUserLocation()
            } else {
                _viewModel.showSnackBar.value =
                    "Please enable device location to let the app direct the camera to your location"
            }
        }
    }
}

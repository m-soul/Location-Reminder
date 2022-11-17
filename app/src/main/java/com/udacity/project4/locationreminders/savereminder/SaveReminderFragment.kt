package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {


    //Get the view model this time as a single to be shared with the another fragment
    val ACTION_GEOFENCE_EVENT = "ACTION_GEOFENCE_EVENT"

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var broadCastIntent: Intent
    private var geofenceStarted = MutableLiveData<Boolean>(false)
    private var reminderDataItem: ReminderDataItem? = null
    private lateinit var geoFenceingClient: GeofencingClient
    private val runningQOrLater = Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
    private val locationRequest =
        LocationRequest.Builder(10000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

    @SuppressLint("NewApi")
    val myforeGroundRequestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        {
            if (it[Manifest.permission.ACCESS_FINE_LOCATION] == false) {
                _viewModel.showSnackBar.value =
                    "The app needs access to the location to start the Geofence"
            }
            if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                if (runningQOrLater && !backGroundLocationGranted()) {
                    requestBackGroundLocationPermission()
                } else {
                    checkDeviceLocationAndStartGeofence()
                }

            }
        }

    @SuppressLint("NewApi")
    val myBackGroundRequestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission())
        {
            if (it == false) {
                _viewModel.showSnackBar.value =
                    "the app needs access to the background location to start the geofence"
            } else {
                checkDeviceLocationAndStartGeofence()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        broadCastIntent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        broadCastIntent.action = ACTION_GEOFENCE_EVENT

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel



        return binding.root
    }

    @SuppressLint("MissingPermission", "NewApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        geoFenceingClient = LocationServices.getGeofencingClient(requireActivity())
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location

            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())


        }
        geofenceStarted.observe(viewLifecycleOwner, Observer {
            if (it) {
                _viewModel.saveReminder(reminderDataItem!!)
            }
        })
        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
            reminderDataItem = ReminderDataItem(
                title,
                description,
                location,
                latitude,
                longitude
            )
            if (_viewModel.validateEnteredData(
                    reminderDataItem!!
                )
            ) {
                if (!foreGroundLocationGranted()) {
                    requestForegroundLocationPermission()
                } else {
                    if (runningQOrLater && !backGroundLocationGranted()) {
                        requestBackGroundLocationPermission()
                    } else {
                        checkDeviceLocationAndStartGeofence()
                    }
                }
            }


        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                checkDeviceLocationAndStartGeofence()
            } else {
                _viewModel.showSnackBar.value =
                    "the app needs the location to be enabled to start the Geofence"
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun backGroundLocationGranted(): Boolean {
        return if (runningQOrLater) {
            (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        } else {
            true
        }
    }

    private fun requestForegroundLocationPermission() {
        if (foreGroundLocationGranted()) {
            return
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            _viewModel.showSnackBar.value =
                "The app needs access to the location to start the Geofence. you can do this from the device settings"
        }
        myforeGroundRequestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackGroundLocationPermission() {
        if (backGroundLocationGranted()) {
            return
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        ) {
            _viewModel.showSnackBar.value =
                "The app needs access the background location to start the Geofence"
        }
        if (runningQOrLater) {
            myBackGroundRequestPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
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

    @SuppressLint("MissingPermission")
    fun checkDeviceLocationAndStartGeofence() {
        if (isLocationEnabled()) {
            startGeofence(reminderDataItem!!)

        } else {
            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
            val settingsClient = LocationServices.getSettingsClient(requireActivity())
            val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        startIntentSenderForResult(
                            exception.resolution.intentSender,
                            1,
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
                    _viewModel.showSnackBar.value =
                        "the app needs the location to be enabled to start the Geofence"
                }
            }
            locationSettingsResponseTask.addOnCompleteListener {
                if (it.isSuccessful) {
                    startGeofence(reminderDataItem!!)
                }
            }
        }


    }

    @SuppressLint("MissingPermission")
    private fun startGeofence(reminderDataItem: ReminderDataItem) {
        val geofenceBuilder = Geofence.Builder().setRequestId(reminderDataItem.id)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setCircularRegion(reminderDataItem.latitude!!, reminderDataItem.longitude!!, 100f)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofenceBuilder)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            geoFenceingClient.addGeofences(
                geofencingRequest,
                PendingIntent.getBroadcast(
                    requireContext(),
                    reminderDataItem.hashCode(),
                    broadCastIntent,
                    PendingIntent.FLAG_MUTABLE
                )
            )
            geofenceStarted.value = true
        } else {
            geoFenceingClient.addGeofences(
                geofencingRequest,
                PendingIntent.getBroadcast(
                    requireContext(),
                    reminderDataItem.hashCode(),
                    broadCastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            geofenceStarted.value = true

        }


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
}






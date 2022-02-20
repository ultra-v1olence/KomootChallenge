package com.example.komoot.challenge

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.navigation.compose.rememberNavController
import com.example.komoot.challenge.service.LocationService
import com.example.komoot.challenge.ui.AppNavGraph
import com.example.komoot.challenge.ui.Screen
import com.example.komoot.challenge.ui.theme.AppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModel()

    private var currentConnection: LocationServiceConnection? = null

    private class LocationServiceConnection : ServiceConnection {
        var photoUrls: LiveData<List<String>>? = null

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LocationService.LocationBinder
            photoUrls = binder.photoUrls
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val locationServiceRunning = isLocationServiceRunning()
        if (locationServiceRunning) {
            connectToExistingService()
        }
        setContent {
            AppTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navHostController = navController,
                    walkInProgress = locationServiceRunning,
                    onNewWalkStarted = {
                        navController.navigate(Screen.WALK.name)
                        startLocationService()
                    },
                    onWalkEnded = {
                        stopLocationService()
                        navController.navigate(Screen.START_WALK.name)
                    },
                    onOpenPermissionSettingsClick = { openPermissionsSettings() },
                    getPhotoUrlsLiveData = { currentConnection?.photoUrls },
                    locationPermissionLiveData = mainViewModel.locationPermissionLiveData,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestLocationPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentConnection?.let { connection ->
            unbindService(connection)
            currentConnection = null
        }
    }

    private fun requestLocationPermission() {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> onLocationPermissionGranted()
            else -> registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    onLocationPermissionGranted()
                } else {
                    onLocationPermissionDenied()
                }
            }.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun onLocationPermissionDenied() {
        mainViewModel.onLocationPermissionDenied()
    }

    private fun onLocationPermissionGranted() {
        mainViewModel.onLocationPermissionGranted()
    }

    private fun connectToExistingService() {
        bindService(
            Intent(this, LocationService::class.java),
            LocationServiceConnection().also { currentConnection = it },
            Context.BIND_AUTO_CREATE
        )
    }

    private fun startLocationService() {
        Intent(this, LocationService::class.java).also { intent ->
            startService(intent)
            bindService(
                intent,
                LocationServiceConnection().also { currentConnection = it },
                Context.BIND_AUTO_CREATE
            )
        }
    }

    private fun stopLocationService() {
        currentConnection?.let { connection ->
            unbindService(connection)
            currentConnection = null
        }
        Intent(this, LocationService::class.java).also { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            startService(intent)
        }
    }

    @Suppress("DEPRECATION")
    private fun isLocationServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return activityManager.getRunningServices(Int.MAX_VALUE).any {
            LocationService::class.java.name == it.service.className
        }
    }

    private fun openPermissionsSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also {
            it.data = Uri.fromParts("package", packageName, null)
            startActivity(it)
        }
    }
}

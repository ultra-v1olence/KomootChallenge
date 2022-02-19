package com.example.komoot.challenge

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.example.komoot.challenge.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private var currentConnection: LocationServiceConnection? = null

    private class LocationServiceConnection : ServiceConnection {
        var photos: LiveData<List<String>>? = null

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LocationService.LocationBinder
            photos = binder.photoUrls
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkForFineLocationPermission(
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    onLocationPermissionGranted()
                } else {
                    onLocationPermissionDenied()
                }
            }
        )
        val locationServiceRunning = isLocationServiceRunning()
        if (locationServiceRunning) {
            bindService(
                Intent(this, LocationService::class.java),
                LocationServiceConnection().also { currentConnection = it },
                Context.BIND_AUTO_CREATE
            )
        }
        setContent {
            AppTheme {
                val navController = rememberNavController()
                AppNavGraph(navController, locationServiceRunning)
            }
        }
    }

    @Composable
    private fun StartNewWalk(
        onNewWalkStarted: () -> Unit,
    ) {
        val locationPermission = mainViewModel.locationPermissionStateFlow.collectAsState()
        val locationPermissionGranted = locationPermission.value
        Scaffold(
            floatingActionButton = {
                if (locationPermissionGranted) {
                    FloatingActionButton(
                        onClick = {
                            onNewWalkStarted()
                        },
                        content = {
                            Icon(
                                imageVector = Icons.Default.Hiking,
                                contentDescription = getString(R.string.start_new_walk_accessibility),
                            )
                        }
                    )
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (locationPermissionGranted) {
                    Text(
                        text = getString(R.string.start_new_walk),
                        modifier = Modifier.padding(horizontal = 48.dp)
                    )
                } else {
                    Text(
                        text = getString(R.string.please_allow_location),
                        modifier = Modifier.padding(horizontal = 48.dp)
                    )
                    // todo: add link to the settings
                }
            }
        }
    }

    @Composable
    private fun Walk(
        onWalkEnded: () -> Unit,
    ) {
        val photos = currentConnection?.photos?.observeAsState()
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        onWalkEnded()
                    },
                    content = {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = getString(R.string.stop_walk_accessibility),
                        )
                    }
                )
            }
        ) {
            val photosList = photos?.value ?: emptyList()
            if (photosList.isNotEmpty()) {
                LazyColumn {
                    photosList.forEach {
                        item {
                            Image(
                                painter = rememberImagePainter(it),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .height(250.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = getString(R.string.no_photos_placeholder),
                            modifier = Modifier.padding(horizontal = 48.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AppNavGraph(
        navHostController: NavHostController,
        locationServiceRunning: Boolean,
    ) {
        NavHost(
            navController = navHostController,
            startDestination = if (locationServiceRunning) {
                Screen.WALK.name
            } else {
                Screen.START_WALK.name
            },
        ) {
            composable(route = Screen.START_WALK.name) {
                StartNewWalk {
                    navHostController.navigate(Screen.WALK.name)
                    startLocationService()
                }
            }
            composable(route = Screen.WALK.name) {
                Walk {
                    stopLocationService()
                    navHostController.navigate(Screen.START_WALK.name)
                }
            }
        }
    }

    private fun checkForFineLocationPermission(activityResultLauncher: ActivityResultLauncher<String>) {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> onLocationPermissionGranted()
            else -> activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun onLocationPermissionDenied() {
        mainViewModel.onLocationPermissionDenied()
    }

    private fun onLocationPermissionGranted() {
        mainViewModel.onLocationPermissionGranted()
    }

    private fun startLocationService() {
        Intent(this, LocationService::class.java).also { intent ->
            startService(intent)
            bindService(
                intent,
                LocationServiceConnection().also { currentConnection = it },
                Context.BIND_AUTO_CREATE
            )
            // todo: leak?
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
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        manager.getRunningServices(Int.MAX_VALUE).forEach {
            if (LocationService::class.java.name == it.service.className) {
                return true
            }
        }
        return false
    }

    private enum class Screen {
        START_WALK,
        WALK,
    }
    // todo: move everything related to the Compose to a separate file
}

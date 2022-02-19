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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.komoot.challenge.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private var currentConnection: LocationServiceConnection? = null

    private class LocationServiceConnection(
        private val lifecycleOwner: LifecycleOwner
    ) : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d("12345", "onServiceConnected")
            val binder = service as LocationService.LocationBinder
            binder.locations.observe(lifecycleOwner) {
                Log.d(
                    "12345",
                    "location: ${it.latitude};${it.longitude}"
                )
            }
            binder.numbers.observe(lifecycleOwner) {
                Log.d(
                    "12345",
                    "number: $it"
                )
            }
            // todo: как передавать данные вьюшкам?
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d("12345", "onServiceDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityResultLauncher: ActivityResultLauncher<String> =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    onLocationPermissionGranted()
                } else {
                    onLocationPermissionDenied()
                }
            }
        checkForFineLocationPermission(activityResultLauncher)
        val locationServiceRunning = isLocationServiceRunning()
        Log.d("12345", "activity onCreate, locationServiceRunning = $locationServiceRunning")
        if (locationServiceRunning) {
            bindService(
                Intent(this, LocationService::class.java),
                LocationServiceConnection(this).also { currentConnection = it },
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = getString(R.string.stop_walk),
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
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
        Log.d("12345", "onLocationPermissionGranted")
        mainViewModel.onLocationPermissionGranted()
    }

    private fun startLocationService() {
        Intent(this, LocationService::class.java).also { intent ->
            startService(intent)
            bindService(
                intent,
                LocationServiceConnection(this).also { currentConnection = it },
                Context.BIND_AUTO_CREATE
            )
        }
    }

    private fun stopLocationService() {
        Log.d("12345", "stopLocationService, currentConnection = $currentConnection")
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

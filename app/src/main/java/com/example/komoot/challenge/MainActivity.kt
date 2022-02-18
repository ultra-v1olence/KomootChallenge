package com.example.komoot.challenge

import android.Manifest
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
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.komoot.challenge.ui.theme.AppTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private var currentConnection: LocationServiceConnection? = null
    // todo: то есть при перевороте девайса соединение ломается? (да; и сервис умирает) поможет ли rememberSaveable?
    // todo: а как подсосаться к существующему сервису при старте активности?

    private class LocationServiceConnection(
        private val lifecycleScope: LifecycleCoroutineScope
    ) : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d("12345", "onServiceConnected")
            val binder = service as LocationService.LocationBinder
            lifecycleScope.launch {
                binder.locations.collect {
                    Log.d(
                        "12345",
                        "location: ${it.latitude};${it.longitude}"
                    )
                }
            }

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
        setContent {
            AppTheme {
                val navController = rememberNavController()
                AppNavGraph(navController)
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
    ) {
        NavHost(navController = navHostController, startDestination = Screen.START_WALK.name) {
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
            PackageManager.PERMISSION_GRANTED -> {
                onLocationPermissionGranted()
            }
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
                LocationServiceConnection(lifecycleScope).also { currentConnection = it },
                Context.BIND_AUTO_CREATE
            )
            // todo: are both calls necessary?
        }
    }

    private fun stopLocationService() {
        currentConnection?.let { connection -> unbindService(connection) }
    }

    private enum class Screen {
        START_WALK,
        WALK,
    }
    // todo: this navigation is useless
}

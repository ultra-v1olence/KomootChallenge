package com.example.komoot.challenge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.komoot.challenge.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityResultLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                onLocationPermissionGranted()
            } else {
                onLocationPermissionDenied()
            }
        }
        setContent {
            AppTheme {
                StartNewWalk(activityResultLauncher)
            }
        }
    }
    
    @Composable
    private fun StartNewWalk(activityResultLauncher: ActivityResultLauncher<String>) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        Log.d("ABCDE","FAB clicked")
                        checkForFineLocationPermission(activityResultLauncher)
                    },
                    content = {
                        Icon(
                            imageVector = Icons.Default.Hiking,
                            contentDescription = getString(R.string.start_new_walk_accessibility),
                        )
                    }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = getString(R.string.start_new_walk),
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
            }
        }
    }

    private fun checkForFineLocationPermission(activityResultLauncher: ActivityResultLauncher<String>) {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> {
                onLocationPermissionGranted()
            }
            else -> {
                activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun onLocationPermissionDenied() {
        Log.d("ABCDE","permission denied")
    }

    private fun onLocationPermissionGranted() {
        Log.d("ABCDE","permission granted")
    }
}

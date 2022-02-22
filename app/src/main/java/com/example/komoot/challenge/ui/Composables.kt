package com.example.komoot.challenge.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import coil.compose.rememberImagePainter
import com.example.komoot.challenge.R
import kotlinx.coroutines.flow.Flow

@Composable
fun AppNavGraph(
    navHostController: NavHostController,
    walkInProgress: Boolean,
    onNewWalkStarted: () -> Unit,
    onWalkEnded: () -> Unit,
    onOpenPermissionSettingsClick: () -> Unit,
    getPhotoUrlsLiveData: () -> LiveData<List<String>>?,
    locationPermissionLiveData: LiveData<Boolean>,
) {
    NavHost(
        navController = navHostController,
        startDestination = if (walkInProgress) {
            Screen.WALK.name
        } else {
            Screen.START_WALK.name
        },
    ) {
        composable(route = Screen.START_WALK.name) {
            StartNewWalk(
                onNewWalkStarted,
                onOpenPermissionSettingsClick,
                locationPermissionLiveData
            )
        }
        composable(route = Screen.WALK.name) {
            CurrentWalk(
                onWalkEnded,
                getPhotoUrlsLiveData()
            )
        }
    }
}

@Composable
private fun StartNewWalk(
    onNewWalkStarted: () -> Unit,
    onOpenPermissionSettingsClick: () -> Unit,
    locationPermissionLiveData: LiveData<Boolean>,
) {
    val locationPermission = locationPermissionLiveData.observeAsState()
    val locationPermissionGranted = locationPermission.value ?: false
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
                            contentDescription = stringResource(R.string.start_new_walk_accessibility),
                        )
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (locationPermissionGranted) {
                Text(
                    text = stringResource(R.string.start_new_walk),
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.click_to_allow_location_permission),
                    modifier = Modifier
                        .padding(horizontal = 48.dp)
                        .clickable { onOpenPermissionSettingsClick() },
                )
            }
        }
    }
}

@Composable
private fun CurrentWalk(
    onWalkEnded: () -> Unit,
    photoUrlsFlow: LiveData<List<String>>?,
) {
    val photos = photoUrlsFlow?.observeAsState()?.value ?: emptyList()
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onWalkEnded()
                },
                content = {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = stringResource(R.string.stop_walk_accessibility),
                    )
                }
            )
        }
    ) {
        if (photos.isNotEmpty()) {
            LazyColumn {
                photos.forEach {
                    item {
                        Image(
                            painter = rememberImagePainter(it),
                            contentDescription = null,
                            contentScale = ContentScale.FillHeight,
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
                        text = stringResource(R.string.no_photos_placeholder),
                        modifier = Modifier.padding(horizontal = 48.dp)
                    )
                }
            }
        }
    }
}

enum class Screen {
    START_WALK,
    WALK,
}


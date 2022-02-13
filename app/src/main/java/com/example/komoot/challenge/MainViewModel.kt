package com.example.komoot.challenge

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel : ViewModel() {

    val locationPermissionStateFlow = MutableStateFlow(false)

    fun onLocationPermissionGranted() {
        locationPermissionStateFlow.value = true
    }

    fun onLocationPermissionDenied() {
        locationPermissionStateFlow.value = false
    }
}
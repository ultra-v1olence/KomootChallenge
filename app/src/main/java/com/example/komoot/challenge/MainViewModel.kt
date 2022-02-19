package com.example.komoot.challenge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _locationPermissionLiveData = MutableLiveData(false)
    val locationPermissionLiveData: LiveData<Boolean>
        get() = _locationPermissionLiveData

    fun onLocationPermissionGranted() {
        _locationPermissionLiveData.value = true
    }

    fun onLocationPermissionDenied() {
        _locationPermissionLiveData.value = false
    }
}
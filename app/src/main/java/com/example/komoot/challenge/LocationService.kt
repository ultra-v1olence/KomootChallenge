package com.example.komoot.challenge

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val locationRequest
        get() = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val task: Task<LocationSettingsResponse> =
            LocationServices
                .getSettingsClient(this)
                .checkLocationSettings(
                    LocationSettingsRequest
                        .Builder()
                        .addLocationRequest(locationRequest)
                        .build()
                )
        task.addOnSuccessListener { locationSettingsResponse ->
            Log.d(
                "12345",
                "isLocationUsable = ${locationSettingsResponse.locationSettingsStates?.isLocationUsable}"
            )
        }
        //todo: do I even need this task?
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d(
                    "12345",
                    "locations: ${locationResult.locations.joinToString { "${it.latitude};${it.longitude}" }}"
                )
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                Log.d(
                    "12345",
                    "isLocationAvailable: ${locationAvailability.isLocationAvailable}"
                )
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (t: SecurityException) {
            Log.e("12345", "no permission?", t)
        }
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(getString(R.string.app_name), "Location updates")
        } else {
            getString(R.string.app_name)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentTitle(getText(R.string.location_service_notification_title))
            .setContentText(getText(R.string.location_service_notification_subtitle))
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(notificationChannel)
        return channelId
    }
}
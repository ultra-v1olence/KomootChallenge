package com.example.komoot.challenge

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val locationRequest
        get() = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

    private val locationLiveData = MutableLiveData<Location>()
    private val numbersLiveData = MutableLiveData<Int>()

    var timer: Timer? = null

    override fun onBind(p0: Intent?) = LocationBinder()

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("12345", "LocationService onUnbind: $this")
        //этот метод почему-то сам вызывается при смерти активности.
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("12345", "LocationService created: $this")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationLiveData.value = locationResult.lastLocation
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (t: SecurityException) {
            Log.e("12345", "no permission?: $this", t)
        }
        Timer("increment every second").also { timer = it }.schedule(
            IncrementTimerTask(
                numbersLiveData
            ),
            0L,
            1000L,
        )
    }

    private class IncrementTimerTask(private val liveData: MutableLiveData<Int>) : TimerTask() {
        var number = 0

        override fun run() {
            liveData.postValue(number++)
        }
    }

    override fun onDestroy() {
        Log.d("12345", "LocationService destroyed: $this")
        timer?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("12345", "LocationService onStartCommand: $this")
        if (intent?.flags?.equals(Intent.FLAG_ACTIVITY_CLEAR_TASK) == true) {
            stopSelf()
            return START_NOT_STICKY
        }
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE) // todo: what does this flag mean?
            }

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                getString(R.string.app_name),
                getString(R.string.app_notification_channel_title),
            )
        } else {
            getString(R.string.app_name)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentTitle(getString(R.string.location_service_notification_title))
            .setContentText(getString(R.string.location_service_notification_subtitle))
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val notificationChannel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(notificationChannel)
        return channelId
    }

    inner class LocationBinder : Binder() {
        val locations: LiveData<Location>
            get() = locationLiveData

        val numbers: LiveData<Int>
            get() = numbersLiveData
    }
}
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val locationRequest
        get() = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(10) // todo: the real number will be about 60
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

    private val locationLiveData = MutableLiveData<Location>()
    private val photoUrlsLiveData = MutableLiveData<List<String>>()

    private val photosRepository = PhotosRepository()

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
                val lastLocation = locationResult.lastLocation
                locationLiveData.value = lastLocation
                GlobalScope.launch { // fixme: of course it should be something else
                    try {
                        val photo = photosRepository.retrievePhoto(lastLocation)
                        photo?.let {
                            val photoUrl = "https://live.staticflickr.com/${it.server}/${it.id}_${it.secret}.jpg"
                            Log.d("12345", "Photo retrieved: $photoUrl")
                            photoUrlsLiveData.postValue(
                                listOf(photoUrl).plus(photoUrlsLiveData.value ?: emptyList())
                            )
                        }
                    } catch (t: Throwable) {
                        Log.e("12345", "Photo retrieval error", t)
                    }
                }
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
    }

    override fun onDestroy() {
        Log.d("12345", "LocationService destroyed: $this")
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
                PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_IMMUTABLE
                    } else {
                        0
                    },
                )
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

        val photoUrls: LiveData<List<String>>
            get() = photoUrlsLiveData
    }
}
package com.example.komoot.challenge.service

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
import com.example.komoot.challenge.MainActivity
import com.example.komoot.challenge.R
import com.example.komoot.challenge.repository.FlickrApiKeys
import com.example.komoot.challenge.repository.PhotosRepository
import com.flickr4java.flickr.Flickr
import com.flickr4java.flickr.REST
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class LocationService : Service() {

    class LocationBinder(val photoUrls: LiveData<List<String>>) : Binder()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val locationRequest
        get() = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(10)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

    private val photoUrlsLiveData = MutableLiveData<List<String>>()

    private val photosRepository = PhotosRepository(
        // actual API keys are not committed to the repository
        Flickr(FlickrApiKeys.API_KEY, FlickrApiKeys.SECRET, REST())
    // todo: replace with koin
    )

    private val notificationChannelId
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                getString(R.string.app_name),
                getString(R.string.app_notification_channel_title),
            )
        } else {
            getString(R.string.app_name)
        }

    private val pendingIntent
        get() = Intent(this, MainActivity::class.java).let { notificationIntent ->
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

    override fun onBind(p0: Intent?) = LocationBinder(photoUrlsLiveData)

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                downloadPhoto(locationResult.lastLocation)
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (securityException: SecurityException) {
            Log.e("LocationService","Can't request location updates", securityException)
        }
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.flags?.equals(Intent.FLAG_ACTIVITY_CLEAR_TASK) == true) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(1, createForegroundNotification(notificationChannelId, pendingIntent))
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

    private fun createForegroundNotification(
        channelId: String,
        pendingIntent: PendingIntent,
    ) = NotificationCompat.Builder(this, channelId)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentTitle(getString(R.string.location_service_notification_title))
        .setContentText(getString(R.string.location_service_notification_subtitle))
        .setContentIntent(pendingIntent)
        .build()


    private fun downloadPhoto(location: Location) {
        coroutineScope.launch {
            try {
                val photoUrl = photosRepository.retrievePhotoUrl(location)
                photoUrl?.let {
                    val existingPhotos = photoUrlsLiveData.value ?: emptyList()
                    if (!existingPhotos.contains(it)) {
                        photoUrlsLiveData.postValue(listOf(it).plus(existingPhotos))
                    }
                }
            } catch (t: Throwable) {
                Log.e("LocationService","Can't download photo", t)
            }
        }
    }
}
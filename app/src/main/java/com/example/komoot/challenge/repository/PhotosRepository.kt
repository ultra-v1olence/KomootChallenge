package com.example.komoot.challenge.repository

import android.location.Location
import com.example.komoot.challenge.ext.distanceToLocation
import com.example.komoot.challenge.ext.flickrUrl
import com.flickr4java.flickr.Flickr
import com.flickr4java.flickr.photos.SearchParameters
import kotlinx.coroutines.flow.MutableSharedFlow

class PhotosRepository(private val flickr: Flickr) {

    val photoUrlsFlow = MutableSharedFlow<List<String>>()

    private var lastLocationWithPhoto: Location? = null
    private val photoUrls = mutableListOf<String>()

    suspend fun retrieveNewPhoto(location: Location) {
        val newLocationIsFarEnoughFromPrevious =
            lastLocationWithPhoto?.let { it.distanceTo(location) >= 100 } ?: true
        if (!newLocationIsFarEnoughFromPrevious) return

        val photosOfGivenLocation = flickr.photosInterface.search(
            SearchParameters().apply {
                this.latitude = location.latitude.toString()
                this.longitude = location.longitude.toString()
                this.hasGeo = true
                this.radius = 0.1
                this.extras = setOf("geo")
            },
            0,
            0,
        )

        photosOfGivenLocation
            .filter { it.distanceToLocation(location.latitude, location.longitude) != null }
            .sortedBy { it.distanceToLocation(location.latitude, location.longitude) }
            .map { it.flickrUrl }
            .firstOrNull { !photoUrls.contains(it) }
            ?.let {
                photoUrls.add(0, it)
                lastLocationWithPhoto = location
            }

        photoUrlsFlow.emit(ArrayList(photoUrls))
    }
}
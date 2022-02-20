package com.example.komoot.challenge.repository

import android.location.Location
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

        val photo = flickr.photosInterface.search(
            SearchParameters().apply {
                this.latitude = location.latitude.toString()
                this.longitude = location.longitude.toString()
            },
            1,
            0
        ).firstOrNull() ?: return

        lastLocationWithPhoto = location
        val photoUrl = photo.let {
            "https://live.staticflickr.com/${it.server}/${it.id}_${it.secret}.jpg"
        }
        if (!photoUrls.contains(photoUrl)) {
            photoUrls.add(0, photoUrl)
        }
        photoUrlsFlow.emit(ArrayList(photoUrls))
    }
}
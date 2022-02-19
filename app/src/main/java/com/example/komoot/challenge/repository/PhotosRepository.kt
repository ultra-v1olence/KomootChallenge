package com.example.komoot.challenge.repository

import android.location.Location
import com.flickr4java.flickr.Flickr
import com.flickr4java.flickr.REST
import com.flickr4java.flickr.photos.SearchParameters

class PhotosRepository {

    private var lastLocationWithPhoto: Location? = null

    fun retrievePhotoUrl(location: Location): String? {
        val newLocationIsFarEnoughFromPrevious = lastLocationWithPhoto?.let {
            it.distanceTo(location) >= 100
        } ?: true
        if (!newLocationIsFarEnoughFromPrevious) return null

        // actual API keys are not committed to the repository
        val flickr = Flickr(FlickrApiKeys.API_KEY, FlickrApiKeys.SECRET, REST())
        val photo = flickr.photosInterface.search(
            SearchParameters().apply {
                this.latitude = location.latitude.toString()
                this.longitude = location.longitude.toString()
            },
            1,
            0
        ).firstOrNull() ?: return null

        lastLocationWithPhoto = location
        return photo.let {
            "https://live.staticflickr.com/${it.server}/${it.id}_${it.secret}.jpg"
        }
    }
}
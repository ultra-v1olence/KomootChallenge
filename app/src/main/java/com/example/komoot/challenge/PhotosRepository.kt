package com.example.komoot.challenge

import com.flickr4java.flickr.Flickr
import com.flickr4java.flickr.REST
import com.flickr4java.flickr.photos.Photo
import com.flickr4java.flickr.photos.SearchParameters

class PhotosRepository {

    fun retrievePhoto(
        latitude: String,
        longitude: String,
    ): Photo? {
        // actual API keys are not committed to the repository
        val flickr = Flickr(FlickrApiKeys.API_KEY, FlickrApiKeys.SECRET, REST())
        return flickr.photosInterface.search(
            SearchParameters().apply {
                this.latitude = latitude
                this.longitude = longitude
            },
            1,
            0
        ).firstOrNull()
    }
}
package com.example.komoot.challenge.ext

import com.flickr4java.flickr.photos.Photo
import kotlin.math.pow

fun Photo.distanceToLocation(latitude: Double, longitude: Double): Double? =
    geoData?.let {
        (it.latitude - latitude).pow(2) + (it.longitude - longitude).pow(2)
    }?.pow(0.5)

val Photo.flickrUrl
    get() = "https://live.staticflickr.com/${server}/${id}_${secret}.jpg"
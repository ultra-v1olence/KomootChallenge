package com.example.komoot.challenge.repository

import android.location.Location
import com.flickr4java.flickr.Flickr
import com.flickr4java.flickr.photos.Photo
import com.flickr4java.flickr.photos.PhotoList
import com.flickr4java.flickr.photos.SearchParameters
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PhotosRepositoryTest {

    private val flickr = mockk<Flickr>()
    private lateinit var repository: PhotosRepository

    @Before
    fun setUp() {
        repository = PhotosRepository(flickr)
    }

    @Test
    fun `flickr interface returns a photo, url matches photo parameters`() {
        val location = mockk<Location>() {
            every { latitude } returns 10.0
            every { longitude } returns 10.0
        }
        val photo = Photo().apply {
            server = "12345"
            id = "976543"
            secret = "abcdefg"
        }
        every {
            flickr.photosInterface.search(any(), any(), any())
        } returns PhotoList<Photo>().apply {
            add(photo)
        }

        val url = repository.retrievePhotoUrl(location)

        assertEquals("https://live.staticflickr.com/12345/976543_abcdefg.jpg", url)
    }
}
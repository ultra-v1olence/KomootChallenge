package com.example.komoot.challenge.repository

import android.location.Location
import app.cash.turbine.test
import com.example.komoot.challenge.ext.flickrUrl
import com.flickr4java.flickr.Flickr
import com.flickr4java.flickr.photos.GeoData
import com.flickr4java.flickr.photos.Photo
import com.flickr4java.flickr.photos.PhotoList
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class PhotosRepositoryTest {

    private val flickr = mockk<Flickr>()
    private lateinit var repository: PhotosRepository

    @Before
    fun setUp() {
        repository = PhotosRepository(flickr)
        every {
            flickr.photosInterface.search(any(), any(), any())
        } returns PhotoList<Photo>().apply {
            add(photo1)
        }
    }

    @Test
    fun `flickr interface triggers flow, url matches photo parameters`() {
        val location = mockk<Location> {
            every { latitude } returns 10.0
            every { longitude } returns 10.0
        }

        runTest {
            repository.photoUrlsFlow.test {
                repository.retrieveNewPhoto(location)
                val urlList = awaitItem()
                assertEquals(1, urlList.size)
                assertEquals("https://live.staticflickr.com/12345/976543_abcdefg.jpg", urlList[0])
                cancelAndConsumeRemainingEvents()
            }
        }
    }

    @Test
    fun `method called twice from the same location, second call does not trigger flow`() {
        val location = mockk<Location> {
            every { latitude } returns 10.0
            every { longitude } returns 10.0
            every { distanceTo(any()) } returns 0f
        }

        runTest {
            repository.photoUrlsFlow.test {
                repository.retrieveNewPhoto(location)
                val urlList = awaitItem()
                repository.retrieveNewPhoto(location)
                expectNoEvents()
                assertEquals(1, urlList.size)
            }
        }
    }

    @Test
    fun `method called twice from distinct locations which are far from each other, the same photo is returned, flow is triggered twice with the same photo`() {
        val location = mockk<Location> {
            every { latitude } returns 10.0
            every { longitude } returns 10.0
            every { distanceTo(any()) } returns 1000f
        }

        runTest {
            repository.photoUrlsFlow.test {
                repository.retrieveNewPhoto(location)
                val urlList1 = awaitItem()
                repository.retrieveNewPhoto(location)
                val urlList2 = awaitItem()
                assertEquals(1, urlList1.size)
                assertEquals(1, urlList2.size)
                assertEquals(urlList1[0], urlList2[0])
            }
        }
    }

    @Test
    fun `method called twice from distinct locations which are far from each other, different photos are returned, flow is triggered twice with first 1 and then 2 photos`() {
        val photo1 = Photo().apply {
            server = "12345"
            id = "976543"
            secret = "abcdefg"
            geoData = GeoData().apply {
                latitude = 50f
                longitude = 8f
            }
        }
        val photo2 = Photo().apply {
            server = "67890"
            id = "324654"
            secret = "zxcvbn"
            geoData = GeoData().apply {
                latitude = 50.1f
                longitude = 8.1f
            }
        }
        every {
            flickr.photosInterface.search(any(), any(), any())
        } returns PhotoList<Photo>().apply {
            add(photo1)
        } andThen PhotoList<Photo>().apply {
            add(photo2)
        }
        val location = mockk<Location> {
            every { latitude } returns 10.0
            every { longitude } returns 10.0
            every { distanceTo(any()) } returns 1000f
        }

        runTest {
            repository.photoUrlsFlow.test {
                repository.retrieveNewPhoto(location)
                val urlList1 = awaitItem()
                repository.retrieveNewPhoto(location)
                val urlList2 = awaitItem()
                assertEquals(1, urlList1.size)
                assertEquals(2, urlList2.size)
                assertNotEquals(urlList1[0], urlList2[0])
                assertEquals(urlList1[0], urlList2[1])
            }
        }
    }

    @Test
    fun `flickr interface returns two images, the closest one is in the list, the second one is added to the list on the 2nd call`() {

        every {
            flickr.photosInterface.search(any(), any(), any())
        } returns PhotoList<Photo>().apply {
            add(photo1)
            add(photo2)
        }
        val location = mockk<Location> {
            every { latitude } returns 50.1
            every { longitude } returns 8.1
            every { distanceTo(any()) } returns 1000f
        }

        runTest {
            repository.photoUrlsFlow.test {
                repository.retrieveNewPhoto(location)
                val urlList1 = awaitItem()
                repository.retrieveNewPhoto(location)
                val urlList2 = awaitItem()
                assertEquals(1, urlList1.size)
                assertEquals(2, urlList2.size)
                assertEquals(photo2.flickrUrl, urlList1[0])
                assertEquals(photo1.flickrUrl, urlList2[0])
                assertEquals(photo2.flickrUrl, urlList2[1])
            }
        }
    }

    private companion object {
        val photo1 = Photo().apply {
            server = "12345"
            id = "976543"
            secret = "abcdefg"
            geoData = GeoData().apply {
                latitude = 50f
                longitude = 8f
            }
        }
        val photo2 = Photo().apply {
            server = "67890"
            id = "324654"
            secret = "zxcvbn"
            geoData = GeoData().apply {
                latitude = 50.1f
                longitude = 8.1f
            }
        }
    }
}
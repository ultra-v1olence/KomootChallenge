package com.example.komoot.challenge.di

import com.example.komoot.challenge.MainViewModel
import com.example.komoot.challenge.repository.FlickrApiKeys
import com.example.komoot.challenge.repository.PhotosRepository
import com.flickr4java.flickr.Flickr
import com.flickr4java.flickr.REST
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val mainModule = module {

    viewModel {
        MainViewModel()
    }

    factory {
        Flickr(FlickrApiKeys.API_KEY, FlickrApiKeys.SECRET, REST())
    }

    factory {
        PhotosRepository(get())
    }
}
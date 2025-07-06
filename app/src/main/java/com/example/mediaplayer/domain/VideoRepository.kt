package com.example.mediaplayer.domain

interface VideoRepository {
    fun getManifestUrl(): String
    fun getLicenseUrl(): String
}

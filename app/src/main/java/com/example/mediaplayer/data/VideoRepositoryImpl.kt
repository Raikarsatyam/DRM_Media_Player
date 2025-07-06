package com.example.mediaplayer.data

import com.example.mediaplayer.domain.VideoRepository
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(): VideoRepository {
    override fun getManifestUrl() = "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd"

    override fun getLicenseUrl() = "https://cwip-shaka-proxy.appspot.com/no_auth"
}
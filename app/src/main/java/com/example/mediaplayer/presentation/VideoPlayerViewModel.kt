package com.example.mediaplayer.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSourceInputStream
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.manifest.DashManifest
import androidx.media3.exoplayer.dash.manifest.DashManifestParser
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.mediaplayer.domain.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class VideoPlayerViewModel @Inject
constructor(
    @ApplicationContext private val context: Context,
    val player: ExoPlayer,
    videoRepository: VideoRepository,
    private val dashMediaSourceFactory: DashMediaSource.Factory
) : ViewModel() {

    private val videoUri = videoRepository.getManifestUrl()
    private val licenseUrl = videoRepository.getLicenseUrl()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _tracksInfo = MutableStateFlow<List<TrackInfo>>(emptyList())
    val tracksInfo = _tracksInfo.asStateFlow()

    // Video position and duration tracking
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private var isUserSeeking = false

    private var positionTrackingJob: Job? = null
    private var hasUserManuallySelectedResolution = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.update { isPlaying }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isLoading.update {
                playbackState == Player.STATE_BUFFERING
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            // Only update resolution if user has manually selected one
            if (hasUserManuallySelectedResolution) {
                updateCurrentlyPlayingResolution(tracks)
            }
        }
    }

    init {
        player.prepare()
        player.playWhenReady = false // Don't auto-play initially

        // Add player listener to track state changes
        player.addListener(playerListener)
        
        // Start position tracking
        startPositionTracking()
        
        viewModelScope.launch(Dispatchers.IO) {
            fetchResolutions(videoUri)
        }
    }

    private fun startPositionTracking() {
        positionTrackingJob?.cancel()
        positionTrackingJob = viewModelScope.launch {
            try {
                while (true) {
                    if (!isUserSeeking) {
                        _currentPosition.update { player.currentPosition }
                    }
                    _duration.update { player.duration.takeIf { it != C.TIME_UNSET } ?: 0L }
                    kotlinx.coroutines.delay(1000) // Update every second
                }
            } catch (e: Exception) {
                Log.d("VideoPlayer", "Position tracking stopped: ${e.message}")
            }
        }
    }

    private fun updateCurrentlyPlayingResolution(tracks: Tracks) {
        Log.d("VideoResolution", "=== Track change detected ===")
        
        val currentVideoTrack = tracks.groups
            .firstOrNull { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
            ?.let { group ->
                Log.d("VideoResolution", "Found video track group with ${group.length} tracks")
                // Get the selected track format
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        val format = group.getTrackFormat(i)
                        Log.d("VideoResolution", "Selected track $i: ${format.width}x${format.height}")
                        return@let format
                    }
                }
                null
            }

        currentVideoTrack?.let { format ->
            val currentWidth = format.width
            val currentHeight = format.height
            
            Log.d("VideoResolution", "Current video format: ${currentWidth}x${currentHeight}")
            
            if (currentWidth != Format.NO_VALUE && currentHeight != Format.NO_VALUE) {
                Log.d("VideoResolution", "Currently playing: ${currentWidth}x${currentHeight}")
                
                // Check if this matches any of our available resolutions
                val availableResolutions = _tracksInfo.value.filter { !it.isAuto }
                Log.d("VideoResolution", "Available resolutions: ${availableResolutions.map { "${it.width}x${it.height}" }}")
                
                val matchingTrack = availableResolutions.find { 
                    it.width == currentWidth && it.height == currentHeight 
                }
                
                if (matchingTrack != null) {
                    Log.d("VideoResolution", "Found matching track: ${matchingTrack.width}x${matchingTrack.height}")
                    _tracksInfo.update { tracksInfo ->
                        tracksInfo.map { track ->
                            track.copy(
                                isSelected = track.width == currentWidth && track.height == currentHeight
                            )
                        }
                    }
                } else {
                    Log.d("VideoResolution", "No matching track found, keeping Auto selected")
                    // If no exact match, keep Auto selected (adaptive streaming)
                    _tracksInfo.update { tracksInfo ->
                        tracksInfo.map { track ->
                            track.copy(isSelected = track.isAuto)
                        }
                    }
                }
            }
        } ?: run {
            Log.d("VideoResolution", "No video track found or not selected")
        }
    }

    fun playVideo() {
        viewModelScope.launch {
            try {
                // Only set media item if it's not already set
                if (player.mediaItemCount == 0) {
                val createMediaItem = createMediaItem(videoUri.toUri(), licenseUrl)
                player.setMediaItem(createMediaItem)
                }
                player.play()
                
                // When video starts, ensure Auto mode is enabled if no manual selection
                if (!hasUserManuallySelectedResolution) {
                    setAdaptiveStreaming()
                    // Ensure Auto remains selected in UI
                    _tracksInfo.update { tracksInfo ->
                        tracksInfo.map { track ->
                            track.copy(isSelected = track.isAuto)
                        }
                    }
                }
            } catch (exception: Exception) {
                Log.d("z900", "playVideo: ${exception.message}")
            }
        }
    }

    private fun pauseVideo() {
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            pauseVideo()
        } else {
            playVideo()
        }
    }

    @OptIn(UnstableApi::class)
    fun createMediaItem(
        videoUri: Uri,
        licenseUrl: String = ""
    ): MediaItem {
        val mediaItem = MediaItem.fromUri(videoUri)
        return when (val type = Util.inferContentType(videoUri)) {
            C.CONTENT_TYPE_DASH -> {
                val builder = mediaItem.buildUpon().apply {
                    setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                            .setLicenseUri(licenseUrl)
                            .build()
                    )
                }
                dashMediaSourceFactory.createMediaSource(builder.build()).mediaItem
            }

            C.CONTENT_TYPE_OTHER -> {
                mediaItem
            }

            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    private suspend fun fetchResolutions(mpdUrl: String) {
        withContext(Dispatchers.IO) {
            val dsFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(context, "MyApp"))
                .setDefaultRequestProperties(
                    mapOf("Accept" to "application/dash+xml")
                )
            val dataSource = dsFactory.createDataSource()
            val spec = DataSpec(mpdUrl.toUri())

            val dsi = DataSourceInputStream(dataSource, spec)
            try {
                // Open the connection
                dsi.open()
                val manifest: DashManifest = DashManifestParser().parse(spec.uri, dsi)

                val sizes = (0 until manifest.periodCount).asSequence()
                    .flatMap { manifest.getPeriod(it).adaptationSets }
                    .filter { it.type == C.TRACK_TYPE_VIDEO }
                    .flatMap { it.representations }
                    .mapNotNull { rep ->
                        val (w, h) = rep.format.width to rep.format.height
                        if (w != Format.NO_VALUE && h != Format.NO_VALUE) w to h else null
                    }
                    .toSet()

                // Update tracks info
                sizes.forEach { (w, h) ->
                    _tracksInfo.update { it.plus(TrackInfo(w, h)) }
                }
                
                // Add "Auto" option at the beginning (select it initially)
                _tracksInfo.update { tracksInfo ->
                    listOf(TrackInfo.createAutoTrack(isSelected = true)) + tracksInfo
                }
            } catch (e: IOException) {
                Log.e("MPD_DEBUG", "Failed to load MPD", e)
            } finally {
                // Always close!
                try {
                    dsi.close()
                } catch (_: IOException) { /* ignore */
                }
            }
        }
    }

    fun setPlayerResolution(trackInfo: TrackInfo) {
        // Mark that user has manually selected a resolution
        hasUserManuallySelectedResolution = true
        
        if (trackInfo.isAuto) {
            setAdaptiveStreaming()
            // Update UI to show auto is selected
            _tracksInfo.update { tracksInfo ->
                tracksInfo.map { track ->
                    track.copy(isSelected = track.isAuto)
                }
            }
            // Reset the manual selection flag since user chose Auto
            hasUserManuallySelectedResolution = false
        } else {
        val trackSelector = (player.trackSelector as? DefaultTrackSelector)
            ?: throw IllegalStateException("ExoPlayer must be using DefaultTrackSelector")

            // Update UI immediately to show user selection
        _tracksInfo.update { tracksInfo ->
            tracksInfo.map { track ->
                    track.copy(isSelected = track == trackInfo)
            }
        }

            // Build new parameters that force the max video size to exactly (w,h)
        val paramsBuilder = trackSelector.buildUponParameters()
            // Prevent automatically switching to higher than this
            .setMaxVideoSize(trackInfo.width, trackInfo.height)
            // Also prevent switching to lower than this (so it's pin-point)
            .setMinVideoSize(trackInfo.width, trackInfo.height)
            // Make sure bitrate/size constraints are honored strictly
            .setForceHighestSupportedBitrate(false)

            // Apply them
            trackSelector.parameters = paramsBuilder.build()
        }
    }

    private fun setAdaptiveStreaming() {
        val trackSelector = (player.trackSelector as? DefaultTrackSelector)
            ?: throw IllegalStateException("ExoPlayer must be using DefaultTrackSelector")

        // Reset to default parameters for adaptive streaming
        val paramsBuilder = trackSelector.buildUponParameters()
            .clearVideoSizeConstraints()
            .setForceHighestSupportedBitrate(false)

        trackSelector.parameters = paramsBuilder.build()
        Log.d("VideoResolution", "Switched to adaptive streaming")
    }

    // Seeking functionality
    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun startSeeking() {
        isUserSeeking = true
    }

    fun stopSeeking() {
        isUserSeeking = false
    }

    fun updateSeekPosition(positionMs: Long) {
        if (isUserSeeking) {
            _currentPosition.update { positionMs }
        }
    }

    override fun onCleared() {
        super.onCleared()
        positionTrackingJob?.cancel()
        player.removeListener(playerListener)
    }
}
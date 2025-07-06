package com.example.mediaplayer

import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.mediaplayer.data.VideoRepositoryImpl
import com.example.mediaplayer.domain.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object PlayerModule {

    @OptIn(UnstableApi::class)
    @Provides
    @ViewModelScoped
    fun provideVideoPlayer(
        app: Application,
        defaultTrackSelector: DefaultTrackSelector
    ): ExoPlayer {
        return ExoPlayer.Builder(app)
            .setTrackSelector(defaultTrackSelector)
            .build()
    }

    @OptIn(UnstableApi::class)
    @Provides
    @ViewModelScoped
    fun providesTrackSelector(@ApplicationContext context: Context) = DefaultTrackSelector(context)


    @OptIn(UnstableApi::class)
    @Provides
    @ViewModelScoped
    fun providesDashMediaSourceFactory(
        defaultDashChunkSourceFactory: DefaultDashChunkSource.Factory,
        defaultDataSourceFactory: DefaultDataSource.Factory
    ) = DashMediaSource.Factory(defaultDashChunkSourceFactory, defaultDataSourceFactory)

    @OptIn(UnstableApi::class)
    @Provides
    @ViewModelScoped
    fun providesDefaultDashChunkSourceFactory(
        defaultDataSourceFactory: DefaultDataSource.Factory
    ) = DefaultDashChunkSource.Factory(defaultDataSourceFactory)

    @Provides
    @ViewModelScoped
    fun providesDefaultDataSourceFactory(@ApplicationContext context: Context) =
        DefaultDataSource.Factory(context)
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class VideoModule {
    @Binds
    abstract fun bindsVideoRepository(videoRepositoryImpl: VideoRepositoryImpl): VideoRepository
}
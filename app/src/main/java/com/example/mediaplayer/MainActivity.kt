package com.example.mediaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.mediaplayer.presentation.VideoPlayerViewModel
import com.example.mediaplayer.ui.theme.MediaPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<VideoPlayerViewModel>()

    // Helper function to format time
    private fun formatTime(milliseconds: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        val hours = TimeUnit.MINUTES.toHours(minutes)
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
            else -> String.format("%d:%02d", minutes, seconds % 60)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaPlayerTheme {
                var lifecycle by remember {
                    mutableStateOf(Lifecycle.Event.ON_CREATE)
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        lifecycle = event
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val tracksInfo = viewModel.tracksInfo.collectAsStateWithLifecycle().value
                val isPlaying = viewModel.isPlaying.collectAsStateWithLifecycle().value
                val isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value
                val currentPosition = viewModel.currentPosition.collectAsStateWithLifecycle().value
                val duration = viewModel.duration.collectAsStateWithLifecycle().value

                // Beautiful gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A1A2E),
                                    Color(0xFF16213E),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent
                    ) { contentPadding ->
                    Column(
                        modifier = Modifier
                                .fillMaxSize()
                            .padding(contentPadding)
                                .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(40.dp))
                            
                            // Video Player Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 20.dp,
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Black.copy(alpha = 0.8f)
                                )
                    ) {
                        AndroidView(
                            factory = { context ->
                                PlayerView(context).also {
                                    it.player = viewModel.player
                                            it.useController = false
                                }
                            },
                            update = {
                                when (lifecycle) {
                                    Lifecycle.Event.ON_PAUSE -> {
                                        it.onPause()
                                        it.player?.pause()
                                    }
                                    Lifecycle.Event.ON_RESUME -> {
                                        it.onResume()
                                    }
                                    else -> Unit
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16 / 9f)
                                        .clip(RoundedCornerShape(20.dp))
                        )
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            // Control Panel Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Progress Bar Section
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatTime(currentPosition),
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(60.dp)
                                        )
                                        
                                        Slider(
                                            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                            onValueChange = { progress ->
                                                val newPosition = (progress * duration).toLong()
                                                viewModel.startSeeking()
                                                viewModel.updateSeekPosition(newPosition)
                                            },
                                            onValueChangeFinished = {
                                                val newPosition = currentPosition.coerceIn(0L, duration)
                                                viewModel.seekTo(newPosition)
                                                viewModel.stopSeeking()
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 16.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF6C5CE7),
                                                activeTrackColor = Color(0xFF6C5CE7),
                                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                                        )
                                        
                                        Text(
                                            text = formatTime(duration),
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(60.dp)
                                        )
                        }
                                    
                        Spacer(modifier = Modifier.height(24.dp))
                                    
                                    // Play/Pause Button
                                    FloatingActionButton(
                                        onClick = { viewModel.togglePlayPause() },
                                        modifier = Modifier
                                            .size(72.dp)
                                            .shadow(
                                                elevation = 8.dp,
                                                shape = CircleShape
                                            ),
                                        containerColor = Color(0xFF6C5CE7),
                                        contentColor = Color.White
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 3.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isPlaying) ImageVector.vectorResource(
                                                    R.drawable.pause_button_svgrepo_com
                                                ) else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            // Resolution Selection Section
                        Text(
                                text = "Video Quality",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                        LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(tracksInfo) { track ->
                                Button(
                                    onClick = { viewModel.setPlayerResolution(track) },
                                        modifier = Modifier
                                            .shadow(
                                                elevation = if (track.isSelected) 8.dp else 4.dp,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (track.isSelected) 
                                                Color(0xFF6C5CE7) 
                                            else 
                                                Color.White.copy(alpha = 0.2f),
                                            contentColor = Color.White
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(
                                            defaultElevation = if (track.isSelected) 6.dp else 2.dp
                                    )
                                ) {
                                        Text(
                                            text = track.getDisplayText(),
                                            fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                }
                            }
                            }
                            
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
}
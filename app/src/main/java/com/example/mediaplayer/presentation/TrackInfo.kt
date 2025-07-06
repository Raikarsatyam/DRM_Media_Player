package com.example.mediaplayer.presentation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf

@Stable
data class TrackInfo(
    val width: Int,
    val height: Int,
    val isSelected: Boolean = false,
    val isAuto: Boolean = false
) {
    val isSelectedState = mutableStateOf(isSelected)
    
    companion object {
        fun createAutoTrack(isSelected: Boolean = false) = TrackInfo(
            width = -1,
            height = -1,
            isSelected = isSelected,
            isAuto = true
        )
    }
    
    fun getDisplayText(): String {
        return if (isAuto) "Auto" else "${height}p"
    }
}

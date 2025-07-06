# 🎥 Media Player

A modern Android video player built with **Jetpack Compose** and **ExoPlayer** that supports adaptive streaming and manual quality selection.

## ✨ Features

- **🎯 Adaptive Streaming** - Automatically adjusts video quality based on network conditions
- **⚙️ Manual Quality Control** - Switch between different resolutions (Auto, 720p, 1080p, etc.)
- **🎮 Video Controls** - Play/pause with beautiful Material Design 3 UI
- **📱 Scrubbing Support** - Seek through video with interactive slider
- **🔒 DRM Support** - Widevine DRM for protected content
- **🎨 Modern UI** - Glassmorphism design with gradient backgrounds and shadows

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Video Player**: ExoPlayer (Media3)
- **Architecture**: MVVM with StateFlow
- **Dependency Injection**: Hilt
- **Streaming Protocol**: DASH (Dynamic Adaptive Streaming)

## 📋 Requirements

- **Android SDK**: Min API 24 (Android 7.0)
- **Target SDK**: API 35
- **Kotlin**: 1.9+
- **Compose**: Latest stable version

## 🚀 Setup & Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd MediaPlayer
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use Android Studio's Run button

## 📱 Usage

1. **Launch the app**
2. **Press Play** - Video starts in Auto quality mode
3. **Quality Selection** - Tap resolution buttons to change video quality
4. **Scrubbing** - Drag the slider to seek through the video
5. **Controls** - Use the floating action button to play/pause

## 🎯 Key Components

### VideoPlayerViewModel
- Manages video playback state
- Handles resolution switching
- Controls seeking functionality
- Tracks video position and duration

### MainActivity
- Jetpack Compose UI
- Material Design 3 components
- Responsive video player interface

### TrackInfo
- Data model for video quality tracks
- Supports both manual and auto selection

## 🔧 Configuration

The player uses a default DASH stream URL. To use your own content:

1. Update `VideoRepositoryImpl.kt`:
   ```kotlin
   override fun getManifestUrl() = "YOUR_DASH_MANIFEST_URL"
   override fun getLicenseUrl() = "YOUR_DRM_LICENSE_URL"
   ```

## 📄 License

This project is for educational purposes.

---

Built with ❤️ using modern Android development practices 
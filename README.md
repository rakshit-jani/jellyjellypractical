# JellyJellyPracticalDemo

An Android app built with **Jetpack Compose**, **MVI architecture**, and **Hilt**, featuring:

- Dual camera preview & recording using **Camera2 API**
- Video playback from scraped feeds using **ExoPlayer**
- In-app gallery to view recorded videos
- Permission handling with full Compose-native UI
- Local internal storage for video files

---

## 📱 Features

### 🔹 1. Feed Tab
- Scrapes video content using a `WebView`-based `WebScraper`.
- Supports:
  - Video autoplay
  - Mute/Unmute
  - Previous/Next navigation
- Uses **ExoPlayer** for playback and playlist stacking.

### 🔹 2. Camera Tab
- Displays **split-screen live preview** from both front and back cameras using `TextureView`.
- Records front and back cameras **simultaneously** with mic audio.
- Auto-stops recording after 15 seconds or via manual Stop.
- Supports Pause/Resume (based on Android version).
- After recording, user is navigated to the **Gallery tab** automatically.
- Videos are stored under:


### 🔹 3. Gallery Tab
- Displays all recorded video pairs (front/back) as a grid.
- Clicking a video opens **full-screen dual playback dialog**.
- Uses **ExoPlayer** for synchronized playback.

---

## 📦 Architecture

### ✅ MVI Pattern
- Intent-driven state management per screen
- Unidirectional data flow
- Clean ViewModel separation

### 🧩 Modules/Packages

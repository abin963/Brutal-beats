<div align="center">

# 🎵 NYX Music

### A Futuristic Android Music Player

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
</p>

<p align="center">
  <img src="https://img.shields.io/github/stars/abin963/Brutal-beats?style=for-the-badge&logo=github&label=STARS" />
  <img src="https://img.shields.io/github/forks/abin963/Brutal-beats?style=for-the-badge&logo=github&label=FORKS" />
  <img src="https://img.shields.io/github/license/abin963/Brutal-beats?style=for-the-badge&label=LICENSE" />
</p>

---

**Discover · Listen · Experience**

</div>

---

## 🎧 About NYX

**NYX Music** is a modern Android music player designed around a futuristic and immersive listening experience.

Built with **Kotlin** and **Jetpack Compose**, NYX combines music playback, discovery, modern animations, immersive artwork and background playback into a single music experience.

The goal is simple:

> **Make listening to music feel as good as looking at it.**

---

## ✨ Features

### 🎵 Music Player

- ▶️ Play / Pause
- ⏭️ Next / Previous
- 🔀 Shuffle
- 🔁 Repeat
- ⏩ Seek through songs
- 🎚️ Playback progress
- 🖼️ Album artwork
- 🎧 Full-screen player

### 🔊 Background Playback

NYX is designed to continue playing music while the application is running in the background.

Supports:

- Background playback
- Media notifications
- Lock-screen controls
- Android media controls
- Foreground media service

### 🔎 Music Discovery

- Search for music
- Discover songs
- View album artwork
- Explore artists
- Music recommendations

### 🎨 Modern UI

NYX focuses heavily on visual design.

- Glassmorphism
- Liquid-glass inspired interface
- Smooth animations
- Dark mode
- Light mode
- Modern typography
- Immersive album artwork
- Responsive layouts

---

## 🖼️ Screenshots

<div align="center">

### Home

<img src="screenshots/home.png" width="300"/>

### Player

<img src="screenshots/player.png" width="300"/>

### Search

<img src="screenshots/search.png" width="300"/>

</div>

> Replace the images above with your actual screenshots.

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| 🟣 Kotlin | Programming language |
| 🎨 Jetpack Compose | UI framework |
| 🎨 Material 3 | UI components |
| 💾 Room | Local database |
| 🌐 Retrofit | API communication |
| 🔌 OkHttp | Networking |
| 🖼️ Coil | Image loading |
| ⚡ Coroutines | Asynchronous operations |
| 🤖 Firebase AI | AI functionality |
| 📱 AndroidX Media | Media playback |
| 🔧 KSP | Code generation |
| 📦 Gradle Kotlin DSL | Build system |

---

## 🏗️ Architecture

```text
NYX Music
│
├── 🎨 UI
│   ├── Home
│   ├── Search
│   ├── Player
│   ├── Library
│   └── Settings
│
├── 🎵 Playback
│   ├── Music Player
│   ├── Media Session
│   ├── Playback Service
│   └── Notifications
│
├── 🌐 Network
│   ├── API
│   ├── Retrofit
│   └── OkHttp
│
├── 💾 Database
│   ├── Room
│   ├── Songs
│   ├── Playlists
│   └── History
│
└── 🤖 AI
    └── Firebase AI

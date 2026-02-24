# 🏴‍☠️ ThePiece – One Piece TCG Companion App

ThePiece is a full-featured Android application built for **One Piece Trading Card Game (TCG)** players.  
It provides card browsing, deck building, OCR scanning, authentication, audio customization, and backend integration.

---

## 📱 Features

### 🔐 Authentication System
- User registration & login
- Secure token-based session authentication
- 30-day session validity
- Persistent login using DataStore
- Backend session validation

---

### 📚 Card Catalog
- Browse complete One Piece TCG database
- Filter by:
  - Set (OP01–OP14, EB, ST, etc.)
  - Card color
  - Card type
- Search by:
  - English name
  - Japanese name
  - Card code
- Search query expansion (English ↔ Japanese support)
- Recommended search suggestions
- Lazy loading for performance

---

### 🃏 Deck Builder
- Create new decks
- Edit existing decks
- Leader selection logic
- Add/remove cards dynamically
- Real-time deck validation
- Banlist rule enforcement
- Grid/List view toggle
- Local storage using Room database
- Reactive UI updates

---

### 📷 Card Scanning (OCR)
- Camera-based card recognition
- Extracts card name and card code
- Japanese name mapping support
- Redirects directly to catalog results

---

### 🎵 Audio System
- Background music playback
- Selectable BGM in Settings
- Persisted user music preference
- Lifecycle-aware playback management

---

### ☁️ Backend Integration
- PHP REST API
- MySQL database
- Token authentication
- Session table management
- Price fetching endpoint
- Secure API communication via Retrofit

---

## 🏗 System Architecture

```
Presentation Layer (Jetpack Compose UI)
                ↓
          ViewModel Layer
                ↓
         Repository Layer
        ↓               ↓
Local Data (Room)   Remote Data (Retrofit API)
                            ↓
                        PHP Backend
                            ↓
                         MySQL DB
```

## 🛠 Technologies Used

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM |
| Local Database | Room |
| Networking | Retrofit + Moshi |
| Local Storage | DataStore |
| Backend | PHP + MySQL |
| OCR | CameraX + TFLite |
| Dependency Provider | AppGraph |

---

## 📂 Project Structure

```
com.koi.thepiece
│
├── data
│   ├── api
│   ├── db
│   ├── repo
│   └── local
│
├── ui
│   ├── screens
│   ├── components
│   └── navigation
│
├── viewmodel
│
└── AppGraph.kt
```

## 🔑 Authentication Flow

1. User logs in
2. Backend generates session token
3. Token stored in:
   - MySQL session table
   - Android DataStore
4. Token attached to all API requests
5. Token expires after 30 days

---

## 🚀 Getting Started

### Requirements
- Android Studio Hedgehog or newer
- Minimum SDK 24+
- Kotlin 1.9+
- Internet connection for API services

---

### Installation

1. Clone repository
git clone https://github.com/kamisamatenshi/AndriodA1.git


2. Open in Android Studio
3. Configure API base URL in `NetworkModule.kt`
4. Build and Run

---

## 📡 Backend Requirements

- PHP 8+
- MySQL
- Apache / Nginx
- Required endpoints:
  - register.php
  - login.php
  - get_cards.php
  - get_price.php
  - validate_session.php

---

## 📊 Performance Considerations

- Lazy loading improves catalog scalability
- Room enables offline deck editing
- Token system reduces repeated authentication
- Compose state-driven UI ensures reactive updates

---

## 🔮 Future Improvements

- 1-to-1 Direct Messaging
- Group & Global Chat
- Deck sharing via ShareID
- Real-time price tracking
- Push notifications
- Admin card management panel

---

## 👥 Contributors

- System & Server Architecture  
- Deck Builder System  
- Scan System  
- Audio & UI  
- UX Design  

---

## 📜 License

This project is for educational purposes.  
All One Piece TCG assets belong to Bandai.

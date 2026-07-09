# BusLK - Real-Time Transit & Smart Ticketing App 🚌🇱🇰

**BusLK** is a comprehensive, real-time Android application designed to modernize and streamline the public transportation experience. Built with strict Object-Oriented Analysis and Design (OOAD) principles, BusLK eliminates the unpredictability of traditional bus schedules and the inefficiencies of manual ticketing through live tracking and intelligent QR code interactions.

## ✨ Key Features

* **Real-Time Fleet Tracking:** Subscribe to live bus locations with sub-500ms latency, rendered dynamically on a zero-cost geospatial map.
* **Smart QR Ticketing:** Frictionless onboarding and dynamic `Trip` instantiation via standard QR payload scanning for automated fare collection.
* **Role-Based Access Control (RBAC):** Context-aware interfaces dynamically rendered for `Passenger`, `Driver`, and `Admin` user classes.
* **Gamified Condition Reporting:** Passengers can submit crowdsourced, verifiable bus condition reports to accrue system loyalty points.
* **Offline Tolerance:** Core spatial mappings, including static `Routes` and `Stops`, are persistently cached locally to maintain operational continuity during network drops.
* **Modern UI/UX:** Features a sleek, modern interface heavily utilizing a **glassmorphism** aesthetic (blurred backgrounds and transparency) for an immersive map viewing experience.

## 🛠️ Tech Stack & Architecture

- **UI Framework:** Jetpack Compose (Modern, declarative UI).
- **Architecture:** **MVVM (Model-View-ViewModel)** with **Repository Pattern**.
  - **State Management:** Kotlin Coroutines & `StateFlow` for reactive UI updates.
  - **Dependency Injection:** Custom ViewModel Factories for decoupled repository management.
- **Backend / Authentication:** 
  - **Firebase Auth:** Email/Password & Google Sign-In via **Android Credential Manager API**.
  - **Firebase Firestore:** Real-time user profile synchronization.
- **Geospatial Mapping:** OpenStreetMap (OSM) / `osmdroid`.

## 📈 Project Status & Accomplishments

### Phase 1: Foundation (Complete)
- [x] Initial Project Scaffolding & Gradle 9.0 configuration.
- [x] Firebase integration and build verification.

### Phase 2: Authentication & Identity (Complete)
- [x] Dual-method Auth: Custom Email/Password and Modern Google Sign-In.
- [x] **Smart Identity Sync:** Registration logic that initializes `UserDoc` with default stats while preserving data for returning users.
- [x] **Data Integrity:** Fully aligned `UserDoc` DTO with the `db.md` schema (Points, Levels, Preferences).
- [x] **Defensive UX:** Strict input validation and robust error mapping for Firebase exceptions.

### Phase 3: Core Features (Complete)
- [x] **Trilingual Localization:** Multi-language interface support (`English`, `Sinhala`, `Tamil`) for all application screens and search queries.
- [x] **Geospatial Map Integration:** Interactive map using OpenStreetMap / `osmdroid` displaying live active bus locations.
- [x] **Smart QR Check-In & Ticketing:** CameraX/ML Kit QR code payload checking, automatic fare collection, and active trip tracking.
- [x] **Default Route Preference Filtering:** Set a default route in settings to automatically filter map markers and nearby listings to quickly access preferred routes.

## 📐 OOP & OOD Principles in BusLK

This application is a case study in solid software engineering:
- **Abstraction:** Using `IAuthRepository` interfaces to decouple UI from the data source.
- **Encapsulation:** Hiding complex Credential Manager and Firebase logic within repositories.
- **Polymorphism:** Utilizing a role-based user system (`Passenger`, `Driver`) with distinct behaviors.
- **Defensive Programming:** Pre-request validation and robust error mapping in the ViewModel layer.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/shashika-mora/buslk-android/issues).

---
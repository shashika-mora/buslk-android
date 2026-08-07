# BusLK - Real-Time Transit & Smart Ticketing Android App 🚌🇱🇰

**BusLK** is a native Kotlin Android application designed to digitize public transportation experiences across Sri Lanka. Built with strict Object-Oriented Analysis and Design (OOAD) and MVVM architecture, BusLK eliminates unpredictable transit timetables and manual ticketing through real-time GPS fleet tracking, CameraX QR check-ins, and multi-language support.

---

## ✨ Key Features & Screen Modules (13 Composables)

* **Live Geospatial Tracking (`HomeScreen`):** Real-time vehicle positioning streamed via Firebase Realtime Database and rendered on a zero-cost OpenStreetMap (`osmdroid`) canvas with custom marker overlays.
* **Smart QR Check-In & Ticketing (`ScanQRScreen`):** Native CameraX view with offline Google ML Kit Barcode Scanning engine parsing `buslk:checkin:{busId}` payloads for automated fare collection.
* **Active Trip Telemetry & Crowd Reporting (`TripScreen`):** Live passenger journey metrics, crowd density condition reports, and leader-elected GPS coordinate broadcasting.
* **Route Catalog & Fleet Intelligence (`SearchScreen`):** Search static routes and bus fleet specifications, view active/offline statuses, and inspect route stop itineraries.
* **Social Friends & Ride Presences (`FriendsScreen`):** Passenger peer graph displaying online presences, active bus ride locations, and quick chat shortcuts.
* **Instant 1-on-1 Chat (`ChatScreen`):** Real-time passenger messaging panel.
* **Lost & Found Hub (`LostAndFoundScreen`):** Community claims ledger, route filter pills, and report submission bottom sheets.
* **Commuter Gamification & History (`ProfileScreen`):** XP points progress tracker, unlocked milestone achievement cards, and complete historical trip records.
* **Community Feedback Engine (`FeedbackScreen`):** Post-trip multi-criteria rating breakdown (overall, cleanliness, comfort, driver behavior) and localized review tag selectors mapped dynamically via fallback rules.
* **Dynamic Settings & Locale Switcher (`SettingsScreen`):** DataStore preferences manager, theme toggles, and runtime language selector.
* **Onboarding & Auth (`OpeningScreen`, `LanguageSelectionScreen`, `LoginScreen`):** Onboarding splash, language setup, Email/Password login, and Google Sign-In via Android Credential Manager API.

---

## 🛠️ Tech Stack & Architecture

- **UI Framework:** 100% Jetpack Compose (Declarative UI, Material 3, `NavigationSuiteScaffold`).
- **Architecture:** **MVVM (Model-View-ViewModel)** with **Repository Pattern** and **Unidirectional Data Flow (UDF)**.
- **Languages & Target:** Kotlin 1.9, Java 11 target, `compileSdk 36`, `targetSdk 36`, `minSdk 24`.
- **State Management & Persistence:** Kotlin Coroutines, `StateFlow`, Jetpack DataStore Preferences.
- **Backend & Cloud Services:**
  - **Firebase Auth:** Email/Password & Google Sign-In via **Credential Manager API**.
  - **Firebase Firestore:** User profiles, trip ledgers, feedback, lost & found, support threads.
  - **Firebase Realtime Database (RTDB):** Low-latency vehicle GPS coordinate streaming (`bus_locations`).
  - **Firebase App Check:** Cryptographic request validation via **Play Integrity API**.
- **Geospatial & Vision:** OpenStreetMap (`osmdroid-android`), CameraX, Google ML Kit Barcode Scanning, Accompanist Permissions.
- **Localization (i18n):** Trilingual support (**English**, **Sinhala**, **Tamil**) using `AppCompatDelegate.setApplicationLocales` for instant runtime hot-swapping without logout.
- **Theming:** Full Material 3 Dark & Light theme synchronization across all 13 screens.

---

## 📐 Security & Repository Hygiene

* **Zero-Secret Policy:** Sensitive files (`google-services.json`, `secrets.xml`, `.env*`, `*.jks`) are strictly excluded from version control via `.gitignore`.
* **Local Configuration:** Place local `google-services.json` inside `app/` and `secrets.xml` inside `app/src/main/res/values/`. Sample templates are scrubbed of active credentials.
* **R8 Obfuscation:** Release builds enable bytecode minification (`isMinifyEnabled = true`, `isShrinkResources = true`).

---

## 🧪 Testing & Execution Commands

```bash
# Run unit tests locally
./gradlew testDebugUnitTest

# Run code linting
./gradlew lint
```
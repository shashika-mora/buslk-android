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

* **Platform:** Android (Java / Android SDK)
* **Backend / State Synchronization:** Firebase Realtime Database
* **Geospatial Mapping:** OpenStreetMap (OSM) / OSMDroid
* **Architecture:** Observer Pattern (for live location streams), strict Data Encapsulation, and Polymorphic routing.

## 🚀 Getting Started

### Prerequisites

* Android Studio (Koala or newer recommended)
* Minimum SDK: API 24 (Android 7.0)
* A valid `google-services.json` file from your Firebase console.

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/buslk-android.git

```


2. Open the project in Android Studio.
3. Place your Firebase `google-services.json` file into the `app/` directory.
4. Sync Gradle and build the project.
5. Run on an emulator or physical device.

## 📐 System Design Overview

This application heavily leverages core OOP concepts:

* **Composition:** `Route` objects are structurally composed of multiple `Stop` objects.
* **Inheritance & Polymorphism:** An abstract `User` base class is extended by specialized `Passenger`, `Driver`, and `Admin` subclasses, each inheriting core traits while implementing distinct behaviors (e.g., Drivers broadcasting GPS coordinates).

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://www.google.com/search?q=https://github.com/yourusername/buslk-android/issues).

---
# PillsCheck 💊

[![Android SDK](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
[![Java 17](https://img.shields.io/badge/Java-17-ED8B00.svg?style=flat&logo=openjdk)](https://www.oracle.com/java/)
[![Room DB](https://img.shields.io/badge/Storage-Room%20SQLite-4285F4.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![Google Tasks](https://img.shields.io/badge/Sync-Google%20Tasks%20API-FBBC05.svg?style=flat&logo=google)](https://developers.google.com/workspace/tasks)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**PillsCheck** is a native Android application built in **Java** designed to track daily medication intake, visualize physical blister packs in real time, sync bi-directionally with Google Tasks (and Wear OS smartwatches), and manage reserve prescription stock to alert you before running out of pills.

---

## ✨ Features

- **Interactive Canvas Blister Packs (`BlisterCanvasView`)**:
  - Realistic visual rendering matching physical blister packs:
    - Silver metallic foil (staggered 8-7-8-7 rows for 30-tablet blisters).
    - Amber/orange translucent foil with horizontal crease lines.
    - Silver foil with elongated 3D vertical capsules with dividing seams.
  - Punctured/empty pocket aesthetics for consumed doses and 3D glossy domes for remaining pills.
  - Direct touch interaction: tap any cavity on the blister to toggle it manually with haptic feedback.
  - Smart geometric highlight contour on the exact next pill to be consumed.

- **Completed Intake Feedback**:
  - Visual emerald green border around cards once daily dose is taken.
  - Automatic detection when pills are manually popped or taken via FAB.

- **Yesterday's Post-Intake Baseline Reference**:
  - Prominently displays *"Yesterday ended with: N pills"* on every medication card so you can glance at your physical blister in hand and immediately deduce if today's pill was taken.
  - One-tap editable baseline dialog to adjust reference counts on the fly.
  - Automatic midnight rollover advancing yesterday's count upon completing today's dose.

- **Bi-Directional Google Tasks Sync (Wear OS Compatible)**:
  - Optional Google account connection via Google Play Services OAuth.
  - Two flexible mapping modes:
    - **Single Global Task**: Completing your chosen task (e.g., from your smartwatch) consumes today's doses across all medications, and taking pills in-app marks the task completed.
    - **Per-Medication Mapping**: Associates each medication with its own distinct Google Task.
  - Fully optional: app remains 100% functional and offline if sync is disabled.

- **Dedicated Stock & Reserves Management (`StockManagementActivity`)**:
  - Standalone inventory management screen accessible from the top bar or medication cards.
  - Steppers to add or remove full boxes (`+1 Box`, `-1 Box`) and individual blisters (`+1 Blister`, `-1 Blister`).
  - Direct numeric editing of reserve blisters and loose pills.
  - Quick action to open a reserve blister into the active pack.

- **Smart Pharmacy & Restock Alerts**:
  - Calculates remaining days of treatment across active blisters + reserve boxes.
  - Highlights pharmacy restock alerts when supply falls below your configured margin (default: 7 days).

- **Recurrent Intake Alarm Engine**:
  - Configurable daily alarms (e.g. 14:00) with hourly repeat intervals if unconfirmed.
  - Evening cutoff / silence schedule (e.g. 22:00).
  - Interactive notification action buttons: **Take Now**, **Snooze 1h**, **Silence Today**.

- **Multi-Language Support (i18n)**:
  - English by default (`values/strings.xml`).
  - Complete Spanish localization (`values-es/strings.xml`).
  - In-app language switcher in Settings with immediate recreation.

- **Privacy-First Architecture (`local_seed.json`)**:
  - Personal medication names, prescription schedules, and stock counts are isolated in `.gitignore`d files.
  - Clean generic templates provided in public assets (`default_medications.json`).

---

## 🔒 Privacy & Local Seed Architecture

To keep medical/prescription information private when publishing to GitHub:
- The public repository includes clean generic template data in `app/src/main/assets/default_medications.json`.
- For personal local builds, define `local_seed.json` in the project root. Gradle automatically copies it to assets during build, and it is strictly `.gitignore`d.

---

## 🚀 Building & Running

### Prerequisites
- JDK 17+ or JDK 21
- Android SDK 34 (Build Tools 34.0.0)

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Build Signed Release APK
```bash
./gradlew assembleRelease
```
The output APK is generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

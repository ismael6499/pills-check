# PillsCheck 💊

**PillsCheck** is a native Android application built in **Java** designed to track daily medication intake, visualize physical blister packs in real time, and calculate prescription stock reserves to alert you before running out of pills.

---

## ✨ Features

- **Interactive Canvas Blister Packs**:
  - Realistic visual rendering matching physical blister packs (Silver foil, Amber translucent, 3D blue/yellow tablets, oblong capsules).
  - Punctured/empty pocket aesthetics for consumed doses and 3D glossy domes for remaining pills.
  - Direct touch interaction: tap any cavity on the blister to toggle it manually with haptic feedback.
- **Yesterday's Post-Intake Baseline Reference**:
  - Displays *"Yesterday ended with: N pills"* on every medication card so you can look at your physical blister right now and immediately deduce if today's pill was taken.
- **One-Tap Daily Intake**:
  - Prominent dashboard button to take all scheduled pills for the day simultaneously, with instant undo capability.
- **Smart Pharmacy & Low-Stock Alerts**:
  - Calculates remaining days of treatment across active blisters + reserve boxes.
  - Highlights warnings when supply falls below your configured threshold (default: 7 days).
- **Reserve Box Management**:
  - When an active blister reaches 0, tap *"Open Reserve Blister"* to transition a blister from reserve to active.
  - Quick restock action: *"+1 Box"* or custom pill amounts.
- **Configurable Reminder Notifications**:
  - High-priority interactive notifications via `AlarmManager`.
  - Notification action buttons: **Take Now**, **Snooze 1h**, **Silence Today**.
  - Repetition every hour if pending, automatically stopping at the evening cutoff time (e.g. 22:00).
- **Multi-Language (i18n)**:
  - English by default (`values/strings.xml`).
  - Spanish option (`values-es/strings.xml`).
  - In-app language switcher in Settings.
- **100% Offline & Private (Room SQLite)**:
  - No cloud dependency, accounts, or telemetry.

---

## 🔒 Personal Data vs Public Repository (`local_seed.json`)

To keep medical/prescription information private when publishing to GitHub:
- The public repo includes generic template data in `app/src/main/assets/default_medications.json`.
- For your personal local builds, define `local_seed.json` in the project root. Gradle automatically copies it to assets during build, and it is strictly `.gitignore`d.

---

## 🚀 Building the Project

### Prerequisites
- JDK 17+ or JDK 21 (Android Studio JBR supported)
- Android SDK 34+

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Build Signed Release APK
```bash
./gradlew assembleRelease
```
The output APK is located at:
`app/build/outputs/apk/release/app-release.apk`

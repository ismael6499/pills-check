# AGENTS.md — PillsCheck (Android App)

## Project Overview
- **Package:** `com.nitsutech.pillscheck`
- **Application:** PillsCheck (Daily Blister & Medication Stock Tracker)
- **Language & Platform:** Java, Native Android SDK (Min SDK 26, Target/Compile SDK 34)
- **User:** Agustín Acosta (`acostaagustin6499@gmail.com`). Always address him directly as **Agustín** in friendly Spanish (*vos*). Never address as Breto; never use formal *usted*.

---

## Build, Deployment & APK Storage Guidelines
1. **Target Directory for APKs:**
   - **Exclusively** deploy compiled APKs to:
     `C:\Users\agust\Documents\Mobile Drop\PillsCheck-v<version>.apk`
   - **NEVER** copy or deploy APKs to `Downloads`.
2. **Single Versioned APK Rule (No Duplicates):**
   - **ONLY** copy the single versioned APK file (e.g., `PillsCheck-v1.0.0.apk`).
   - **NEVER** generate, copy, or maintain duplicate unversioned APKs (e.g., do NOT create `PillsCheck.apk`).
3. **ADB Installation:**
   - Use ADB at:
     `C:\Users\agust\AppData\Local\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v3.3.3\adb.exe`
   - Install command:
     `adb install -r "C:\Users\agust\Documents\Mobile Drop\PillsCheck-v<version>.apk"`

---

## Privacy & Local Seed Architecture
1. **Personal Prescription Isolation:**
   - Agustín's personal medication counts and reserve stocks are isolated in `local_seed.json` in the project root.
   - `local_seed.json` and `app/src/main/assets/local_seed.json` are strictly **`.gitignore`d** to protect personal medical data when publishing to GitHub.
2. **Public Repository Template:**
   - The public repository uses `app/src/main/assets/default_medications.json` containing clean, generic medication templates.
   - If `local_seed.json` exists locally, Gradle copies it into assets before compilation, and Room seeds the database from it on first run.

---

## Visual Blister Architecture (`BlisterCanvasView`)
1. **Realistic Canvas Rendering:**
   - **Folcres:** Silver metallic foil (3x10 grid), round 3D blue tablets with glossy highlights, empty slots rendered as indented foil with pocket shadows and broken foil reflection.
   - **Minoxidil:** Translucent amber/orange foil (2x5 grid) with a horizontal center ridge crease, round yellow tablets.
   - **Vitamin D:** Silver foil (2x5 grid), elongated oblong 3D capsules with center dividing seam line.
2. **Interactive Taps:**
   - Direct touch on any cavity toggles its state with haptic feedback, updating Room DB and recalculating supply in real time.

---

## Yesterday's Post-Intake Baseline
- Every medication card prominently displays:
  `Yesterday ended with: N pills (Post-intake)`
- This allows Agustín to glance at his real blister pack in hand at any moment and instantly know whether today's dose was already taken.

---

## Notification & Reminder Schedule
- **Initial Alarm:** 14:00 (configurable in Settings).
- **Repetition:** Every 1 hour if intake remains unconfirmed.
- **Evening Cutoff:** 22:00 (configurable silence cutoff).
- **Interactive Notification Buttons:**
  - `Take Now` (marks all taken in DB and cancels reminder).
  - `Snooze 1h` (schedules follow-up alarm).
  - `Silence Today` (cancels further alarms until tomorrow).

---

## Internationalization (i18n) & UI Strings
1. **English by Default:**
   - All UI labels, buttons, dialogs, and toasts default to **English** (`values/strings.xml`).
2. **Spanish Localization:**
   - Complete Spanish translation provided in `values-es/strings.xml`.
3. **In-App Language Selector:**
   - Settings screen includes an explicit English / Español switcher with immediate UI recreation.
4. **Zero Hardcoded Strings:**
   - Never hardcode user-facing strings in layout files or Java classes.

---

## Git Workflow & Backdated Commits
1. **Local Identity:**
   ```powershell
   git config user.name "Agustin"
   git config user.email "acostaagustin6499@gmail.com"
   ```
2. **Backdated Commits Format:**
   ```powershell
   $env:GIT_AUTHOR_DATE="2026-10-31T17:05:12-03:00"; $env:GIT_COMMITTER_DATE="2026-10-31T17:05:12-03:00"; git commit -m "feat: commit message"
   Remove-Item Env:\GIT_AUTHOR_DATE
   Remove-Item Env:\GIT_COMMITTER_DATE
   ```

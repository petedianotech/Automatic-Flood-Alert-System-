# Workbee - Automated Flood Alert System

An Android application developed to monitor river water level indicator sensors via optical LED detection and issue real-time flood warning notifications.

---

## 🏫 Project & Developer Attribution

* **Organization**: ADDA STEM Club
* **School**: Dzenje Community Day Secondary School, Mulanje, Malawi
* **Full Stack Developer**: Peter Damiano ([peterdamiano.vercel.app](https://peterdamiano.vercel.app))

---

## 🌊 Overview & How It Works

This application provides a practical early-warning flood monitoring solution for communities near flood-prone river basins.

### 1. Hardware Sensor Integration
A riverbank sensor node monitors water levels and displays the current danger level using three distinct LED light indicators:
* 🟢 **Green LED**: Normal water levels.
* 🔵 **Blue LED**: Warning level (river level rising).
* 🔴 **Red LED**: Critical danger level (imminent flood threat).

### 2. Optical LED Detection (Computer Vision)
Instead of requiring expensive network modules on every riverbank sensor, the app uses an Android device camera pointed at the sensor node:
* **CameraX Live Stream**: Displays a live viewfinder with a Region-of-Interest (ROI) targeting reticle.
* **OpenCV Color Analysis**: Analyzes captured frames using HSV (Hue, Saturation, Value) color thresholding to identify whether the active LED is Green, Blue, or Red.

### 3. Automated Alert Notifications
When an optical scan detects a change in status:
* **Blue State (Warning)**: Dispatches a warning notification to community app users, advising them to prepare emergency supplies and move valuables to high ground.
* **Red State (Critical Danger)**: Dispatches an urgent heads-up emergency notification with sound and vibration, activating the audio siren alert and displaying immediate evacuation instructions.
* **Green State (Normal)**: Logs routine operational data without disturbing users.

---

## 📱 Key App Features

* **Live Camera Viewfinder**: Real-time camera feed with HUD overlays and target reticle.
* **Automated Scan Scheduler**: Configurable auto-scan timer (5–120 seconds) for periodic camera detection.
* **Admin Dashboard**:
  * Live optical analysis results with confidence metrics.
  * Manual and automated camera triggers.
  * Test calibration mode for system verification.
  * Historical detection logs stored locally and synced to the cloud.
* **Villager Portal**:
  * Clear status banner (Normal, Warning, Danger).
  * Color-coded safety and evacuation guidelines.
  * Directory of nearby high-ground evacuation shelters.
  * Direct-dial offline emergency contact list.

---

## 🛠️ Technology Stack

* **Language**: Kotlin
* **UI Framework**: Jetpack Compose with Material Design 3
* **Camera Pipeline**: AndroidX CameraX (`PreviewView`)
* **Computer Vision Engine**: OpenCV / HSV Color Thresholding Matrix
* **Data Persistence**: Room Database (Local) & Firebase Firestore (Cloud)
* **Messaging & Notifications**: Android `NotificationManager` & Firebase Cloud Messaging (FCM)

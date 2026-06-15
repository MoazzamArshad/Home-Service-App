# HomeServe - Home Services Marketplace Platform

HomeServe is a premium, full-stack platform that connects customers with local service providers (plumbers, electricians, cleaners, etc.) for on-demand home services. The project consists of a modern Android Mobile Application and a fully responsive Admin Web Console, backed by Firebase services.

---

## 🛠️ Technology Stack

### 1. Mobile Application (Android)
* **UI Framework**: Jetpack Compose (Kotlin) with modern Material Design 3 guidelines.
* **Architecture**: MVVM (Model-View-ViewModel) pattern leveraging `StateFlow` and Coroutines for state management.
* **Libraries**: Navigation Compose, Coil (async image loading), Google Play Services (Location & Fused Location Provider).
* **Local Cache**: Local Cache DB helper for offline availability.

### 2. Backend & Cloud Infrastructure (Firebase)
* **Cloud Database**: Firebase Firestore (Real-time NoSQL database).
* **Object Storage**: Firebase Storage (Secure hosting for profile photos, CNIC/ID verification cards, and chat media).
* **Push Notifications**: Firebase Cloud Messaging (FCM) to deliver alerts directly to devices when the app is in the background or closed.
* **Database Triggers**: Firebase Cloud Functions (Node.js v2 v2-triggers) listening to document changes to auto-send FCM payloads.

### 3. Administrative Console (Web)
* **Front-end**: Semantic HTML5 & Javascript (ES6+) with the Firebase client-side SDK.
* **Design System**: Vanilla CSS featuring dark-mode HSL variables, glassmorphism, and responsive grid layouts.

---

## ✨ Features Overview

### 👤 Customer App
* **Unified OTP Auth**: Seamless Phone Number authentication (currently mocked for rapid local testing).
* **Onboarding & GPS Location**: Automatic live coordinate fetching via Fused Location Provider with reverse geocoding to retrieve readable addresses.
* **Browse & Book Services**: Interactive catalog of categories and sub-services. Customers can book up to **3 services** concurrently.
* **Booking Ledger**: Scrollable and real-time ledger of all past and active ongoing bookings.
* **Real-time Chat & Images**: In-app messaging directly with providers with image attachments.
* **Ratings & Reviews**: Fully functional rating system to write feedback for completed bookings.

### 👨‍🔧 Service Provider App
* **Provider Registration**: Upload Profile Picture and ID/CNIC verification front card. Set service action radius.
* **Job Queue Management**: View all incoming job requests in a scrollable, real-time list.
* **Concurrent Job Limits**: Providers can accept and execute up to **2 active jobs** at a time.
* **Negotiable Pricing**: Providers can change/renegotiate prices directly in chat, adjusting invoices dynamically.
* **Status Controls**: Real-time status actions to manage jobs from acceptance, starting work, to completion.

### 👑 Admin Web Dashboard
* **Secure Access**: Fully client-side authentication with pre-configured developer logins (`Abdullah`, `Moazam`, `Farhan` using password `pucit`).
* **Analytics & Reports**: Visual graphs detailing total bookings, total money transferred, cancellation statistics, and active system counts.
* **Onboarding Verification**: Approve or decline service providers after reviewing their uploaded ID/CNIC cards.
* **Database Bootstrapper**: Single-click "Seed Mock Data" button to populate Firestore with sample bookings, chats, and providers, lighting up the dashboard graphs on localhost.

---

## 🚀 Setup & Installation

### Prerequisites
* **Android Development**: Android Studio (Ladybug or newer), JDK 11+, Android SDK 36.
* **Web/Backend**: Node.js (v18+) and Firebase CLI.

---

### Step 1: Firebase Project Configuration
1. Create a project on the [Firebase Console](https://console.firebase.google.com/).
2. Enable the following services:
   * **Authentication**: Turn on *Phone* and *Email/Password* providers.
   * **Firestore Database**: Initialize in test mode.
   * **Firebase Storage**: Set up default bucket.
3. Add an Android app with the package `com.example.homeserve`.
4. Run `./gradlew signingReport` in the root of this project and paste your local `debug` SHA-1 and SHA-256 fingerprints under your Firebase App Settings.
5. Download your custom `google-services.json` and place it in the `app/` directory of the project.

---

### Step 2: Configure and Run the Android App
1. Open the project in Android Studio.
2. Let Gradle sync and download dependencies.
3. Clean and build the app:
   ```bash
   ./gradlew clean assembleDebug
   ```
4. Run it on a real device or emulator with Google Play Services.

> [!TIP]
> **Development OTP Bypass**: To simplify testing on physical devices and emulators, the SMS validation is mocked. Enter any phone number, click send, and input `123456` in the verification box to authenticate instantly.

---

### Step 3: Run the Admin Web Panel
1. The admin web console resides inside the `admin-website/` directory.
2. Initialize Firebase configuration:
   * Open `admin-website/app.js` and replace the `firebaseConfig` object at the top with your project's configuration snippet from the Firebase Console.
3. Run locally using a local development server or open it directly:
   ```bash
   # Example using Python's built-in server
   cd admin-website
   python3 -m http.server 8000
   ```
4. Open `http://localhost:8000` in your browser. Log in using username `Abdullah` and password `pucit`.
5. Click **"Seed Mock Data"** at the bottom of the sidebar to automatically populate your Firestore and visualize the reports dashboard immediately.

---

### Step 4: Deploy Cloud Functions (Optional)
If you wish to test real-time FCM push notifications:
1. Initialize Firebase CLI in the root directory:
   ```bash
   firebase login
   firebase use --add YOUR_PROJECT_ID
   ```
2. Deploy the database-triggered functions:
   ```bash
   firebase deploy --only functions
   ```
*(Note: Deploying Cloud Functions requires upgrading your Firebase project to the Blaze plan.)*

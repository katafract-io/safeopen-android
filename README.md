# SafeOpen — URL & QR Code Safety Inspector

SafeOpen is a native Android app for inspecting URLs and QR codes before opening them. Scan QR codes with your camera, paste links from clipboard, or receive URLs via share sheet. The app classifies payloads (URL, email, phone, WiFi, SMS, etc.), runs local risk scoring (low/caution/high/unknown), traces redirect chains, and shows clear visual risk indicators. Optional backend enrichment via api.katafract.com. Built with Kotlin, MVVM+Compose, and ML Kit Barcode Scanning.

**Stack:** Android minSdk 26, targetSdk 35 | Kotlin 2.0 | Jetpack Compose | CameraX | ML Kit Barcode Scanning | OkHttp | Gson

**Build & Deploy:** `./gradlew bundleRelease` → Google Play internal track via GitHub Actions (tag v*). Keystore secrets via GitHub repo settings.

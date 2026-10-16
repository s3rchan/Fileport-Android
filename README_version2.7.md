# Fileport Android v2.7 — PC-led pairing / connecting

Fileport Android works with Fileport PC through the local FPTP/1 protocol.

- Open Fileport on Android and start setup on Windows.
- Windows selects the phone through Wi-Fi / LAN.
- Compare the six-digit pairing code on both devices and approve on both.
- Subsequent connections require phone approval, unless automatic connection was enabled for that trusted PC.
- Android shows a connected-device notification with a Disconnect action.
- Bluetooth is not required. No external server is used for device-to-device transfers.

## Android project layout

Upload **the contents** of the prepared Fileport Android source folder to the repository root.
Keep `settings.gradle`, `build.gradle`, `gradle.properties`, and the `app/` folder at the root.

## Automated APK builds

GitHub Actions builds a debug APK whenever the Android source is pushed to `main`.
Open **Actions → Build Fileport Android APK**, select the successful run, then download
**Artifacts → Fileport-Android-Debug-APK**.

The debug APK is for testing. Stable in-place upgrades need a release APK signed
with the same private Android signing key across versions. Do not commit signing
keys or passwords to GitHub.

## Naming policy

File and folder names and technical documentation are in English. Future versions
use one versioned README, such as `README_version2.8.md`.
The current `com.yakin.app` Java package and `applicationId` are preserved
for compatibility with previously installed versions.

# Fileport Android v2.8 — English source naming and automated APK build

Fileport Android uses the local FPTP/1 protocol for file transfer and PC-initiated pairing.

## What changed in v2.8

- The Java source namespace is `com.fileport.android` and its path is `app/src/main/java/com/fileport/android/MainActivity.java`.
- Android `applicationId` remains `com.yakin.app` to preserve in-place upgrade compatibility.
- `versionCode` is 28 and `versionName` is 2.8.0.
- File names, source paths, and technical documentation use English.
- GitHub Actions automatically builds a test APK from the `main` branch.

## Initial source upload

Upload the contents of the prepared Fileport Android v2.8 source directory to this repository's root.
`settings.gradle`, `build.gradle`, `gradle.properties`, and `app/` must be in the root.

The GitHub Actions workflow and this README are already created in the repository. The downloaded source ZIP includes copies for portability.

## Android APK builds

Open **Actions → Build Fileport Android APK** and choose a successful run.
Download the `Fileport-Android-Debug-APK` artifact. It contains an APK for testing.

**Android signing:** GitHub-generated debug APKs cannot reliably update existing installations because their signing keys may differ. Release APK updates require the same protected release signing key as previous versions. Do not commit keystore files or passwords.

## Pairing and networking

- Both devices are on the same reachable Wi-Fi or LAN network.
- PC selects the phone and initiates the first pairing.
- Verify the matching six-digit passkey on both screens.
- PC can request reconnection; Android may require approval or automatically accept a trusted PC if enabled.
- Connected-device notification offers Disconnect.
- Bluetooth and external website connectivity are not required for local transfer.

## Security

FPTP/1 remains an experimental encrypted application protocol without an independent third-party security audit.

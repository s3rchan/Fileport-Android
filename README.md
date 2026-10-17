# Fileport — Android

**Fileport** enables direct file sharing and messaging between an Android phone and a Windows computer over the **same Wi-Fi or local area network (LAN)**.

**Current Android version: v2.9.1** · **Windows app:** [Fileport PC](https://github.com/s3rchan/Fileport-PC)

[**Download the Android APK — GitHub Releases**](https://github.com/s3rchan/Fileport-Android/releases/latest) · [**Download the Windows app**](https://github.com/s3rchan/Fileport-PC/releases/latest)

> Fileport is different from the earlier room-code/WebRTC server project. With the current application, the **Windows PC initiates the connection**, and the **Android phone approves it**. Direct local transfers do not require an external server or internet access.

## Features

- **PC-initiated connection:** Select a phone in the Windows app and send a connection request.
- **Android approval screen:** Accept or reject an incoming connection using **OK** or **Cancel**.
- **Trusted devices:** Optionally allow **Automatically connect to this computer** for future connections.
- **Six-digit security verification:** Compare and approve the code on both devices during initial pairing.
- **File transfers and chat:** Exchange files and text messages directly over the LAN.
- **Local history:** Connection and transfer history stays on the device; transferred files are not saved in chat history.
- **In-app GitHub updates:** Check for newer versions, download an APK, and approve its installation through Android.
- **Offline APK updates:** Select a compatible APK file already stored on your phone.
- **English and Turkish interface.**

## Requirements

- **Android 10 (API level 29) or newer**.
- A Windows computer running Fileport PC, reachable on the same local network.
- A Wi-Fi/LAN connection that allows the phone and computer to communicate.

> Local file transfers may work without internet access. **Checking GitHub Releases and downloading an APK update** requires internet access.

## Install Fileport Android

1. Open [Fileport Android Releases](https://github.com/s3rchan/Fileport-Android/releases/latest).
2. Download `Fileport-Android-vX.Y.Z.apk` from the **Assets** section.
3. Review Android's installation prompt and, if necessary, allow installation from the selected source.
4. Launch Fileport on your phone. On initial setup, the app waits for a connection request from your computer.
5. Open Fileport PC on Windows, select your phone, and initiate the connection.
6. Approve the **Connection request** on Android. During first-time pairing, compare the six-digit code on both devices and confirm.
7. Once the connection is established, use chat and file sharing.

**Note:** The Windows PC initiates connections. The Android app no longer needs a separate **Reconnect** dialog or manual IP address entry.

## Update through GitHub

Since Fileport Android v2.9.0, updates can be checked from within the app:

1. Open the **⋮** menu in the upper-right corner.
2. Select **GitHub updates → Check for updates**.
3. When a newer version is available, choose **Download and install**.
4. After downloading and verifying the APK, confirm installation through Android's system installer.

The updater checks the application package ID, version, and signing certificate against the installed app. **Installation is never silent:** Android asks the user to approve it.

**APK signing matters:** A **debug APK** downloaded from GitHub Actions may use a different signing key and might not update over the official Release APK. For ordinary installation and future updates, use the **signed APK from Releases**. Uninstalling a debug build may erase local app data.

## Version history

| Version | Main changes |
| --- | --- |
| **v2.9.1** | Removed the obsolete Android reconnect/manual IP dialog while retaining PC-initiated connections. |
| **v2.9.0** | Added GitHub Releases update checks, APK download, and user-approved installation. |
| **v2.8.0** | Standardized source file naming in English and introduced GitHub Actions APK builds. |

[**All Android releases**](https://github.com/s3rchan/Fileport-Android/releases)

## Development and builds

The project combines a Java-based Android application layer with an embedded **HTML, CSS, and JavaScript** interface. Builds use Gradle.

Developer requirements:

- JDK 17
- Android SDK Platform 35 and Build Tools 35
- Gradle 8.10.2 (as configured in CI)

```bash
gradle :app:assembleDebug
```

### GitHub Actions

- [**Build Fileport Android APK**](https://github.com/s3rchan/Fileport-Android/actions) — generates a **debug APK for testing** when `main` changes, available under **Artifacts**.
- **Publish Fileport Android Release** — builds a **signed Release APK**, verifies its signature, and uploads it to GitHub Releases when an `android-vX.Y.Z` tag is pushed or a manual **Run workflow** is started.

Signed releases require these repository secrets to be configured in GitHub Actions:

| Secret | Purpose |
| --- | --- |
| `FILEPORT_ANDROID_KEYSTORE_BASE64` | Base64 encoding of the persistent release keystore |
| `FILEPORT_ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `FILEPORT_ANDROID_KEY_ALIAS` | Signing key alias |
| `FILEPORT_ANDROID_KEY_PASSWORD` | Signing key password |

**Never commit signing keys or passwords to the source repository.** Safely preserve the same signing key to support future in-place upgrades.

**Android application ID:** `com.yakin.app`. The Java namespace is `com.fileport.android`, but the application ID remains unchanged for update compatibility.

## Security and privacy

- Fileport connects devices directly over the local network.
- **Always compare the six-digit security code** when pairing for the first time.
- Do not approve connection requests from unknown computers.
- The FPTP/1 transfer protocol uses experimental application-level encryption and **has not undergone an independent third-party security audit**.
- Obtain updates only through the official [Fileport Android Releases](https://github.com/s3rchan/Fileport-Android/releases).

For additional technical information, see [FPTP_PROTOCOL_v1.md](FPTP_PROTOCOL_v1.md).

## Fileport brand identity

Approved design standards for upcoming app versions:

| Element | Standard |
| --- | --- |
| Primary blue | `#0637E7` |
| Light blue | `#368CFF` |
| Navy | `#01224F` |
| Light background | `#CFE1F4` |
| White | `#FFFFFF` |
| Font | **Open Sauce** |
| Logo | **White anchor symbol and Fileport wordmark on blue** |

> The updated brand identity will be applied to future versions. Documenting it here does not mean the current v2.9.1 APK already includes the new visuals.

---

**Fileport — direct file transfers and chat between Android and Windows.**

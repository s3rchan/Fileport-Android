# Fileport Android v2.9 — GitHub Releases updater

Android application ID remains `com.yakin.app` to preserve package identity.
Version: `2.9.0`, `versionCode 29`.

## New in v2.9

- Open the **More options** menu and choose **GitHub updates → Check for updates**.
- Fileport requests the latest stable GitHub Release over HTTPS from this repository.
- If a newer `android-vX.Y.Z` release exists with a matching
  `Fileport-Android-vX.Y.Z.apk` asset, the user can choose **Download and install**.
- The native updater limits the download size, checks the published SHA-256
  asset digest when GitHub supplies it, validates the Android application ID,
  requires a strictly newer version code, and verifies that the APK signing
  certificate matches the installed Fileport app.
- The Android system installer requests explicit approval; no silent installs.
- The existing offline APK picker remains available.
- English and Turkish menu labels.

## CI builds vs public releases

**Actions → Build Fileport Android APK** builds a **debug** APK on pushes to
`main`. Debug APKs are for testing only, and their signatures are not stable
across different GitHub runners.

**Actions → Publish Fileport Android Release** builds a **signed release** APK
when a tag matching `android-v*` is pushed. The job verifies the signature and
publishes an APK plus its SHA-256 file to GitHub Releases.

### One-time setup: protected signing key

Create a release signing keystore **on a trusted computer**, for example:

```bash
keytool -genkeypair -v -keystore fileport-release.jks -alias fileport -keyalg RSA -keysize 3072 -validity 10000
```

Save the keystore and passwords in secure backups. **Never commit the keystore,
passwords or base64-encoded key to GitHub source files.**

To encode the keystore in Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\fileport-release.jks"))
```

Add these four **Actions repository secrets** under
**Settings → Secrets and variables → Actions → New repository secret**:

- `FILEPORT_ANDROID_KEYSTORE_BASE64`: the entire base64-encoded JKS file
- `FILEPORT_ANDROID_KEYSTORE_PASSWORD`: keystore password
- `FILEPORT_ANDROID_KEY_ALIAS`: alias (e.g. `fileport`)
- `FILEPORT_ANDROID_KEY_PASSWORD`: key password

Once secrets exist and the v2.9 code is ready, push the release tag:

```bash
git tag android-v2.9.0
git push origin android-v2.9.0
```

The tag must point to the commit with `versionName '2.9.0'`. GitHub Actions
will build and publish `Fileport-Android-v2.9.0.apk` automatically. Do not
create a separate manual GitHub Release for the same tag at the same time.

## Important upgrade compatibility

An existing APK can only be upgraded in place if its **package ID and signing
certificate match**. The previous v2.8 GitHub Actions artifact was a debug APK
with a runner-generated signing key. A newly signed release APK generally
**cannot update that debug installation in place**. Uninstalling the debug
version may erase its local application data and pairing history. Back up
anything important first.

After installing the first release-signed APK, subsequent versions signed with
**the same release keystore** can upgrade in place. Never rotate the keystore
without planning an Android signing-key migration.

The repository is currently named `s3rchan/Fileport-Android-2.8`. For long-term
updating, keep one stable repository for all Android versions, rather than
creating a new repository per version.

## Security note

The updater uses the official GitHub HTTPS API and only accepts APK assets from
the Fileport Android Releases. Android verifies the installed package signing
identity before installation. FPTP/1 transport encryption remains experimental
and has not undergone an independent security audit.

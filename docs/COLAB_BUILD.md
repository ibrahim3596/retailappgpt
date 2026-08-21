# Free cloud Android build — Google Colab

Use this when GitHub Actions minutes are exhausted or the local PC is too weak for Android builds.

## What this does

Builds the `retailpos-v2` debug APK in a temporary Google Colab runtime. The PC only needs a browser.

## Recommended method

1. Open Google Colab.
2. Create a new notebook.
3. Upload a ZIP of the repository branch `retailpos-v2`.
4. Run the setup/build cells below.

```bash
# Java 17
!apt-get update -qq
!apt-get install -y -qq openjdk-17-jdk unzip wget

# Gradle 8.13
!wget -q https://services.gradle.org/distributions/gradle-8.13-bin.zip
!unzip -q gradle-8.13-bin.zip -d /opt
a! /opt/gradle-8.13/bin/gradle --version
```

Remove the accidental `a!` if copying manually; the correct command is:

```bash
!/opt/gradle-8.13/bin/gradle --version
```

### Android SDK

Colab images normally include Android SDK tooling, but verify it before building:

```bash
!ls -la $ANDROID_HOME || true
!sdkmanager --version
```

Install the required SDK packages if necessary:

```bash
!yes | sdkmanager --licenses >/dev/null || true
!sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

### Build

After extracting the uploaded repository ZIP and changing into the repository directory:

```bash
!/opt/gradle-8.13/bin/gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

The APK should be at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Download that file from the Colab file browser and install it on the Android phone.

## Private repository option

Do not paste a GitHub personal access token into a shared notebook. Prefer downloading the branch ZIP from GitHub in the browser while signed in, then uploading that ZIP to Colab.

## Device testing

Install the debug APK on the phone. If Android blocks installation, enable installation from the browser/file manager used to open the APK. Disable that permission again after installation if desired.

First test order:

1. Fresh install → owner setup
2. Staff PIN/login
3. Create product
4. Barcode/manual product lookup
5. Add product to cart
6. Cash checkout
7. Verify stock decreased
8. Credit sale + Khata
9. Purchase + received stock
10. Batch/expiry
11. Return/refund
12. Hold/resume bill
13. Force-close/reopen
14. Offline billing
15. Camera/barcode
16. Voice billing

Record every crash, incorrect total, incorrect stock value, navigation failure and permission problem. Fixes should be made against the real device result rather than guessed from static code.

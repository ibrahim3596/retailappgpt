# Free cloud Android build — Google Colab

Use this when GitHub Actions minutes are exhausted or the local PC is too weak for Android builds.

## What this does

Builds the `retailpos-v2` debug APK in a temporary Google Colab runtime. The PC only needs a browser.

## Recommended method

1. Open Google Colab.
2. Create a new notebook.
3. Download the ZIP for branch `retailpos-v2` from GitHub while signed in.
4. Upload that ZIP to Colab.
5. Extract it and run the setup/build cells below.

### Java 17 + Gradle 8.13

```bash
!apt-get update -qq
!apt-get install -y -qq openjdk-17-jdk unzip wget
!wget -q https://services.gradle.org/distributions/gradle-8.13-bin.zip
!unzip -q gradle-8.13-bin.zip -d /opt
!/opt/gradle-8.13/bin/gradle --version
```

### Android SDK

Verify the Android SDK tooling:

```bash
!ls -la $ANDROID_HOME || true
!sdkmanager --version
```

Install required SDK packages if necessary:

```bash
!yes | sdkmanager --licenses >/dev/null || true
!sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

### Build

After extracting the uploaded repository ZIP and changing into the repository directory:

```bash
!/opt/gradle-8.13/bin/gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

APK location:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Download that APK from the Colab file browser and install it on the Android phone.

## Private repository safety

Do not paste a GitHub personal access token into a shared notebook. Prefer downloading the branch ZIP through the signed-in GitHub web UI, then uploading that ZIP to Colab.

## Device testing order

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

Record every crash, incorrect total, incorrect stock value, navigation failure and permission problem. Fix against real device results rather than guessing from static code.

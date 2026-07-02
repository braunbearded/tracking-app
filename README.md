# Tracking App

Small Android app for defining trackers, starting sessions, and recording item values in SQLite.

## What it uses

- Android SDK
- SQLite
- No Google Play services
- No Firebase

## Build prerequisites

You need:

- JDK 21
- Android SDK platform 36
- Android build tools
- A configured `ANDROID_SDK_ROOT` or `local.properties`

If you build from the command line, the Gradle wrapper is included in this repo.

## How to build

### Android Studio

1. Open the project root in Android Studio.
2. Let it sync Gradle.
3. Build the app with **Build > Make Project** or **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

### Command line

From the project root:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew assembleDebug
```

If your Android SDK is not already configured, add a `local.properties` file in the project root:

```properties
sdk.dir=/path/to/Android/Sdk
```

## APK output

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release APK

This project does not currently include signing configuration. If you want a release APK, add a keystore and signing config first, then run:

```bash
./gradlew assembleRelease
```

## Notes for F-Droid

- Keep dependencies FOSS-only.
- Avoid Google Play services and Firebase.
- Keep the build reproducible and avoid closed-source plugins or SDKs.

## Project structure

- `app/src/main/java/com/example/trackingapp/MainActivity.java` main UI flow
- `app/src/main/java/com/example/trackingapp/TrackingDatabase.java` SQLite schema and access
- `app/src/main/java/com/example/trackingapp/TrackerJsonRepository.java` JSON editor save path
- `app/src/main/java/com/example/trackingapp/JsonUtil.java` JSON helpers
- `app/src/main/java/com/example/trackingapp/Models.java` data model classes


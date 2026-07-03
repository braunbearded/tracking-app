# Repository Guidelines

## Project Structure & Module Organization
This is a single-module Android app.

- `app/src/main/java/com/example/trackingapp/` contains the app code.
  - `MainActivity.java` owns the UI flow.
  - `TrackingDatabase.java` handles SQLite access.
  - `TrackerJsonRepository.java`, `JsonUtil.java`, and `Models.java` hold persistence and model helpers.
- `app/src/main/res/` contains Android resources such as styles and layouts.
- `app/src/main/AndroidManifest.xml` defines the app entry point and manifest config.

## Build, Test, and Development Commands
Use the Gradle wrapper from the repository root.

- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew assembleRelease` builds a release APK if signing is configured.
- `./gradlew clean` removes build outputs.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Coding Style & Naming Conventions
Follow the existing Java style in the repo:

- Use 4-space indentation.
- Keep package names lowercase, for example `com.example.trackingapp`.
- Use `PascalCase` for classes and `camelCase` for methods, fields, and local variables.
- Prefer descriptive UI helper names such as `primaryButton()` or `showHome()`.

No formatter or linter is currently configured, so keep changes consistent with nearby code.

## Testing Guidelines
There is no test suite checked in yet (`app/src/test/` and `app/src/androidTest/` are absent). If you add tests, place unit tests under `app/src/test/` and instrumented tests under `app/src/androidTest/`. Name tests after the behavior being verified, such as `TrackingDatabaseTest`.

## Commit & Pull Request Guidelines
Recent history uses short, imperative commit messages such as `Implement native Android tracking app` and `add new version`. Keep commits focused and descriptive.

Pull requests should include:

- A short summary of the user-visible change.
- Notes on build or runtime impact.
- Screenshots or screen recordings for UI changes.
- Linked issues when applicable.

## Security & Configuration Tips
Do not commit `local.properties`, keystores, or other machine-specific Android SDK settings. The README recommends setting `sdk.dir` locally when building outside Android Studio.

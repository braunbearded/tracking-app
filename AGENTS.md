# Repository Guidelines

## Project Structure & Module Organization
This is a single-module Android app without Google Play Services.

- `app/src/main/java/com/example/trackingapp/` contains app code.
  - `MainActivity.java` owns the screen flow, footer navigation, settings, and dialogs.
- `TrackingDatabase.java` handles SQLite access.
- `theme/ThemeStore.java` stores theme mode, accent color, and derived palette values.
- `ui/HomeUi.java` renders the session and tracker overviews.
- `ui/AppUi.java` builds shared Material-style widgets.
- `ui/SettingsUi.java` renders the settings screen and About dialog.
- `ui/TrackerFlowUi.java` owns the tracker editor, session flow, and tracker selection dialog.
- `TrackerJsonRepository.java`, `JsonUtil.java`, and `Models.java` cover persistence and models.
- `app/src/main/res/` contains resources and styles.
- `app/src/main/AndroidManifest.xml` defines the entry point.

## Build, Test, and Development Commands
Use the Gradle wrapper from the repository root.

- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew assembleRelease` builds a release APK if signing is configured.
- `./gradlew clean` removes build outputs.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Coding Style & Naming Conventions
Follow the existing Java style:

- Use 4-space indentation.
- Keep package names lowercase, for example `com.example.trackingapp`.
- Use `PascalCase` for classes and `camelCase` for methods, fields, and variables.
- Prefer descriptive UI helpers such as `primaryButton()`, `showHome()`, and `navItem()`.

Keep UI changes consistent with the current Material 3 direction:

- top app bar with app title and overflow menu
- bottom navigation with two equal-width tabs
- icon and label color indicate the selected tab only
- footer touch areas should stay rectangular and extend to the edges
- About dialog and settings should remain compact and scrollable on small screens
- shared UI helpers belong in `ui/AppUi.java`; screen-specific settings logic belongs in `ui/SettingsUi.java`
- overview lists belong in `HomeUi.java`; keep `MainActivity.java` focused on routing and lifecycle
- tracker editing and session entry belong in `TrackerFlowUi.java`

## Testing Guidelines
There is no checked-in test suite yet. If you add tests, place unit tests under `app/src/test/` and instrumented tests under `app/src/androidTest/`. Name tests after the behavior being verified, such as `TrackingDatabaseTest`.

## Commit & Pull Request Guidelines
Recent history uses short, imperative commit messages. Keep commits focused and descriptive.

Pull requests should include:

- A short summary of the user-visible change.
- Notes on build or runtime impact.
- Screenshots or screen recordings for UI changes.
- Linked issues when applicable.

## Security & Configuration Tips
Do not commit `local.properties`, keystores, or other machine-specific Android SDK settings. The project is intended to stay Android-only, with SQLite storage and no hidden Google service dependency.

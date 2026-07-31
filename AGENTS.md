# Repository Guidelines

## Project Structure & Module Organization
This is a single-module Android app without Google Play Services.

- `app/src/main/java/com/zaelio/app/` contains app code.
  - `MainActivity.java` owns routing, lifecycle, top app bar actions, and bottom navigation.
- `TrackingDatabase.java` handles SQLite schema, destructive upgrades, seed data, and data access. Current schema version is 7.
- `theme/ThemeStore.java` stores theme mode, accent color, font scale, and derived palette values.
- `ui/AppUi.java` builds shared Material-style widgets.
- `ui/SettingsUi.java` renders the settings and About screens.
- `HomeUi.java` renders the session and tracker overviews, including long-press/left-swipe/overflow-menu delete gestures and drag-handle ordering.
- `ReorderHelper.java` contains shared constrained drag-reorder touch logic for overview and editor lists.
- `TrackerFlowUi.java` owns the tracker editor, session flow, and tracker selection.
- `FieldInputUi.java` renders session field controls, including timers, numeric controls, and multiline text.
- `TrackerJsonRepository.java`, `BackupJsonRepository.java`, `JsonUtil.java`, `FormatUtil.java`, and `Models.java` cover JSON persistence, backup import/export, shared formatting, and model classes.
- `app/src/main/res/` contains resources and styles.
- `app/src/main/AndroidManifest.xml` defines the entry point and the `VIBRATE` permission used for delete-selection feedback.

## Build, Test, and Development Commands
Use the Gradle wrapper from the repository root.

- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew testDebugUnitTest` runs local JVM/Robolectric tests. Run this before reporting a completed code change unless the user explicitly asks not to.
- `./gradlew assembleRelease` builds a release APK if signing is configured.
- `./gradlew clean` removes build outputs.

After code changes, run `./gradlew testDebugUnitTest` and inspect the output for real failures, warnings, or skipped coverage concerns; do not rely only on the final success line. For UI/build-impacting changes, also run `./gradlew assembleDebug`.

When behavior, commands, schema, project structure, or user-facing features change, update the relevant docs in the same change (`README.md`, `AGENTS.md`, or other docs).

Keep the standard Gradle wrapper files committed: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, and `gradle/wrapper/gradle-wrapper.properties`.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Coding Style & Naming Conventions
Follow the existing Java style:

- Use 4-space indentation.
- Keep package names lowercase, using `com.zaelio.app` for app code.
- Keep Java package declarations aligned with file paths when moving files.
- Use `PascalCase` for classes and `camelCase` for methods, fields, and variables.
- Prefer descriptive UI helpers such as `primaryButton()`, `showHome()`, and `navItem()`.

Keep UI changes consistent with the current Material 3 direction:

- top app bar with app title and overflow menu
- bottom navigation with two equal-width tabs
- icon and label color indicate the selected tab only
- footer touch areas should stay rectangular and extend to the edges
- settings, data transfer, and about screens should remain compact and scrollable on small screens
- shared screen/dialog helpers belong in `ui/AppUi.java`; screen-specific settings logic belongs in `ui/SettingsUi.java`
- overview lists, delete gestures, and drag ordering belong in `HomeUi.java`; keep `MainActivity.java` focused on routing and lifecycle
- shared spacing/touch-size constants, Material input helpers, and list-row/action-icon widgets belong in `ui/AppUi.java`; avoid scattering repeated `ui.px(...)` literals through screen code
- shared reorder mechanics belong in `ReorderHelper.java`; list-specific persistence stays with the owning screen/database code
- tracker editing and session routing belong in `TrackerFlowUi.java`; individual session input widgets belong in `FieldInputUi.java`

## Testing Guidelines
Unit tests live under `app/src/test/` and use JUnit 4 plus Robolectric for Android SQLite coverage. Instrumented tests, if needed, belong under `app/src/androidTest/`. Name tests after the behavior being verified, such as `TrackingDatabaseTest`.

Prioritize tests for:

- SQLite schema shape and destructive upgrades, especially keeping removed item tables/columns out.
- Tracker/session JSON import/export and editor autosave behavior.
- Session record preservation when tracker definitions are edited or imported.
- Numeric, duration, and string field parsing and field-size behavior.

## Commit & Pull Request Guidelines
Recent history uses short, imperative commit messages. Keep commits focused and descriptive.

Pull requests should include:

- A short summary of the user-visible change.
- Notes on build or runtime impact.
- Screenshots or screen recordings for UI changes.
- Linked issues when applicable.

## Known Risks & Maintenance Notes
Before changing persistence or editor flows, check these areas carefully:

- Database migrations must be backward compatible. In particular, `TrackingDatabase.migrateToFieldsOnly()` is sensitive to cursor column indexes.
- Editing a tracker through `TrackerJsonRepository.updateTracker()` currently rebuilds fields and can delete existing `field_records`; avoid accidental session data loss.
- The app builds its UI programmatically. Keep shared widgets in `ui/AppUi.java` and avoid duplicating style logic in screen classes.
- Timer state in `TrackerFlowUi` is in-memory and should be cleared on screen exit or activity destruction.
- Delete candidate feedback in overview lists should stay clear even when the accent color is red; keep the non-color cue (strikethrough/scale/alpha) alongside vibration.
- Overview ordering is persisted through `overviewOrder`; new rows should remain visible near the top and migrations should preserve the old newest-first default order.

## Security & Configuration Tips
Do not commit `local.properties`, keystores, or other machine-specific Android SDK settings. The project is intended to stay Android-only, with SQLite storage and no hidden Google service dependency.

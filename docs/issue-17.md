# Issue 17 - Design angleichen

Reduced the overall corner radius across shared app surfaces so the UI reads flatter and closer to Material 3.

What changed:
- Card and container radius moved from `28dp` to `4dp`.
- Shared button radius moved from `24dp` to `6dp`.
- Small chip-style labels moved from a fully pill-shaped radius to `4dp`.
- Start screen cards now share the same compact anatomy for sessions and trackers.
- Top-level hero and about surfaces now use the same restrained card treatment with a thin accent bar.

Validation:
- `./gradlew assembleDebug`

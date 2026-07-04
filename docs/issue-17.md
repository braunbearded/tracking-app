# Issue 17 - Design angleichen

Reduced the overall corner radius across shared app surfaces so the UI reads flatter and closer to Material 3.

What changed:
- Card and container radius moved from `28dp` to `12dp`.
- Shared button radius moved from `24dp` to `16dp`.
- Small chip-style labels moved from a fully pill-shaped radius to `16dp`.

Validation:
- `./gradlew assembleDebug`

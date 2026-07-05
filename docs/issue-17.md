# Issue 17 - Design angleichen

Reduced the overall corner radius across shared app surfaces so the UI reads flatter and closer to Material 3.

What changed:
- Card and container radius moved from `28dp` to `4dp`.
- Shared button radius moved from `24dp` to `6dp`.
- Small chip-style labels moved from a fully pill-shaped radius to `4dp`.
- All primary surface cards now use the same anatomy: eyebrow, title, subtitle, meta, and content.
- Start screen cards, tracker/session editor cards, and settings/about cards now follow that same structure.
- Top-level hero and about surfaces now use the same restrained card treatment as the rest of the app.

Validation:
- `./gradlew assembleDebug`

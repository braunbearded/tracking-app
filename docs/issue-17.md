# Issue 17 - Design angleichen

Reduced the overall corner radius across shared app surfaces so the UI reads flatter and closer to Material 3.

What changed:
- Card and container radius moved from `28dp` to `4dp`.
- Shared button radius moved from `24dp` to `6dp`.
- Small chip-style labels moved from a fully pill-shaped radius to `4dp`.
- Primary surfaces now use the same restrained card anatomy with eyebrow, title, meta, and content.
- Start screen, tracker/session editor, and settings/about screens now share the same visual language.
- Tracker and session screens now use the shared top app bar with contextual actions like delete/complete.
- Explanatory copy and duplicate save actions were removed in favor of direct autosave on change.
- Borders were removed from cards and button surfaces where possible to keep the UI cleaner.
- The new-session chooser was simplified to a plain text header and list.

Validation:
- `./gradlew assembleDebug`

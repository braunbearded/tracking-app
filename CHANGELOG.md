# Changelog

## 1.0.5

- Removed Android Gradle dependency metadata from release APKs so F-Droid accepts reproducible-build binaries.

## 1.0.4

- Release builds now use JDK 21 to match the F-Droid build server. 
- Added signing-key verification to the GitHub release workflow and release script.
- Added a local helper script to check F-Droid reproducible builds.   

## 1.0.3

- - F-Droid-Metadaten für reproduzierbare Builds ergänzt.
- - Release-Script aktualisiert, damit F-Droid volle Commit-Hashes statt Tags verwendet.
- - F-Droid- und Release-Dokumentation präzisiert.

## 1.0.2

- Gradle-Konfiguration auf die neue Property-Assignment-Syntax mit `=` aktualisiert.

## 1.0.1

- F-Droid/Fastlane-Metadaten und Screenshot für die Paketierung ergänzt.
- Quellcode-Links in README und About-Screen auf `github.com/braunbearded/zaelio` aktualisiert.
- Gradle-Wrapper-Prüfsumme ergänzt, damit der Build reproduzierbarer und prüfbarer ist.
- Release- und Tagging-Dokumentation präzisiert.

## 1.0.0

- Erste öffentliche Version von Zaelio.
- Eigene Tracker, Sessions und lokale SQLite-Speicherung.
- JSON Import/Export für Tracker, Sessions und Backups.
- Helles/dunkles Design, Akzentfarbe, Schriftgröße und Feldgröße.

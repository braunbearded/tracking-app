# 📊 Zaelio

Eine kleine Android-App zum Erstellen eigener Tracker, Starten von Sessions und Speichern von Messwerten lokal in SQLite.

## ✨ Features

- Eigene Tracker mit Feldern und Items erstellen
- Sessions erfassen und fortsetzen
- Listen-Einträge per Long-Press, Links-Swipe oder `...`-Menü löschen
- Sessions und Tracker per Drag-Handle in der Übersicht sortieren
- Android-Zurück navigiert sinnvoll; auf Home beendet erst ein schneller Doppel-Zurück-Druck die App
- Werte lokal in SQLite speichern
- Tracker, Sessions oder komplette Backups als JSON importieren/exportieren
- Helles/dunkles Design, Schriftgröße, Akzentfarbe und globale Feldgröße einstellbar
- Kein Google Play Services, kein Firebase, keine Cloud

## 📱 Screenshots

Noch keine Screenshots im Repository. Lege sie z. B. unter `docs/screenshots/` ab und verlinke sie hier.

## 🧰 Benötigte Abhängigkeiten

Zum Bauen brauchst du lokal:

- JDK 17 oder neuer
- Android SDK mit Platform `36`
- Android Build Tools passend zum SDK
- Gradle Wrapper aus diesem Repository (`./gradlew`)

Projektabhängigkeiten:

- Android Gradle Plugin `9.3.1`
- Gradle `9.5.1`
- Material Components `com.google.android.material:material:1.14.0`

Test-Abhängigkeiten:

- JUnit `4.13.2`
- AndroidX Test Core `1.7.0`
- Robolectric `4.16.1`
- ASM `9.10.1` für Robolectric auf modernen JDKs
- org.json `20260719`

Das Projekt nutzt absichtlich keine Google Play Services und kein Firebase.

## ⚙️ Einrichtung

Wenn dein Android SDK nicht automatisch gefunden wird, erstelle im Projektordner eine `local.properties`:

```properties
sdk.dir=/pfad/zu/deinem/Android/Sdk
```

Optional Java setzen:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

## 🛠️ Debug-Build erstellen

```bash
./gradlew assembleDebug
```

Die APK liegt danach hier:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 🚀 Release-Version erstellen

1. Version in `app/build.gradle` erhöhen:

```gradle
versionCode 2
versionName '1.1'
```

2. Release bauen:

```bash
./gradlew assembleRelease
```

Die unsigned Release-APK liegt danach hier:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

Für eine installierbare/veröffentlichbare Release-APK brauchst du zusätzlich eine Android-Signierung per Keystore. Diese ist aktuell nicht im Repository konfiguriert.

## 🔐 Release signieren

Keystore erstellen:

```bash
keytool -genkeypair -v -keystore zaelio.jks -alias zaelio -keyalg RSA -keysize 2048 -validity 10000
```

Danach eine Signing Config in `app/build.gradle` ergänzen oder die APK extern mit `apksigner` signieren. Keystore-Dateien und Passwörter niemals committen.

## 📁 Projektstruktur

```text
app/src/main/java/com/zaelio/app/
├── MainActivity.java              # Routing, Lifecycle, Top Bar, Navigation
├── TrackingDatabase.java          # SQLite Schema v6, Migrationen, Datenzugriff
├── TrackerJsonRepository.java     # JSON Import/Export und Tracker-Speicherung
├── BackupJsonRepository.java      # JSON Backup für Tracker, Sessions und Werte
├── JsonUtil.java                  # JSON-Helfer
├── Models.java                    # Datenmodelle
├── HomeUi.java                    # Session-/Tracker-Übersicht
├── ReorderHelper.java             # Gemeinsames Drag-Reorder-Verhalten
├── TrackerFlowUi.java             # Tracker-Editor und Session-Routing
├── FieldInputUi.java              # Eingabefelder, Timer und Zahlensteuerung
├── theme/ThemeStore.java          # Theme, Akzentfarbe, Schriftgröße
└── ui/
    ├── AppUi.java                 # Gemeinsame UI-Bausteine
    └── SettingsUi.java            # Einstellungen und Über-Screen
```

## 🧪 Tests

Lokale Unit-Tests laufen mit JUnit und Robolectric:

```bash
./gradlew testDebugUnitTest
```

Aktueller Fokus:

- `JsonUtilTest` prüft JSON-Roundtrips und Tracker-Export.
- `TrackingDatabaseTest` prüft Seed-Daten, Sessions, Records, Previous Values, Löschlogik, Übersichtssortierung und Migration auf Schema v6.
- `BackupJsonRepositoryTest` prüft kompletten Export/Import sowie Tracker-only Export.

Zusätzlicher Build-Check:

```bash
./gradlew assembleDebug
```

## 📝 Hinweise

- App-Daten bleiben lokal auf dem Gerät.
- Die App nutzt `android.permission.VIBRATE` nur für kurzes Feedback beim Markieren eines Löschkandidaten.
- `local.properties`, Keystores und Passwörter nicht committen.
- Für F-Droid/OSS-Builds nur freie Abhängigkeiten verwenden.

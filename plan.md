# Prompt: Mobile Tracking-App umsetzen

Setze eine generische mobile-first Tracking-App mit folgendem Technologie-Stack um:

    android (ohne google apis)
    sqlite

Das visuelle Design ist nicht entscheidend. Wichtig sind der generelle Aufbau, die Datenstruktur und das Verhalten der App.

Es soll zunächst keine zusätzliche Funktionalität wie Import/Export, Duplizieren, Statistiken oder Sync geben. Der Fokus liegt auf der Kernfunktion: Tracker definieren, Sessions starten, Werte erfassen und speichern.

---

## 1. Begriffe

Die App verwendet folgende fachliche Begriffe:

    Tracker
    Eine Vorlage, die definiert, was getrackt wird.
    Ein Tracker enthält mehrere Items.

    Item
    Eine erfassbare Einheit innerhalb eines Trackers.
    Ein Item enthält mehrere Fields.

    Field
    Ein einzelnes Eingabefeld innerhalb eines Items.
    Ein Field hat einen Typ, zum Beispiel string, int, float oder duration.

    Session
    Ein konkreter Durchlauf eines Trackers.
    Eine Session gehört immer zu genau einem Tracker.

    ItemRecord
    Die gespeicherten Werte eines Items innerhalb einer Session.

## 1.1 Aktueller Stand

Der Code setzt diese Anforderungen bereits um:

- Android-App ohne Google Play Services
- SQLite als lokale Datenbasis
- Material-3-orientierte Oberfläche
- Startscreen mit `Sessions` und `Tracker`
- Overflow-Menü mit `Einstellungen` und `Über die App`
- Einstellungen für Darkmode und 8 Akzentfarben
- Floating Action Button für neue Sessions oder Tracker
- About-Dialog mit Repository- und Versionsdaten
- Footer-Navigation mit zwei gleich breiten Touch-Flächen, damit die Klickbereiche bis an die Kanten reichen

Offen oder bewusst noch nicht vorgesehen:

- Import/Export
- Sync
- Statistikansichten
- Duplizieren von Trackern oder Sessions

---

## 2. Grundidee

Die App dient dazu, wiederkehrende Werte schnell zu erfassen.

Der Nutzer definiert **Tracker**.

Ein Tracker beschreibt einen Tracking-Kontext, zum Beispiel:

    Training

Innerhalb eines Trackers gibt es mehrere **Items**, zum Beispiel:

    Klimmzug 1
    Klimmzug 2
    Liegestütz
    Plank

Jedes Item kann aus einem oder mehreren **Fields** bestehen.

Beispiel:

    Item: Klimmzug 1

    Fields:
    - Wiederholungen: int
    - Zusatzgewicht: float
    - Notiz: string

Für einen Tracker können mehrere **Sessions** erstellt werden.

Eine Session ist ein konkreter Durchlauf dieses Trackers.

Beispiel:

    Tracker: Training

    Session 1
    - Klimmzug 1: 10 Wiederholungen, 20 kg
    - Klimmzug 2: 8 Wiederholungen, 15 kg

    Session 2
    - Klimmzug 1: 11 Wiederholungen, 20 kg
    - Klimmzug 2: 8 Wiederholungen, 17.5 kg

---

## 3. Hauptbereiche der App

Die Startseite besteht aus mindestens zwei Bereichen, zum Beispiel als Tabs:

    [ Sessions ] [ Tracker ]

---

## 4. Tab: Sessions

Dieser Bereich zeigt alle vorhandenen Sessions über alle Tracker hinweg.

Jede Session sollte anzeigen:

    Tracker-Name
    Session-Nummer oder Datum
    Status: offen / abgeschlossen
    Fortschritt oder Anzahl gespeicherter Items
    Kurze Vorschau der gespeicherten Werte

Im Header dieses Tabs gibt es eine Aktion:

    Neue Session starten

Beim Klick darauf wird zuerst ausgewählt, für welchen **Tracker** eine neue Session gestartet werden soll.

Danach öffnet sich die Tracker-Ansicht für diese neue Session.

Ein Klick auf eine bestehende Session öffnet diese Session.

Dabei gilt:

    Offene Sessions können weiter bearbeitet werden.
    Abgeschlossene Sessions sind unveränderlich.
    Abgeschlossene Sessions dürfen geöffnet, aber nicht mehr bearbeitet werden.
    In abgeschlossenen Sessions sind Eingabefelder und Controls deaktiviert oder die Ansicht ist read-only.

---

## 5. Tab: Tracker

Dieser Bereich zeigt alle definierten Tracker.

Für jeden Tracker sollte angezeigt werden:

    Name des Trackers
    Anzahl der Items
    Anzahl der Fields
    Optional: letzte Nutzung

Ein Tracker kann bearbeitet werden.

Für einen ersten Stand reicht ein einfacher Editor, zum Beispiel:

    JSON-Editor

oder:

    Einfaches Formular

Bearbeitbar sein sollten:

    Tracker-Name
    Items
    Fields pro Item
    Field-Typ
    Default-Werte
    Increments
    Einheiten
    Option prefillFromPrevious

Bei bestehenden Sessions gilt:

    Änderungen an einem Tracker gelten ab sofort für neue oder offene Sessions.
    Abgeschlossene Sessions bleiben unveränderlich.
    Für den ersten Stand ist keine Versionierung nötig.

---

## 6. Tracker-Ansicht

Die Tracker-Ansicht ist die eigentliche Eingabemaske für eine einzelne Session eines Trackers.

Sie zeigt die Items dieses Trackers an.

Beispiel:

    Tracker: Training
    Session: offen

    [ Klimmzug 1 ]
    [ Klimmzug 2 ]
    [ Liegestütz ]
    [ Plank ]

Die Items befinden sich in einer scrollbaren Ansicht.

Da die App mobil genutzt werden soll, ist ein Aufbau sinnvoll, bei dem ein Item groß und fokussiert dargestellt wird.

Mögliche Navigation:

    Speichern & weiter
    Nur weiter
    Zurück
    Session speichern / schließen
    Übersicht

Der Nutzer soll schnell von einem Item zum nächsten springen können.

---

## 7. Item

Ein Item ist eine erfassbare Einheit innerhalb eines Trackers.

Beispiel:

    Klimmzug 1

Ein Item kann mehrere Fields enthalten.

Beispiel:

    Klimmzug 1

    Wiederholungen
    [ - ] [ 10 ] [ + ]

    Zusatzgewicht
    [ - ] [ 20.0 kg ] [ + ]

    Notiz
    [ sauberer Satz ]

---

## 8. Field-Typen

Die App unterstützt mindestens folgende Field-Typen:

---

### 8.1 string

Freitext-Eingabe.

Eigenschaften:

    type: string
    defaultValue: optional
    prefillFromPrevious: true/false

Controls:

    Keine zusätzlichen Controls notwendig

---

### 8.2 int

Ganzzahl-Eingabe.

Eigenschaften:

    type: int
    defaultValue
    increment
    unit: optional
    prefillFromPrevious: true/false

Controls:

    Minus-Button
    Input-Feld
    Plus-Button

Beim Drücken von Plus oder Minus wird der Wert verändert.

Wichtig:

    Beim Drücken der Controls soll das Input-Feld nicht fokussiert werden.
    Die mobile Tastatur soll also nicht erscheinen.

---

### 8.3 float

Kommazahl-Eingabe.

Eigenschaften:

    type: float
    defaultValue
    increment
    decimals
    unit: optional
    prefillFromPrevious: true/false

Controls:

    Minus-Button
    Input-Feld
    Plus-Button

Auch hier darf beim Drücken der Controls nicht automatisch die Tastatur geöffnet werden.

---

### 8.4 duration

Zeitdauer-Eingabe.

Eigenschaften:

    type: duration
    defaultValue: optional
    prefillFromPrevious: true/false

Controls:

    Start
    Stop
    Reset optional

Die Duration speichert eine Dauer, nicht nur einen Textwert.

Duration-Werte sollen intern als Zahl gespeichert werden, zum Beispiel als Sekunden oder Millisekunden.

Die Anzeige erfolgt formatiert, zum Beispiel:

    00:01:32

---

## 9. Vorbelegung mit letztem Wert

Es gibt kein separates previousValue-Feld im Datenmodell.

Die Vorbelegung wird ausschließlich über die Field-Option gesteuert:

    prefillFromPrevious: true/false

Wenn `prefillFromPrevious` auf `true` steht, wird beim Erstellen oder Öffnen einer offenen Session der letzte gespeicherte Wert dieses Fields verwendet.

Die Zuordnung erfolgt über:

    Tracker
    Item
    Field-Key
    letzte vorherige Session desselben Trackers

Wichtig:

    Es soll immer der letzte gespeicherte Wert verwendet werden.
    Wenn der letzte gespeicherte Wert null ist, dann wird null übernommen.
    Wenn kein vorheriger Wert existiert, wird der defaultValue verwendet.
    Wenn kein defaultValue existiert, bleibt das Field leer oder null.
    prefillFromPrevious erzeugt keinen separaten gespeicherten previousValue.
    Der übernommene Wert wird erst Teil der aktuellen Session, wenn der Nutzer das Item beziehungsweise die Session speichert.

Wenn `prefillFromPrevious` auf `false` steht, wird der defaultValue verwendet.

---

## 10. Sessions

Eine Session gehört immer zu genau einem Tracker.

Eine Session hat mindestens:

    id
    trackerId
    createdAt
    updatedAt
    status

Mögliche Status:

    open
    completed

Eine Session startet im Status:

    open

Eine Session wird abgeschlossen, sobald der Nutzer sie über eine Speichern-/Schließen-Aktion beendet.

Beispiel für eine Aktion:

    Session speichern / schließen

oder am Ende der Eingabe:

    Speichern & Session abschließen

Sobald eine Session abgeschlossen ist:

    status = completed
    Die Session ist unveränderlich.
    Werte dürfen nicht mehr geändert werden.
    Die Session darf weiterhin geöffnet und angesehen werden.
    Die Session darf nicht mehr in den Bearbeitungsmodus wechseln.

Eine Session gilt nicht automatisch nur deshalb als abgeschlossen, weil alle Items einmal gespeichert wurden.

Der Abschluss erfolgt explizit durch die Speichern-/Schließen-Aktion des Nutzers.

---

## 11. ItemRecords

Ein ItemRecord ist ein gespeicherter Wert für ein Item innerhalb einer Session.

Ein ItemRecord gehört zu:

    sessionId
    trackerId
    itemId

Ein ItemRecord enthält die Werte aller Fields dieses Items.

Beispiel:

    sessionId: session-003
    trackerId: training
    itemId: pullup-1

    values:
      reps: 10
      weight: 20.0
      note: "sauber"

Für ein Item sollte es innerhalb einer Session maximal einen aktuellen ItemRecord geben.

Wenn eine offene Session bearbeitet wird, wird der bestehende ItemRecord aktualisiert.

Wenn eine Session abgeschlossen ist, dürfen ItemRecords nicht mehr verändert werden.

---

## 12. Datenmodell

Das Datenmodell soll mindestens folgende Strukturen abbilden:

    Tracker
    - id
    - name
    - description optional
    - items
    - createdAt
    - updatedAt

    Item
    - id
    - trackerId
    - title
    - order
    - fields

    FieldDefinition
    - id
    - itemId
    - key
    - label
    - type: string | int | float | duration
    - order
    - defaultValue
    - increment
    - decimals
    - unit
    - required optional
    - prefillFromPrevious

    Session
    - id
    - trackerId
    - createdAt
    - updatedAt
    - status: open | completed

    ItemRecord
    - id
    - sessionId
    - trackerId
    - itemId
    - values
    - createdAt
    - updatedAt

Die Persistenz soll passend zum angegebenen Technologie-Stack umgesetzt werden.

Für einen ersten Stand darf eine einfache lokale Persistenz verwendet werden, sofern die Datenstruktur später sauber erweitert werden kann.

---

## 13. App-Flows

---

### 13.1 Flow: Startseite öffnen

    App startet
    → Tab "Sessions" wird angezeigt
    → Alle vorhandenen Sessions werden geladen
    → Sessions werden sinnvoll sortiert, zum Beispiel neueste zuerst

---

### 13.2 Flow: Neue Session starten

    Nutzer klickt "Neue Session starten"
    → Tracker-Auswahl öffnet sich
    → Nutzer wählt einen Tracker
    → Neue Session wird mit status = open erstellt
    → Tracker-Ansicht öffnet sich
    → Erstes Item des Trackers wird angezeigt
    → Fields werden anhand von prefillFromPrevious oder defaultValue initialisiert

---

### 13.3 Flow: Offene Session öffnen

    Nutzer klickt auf eine offene Session
    → Passende Session wird geladen
    → Passender Tracker wird geladen
    → Gespeicherte ItemRecords werden geladen
    → Tracker-Ansicht öffnet sich im Bearbeitungsmodus

Sinnvolle Startposition kann sein:

    erstes noch nicht gespeichertes Item

oder:

    erstes Item

---

### 13.4 Flow: Abgeschlossene Session öffnen

    Nutzer klickt auf eine abgeschlossene Session
    → Passende Session wird geladen
    → Passender Tracker wird geladen
    → Gespeicherte ItemRecords werden geladen
    → Tracker-Ansicht öffnet sich im read-only Modus
    → Eingabefelder und Controls sind deaktiviert
    → Es gibt keine Speichern-Aktion

---

### 13.5 Flow: Item speichern

    Nutzer gibt Werte ein
    → Nutzer klickt "Speichern & weiter"
    → ItemRecord für aktuelles Item wird gespeichert oder aktualisiert
    → Fortschritt der offenen Session wird aktualisiert
    → Nächstes Item wird angezeigt

Wenn das letzte Item erreicht ist:

    → Nutzer kann die Session explizit speichern / schließen
    → Erst dann wird die Session abgeschlossen

---

### 13.6 Flow: Session abschließen

    Nutzer klickt "Session speichern / schließen"
    → Alle aktuell gespeicherten ItemRecords bleiben erhalten
    → Session wird auf status = completed gesetzt
    → Session wird unveränderlich
    → Nutzer wird zur Sessions-Übersicht zurückgeführt oder sieht die Session im read-only Modus

---

### 13.7 Flow: Tracker bearbeiten

    Nutzer öffnet Tab "Tracker"
    → Nutzer wählt einen Tracker
    → Editor öffnet sich
    → Nutzer ändert Items oder Fields
    → Änderungen werden gespeichert

Bei bestehenden Sessions gilt:

    Offene Sessions verwenden die aktuellen Tracker-Definitionen.
    Abgeschlossene Sessions bleiben unveränderlich.
    Für den ersten Stand ist keine Versionierung nötig.

---

## 14. Wichtige UI-Struktur

Die App besteht grob aus diesen Screens:

    HomeScreen
    ├── SessionsTab
    │   ├── Header mit "Neue Session starten"
    │   └── SessionList
    │
    ├── TrackersTab
    │   ├── TrackerList
    │   └── TrackerEditor
    │
    └── TrackerSessionScreen
        ├── Header mit Tracker + Session
        ├── Scrollbare Items
        ├── FieldControls
        └── Navigation / Session abschließen

---

## 15. Wichtige Verhaltensregeln

Die wichtigsten Regeln für die Umsetzung:

    Eine Session gehört zu genau einem Tracker.

    Ein Tracker besteht aus mehreren Items.

    Ein Item besteht aus mehreren Fields.

    Fields werden über prefillFromPrevious optional mit dem letzten gespeicherten Wert vorbelegt.

    Es gibt kein separates previousValue-Feld.

    Wenn der letzte gespeicherte Wert null ist, wird null übernommen.

    Controls für int, float und duration dürfen nicht ungewollt die mobile Tastatur öffnen.

    Offene Sessions können bearbeitet werden.

    Abgeschlossene Sessions sind unveränderlich.

    Eine Session wird nur durch eine explizite Speichern-/Schließen-Aktion abgeschlossen.

    Neue Sessions werden über die Sessions-Übersicht gestartet.

    Beim Starten einer neuen Session muss ein Tracker ausgewählt werden.

    Tracker können auf einer eigenen Übersichtsseite angezeigt und bearbeitet werden.

    Zusätzliche Funktionen wie Import, Export, Sync, Statistiken oder Duplizieren sollen zunächst nicht umgesetzt werden.

---

## 16. Architektur

Die App sollte sauber strukturiert sein.

Empfohlene logische Trennung:

    UI
    → zeigt Screens und Controls

    State Layer
    → hält aktuelle Session, aktuellen Tracker und aktuelle Eingaben

    Domain Logic
    → berechnet Vorbelegungen, Fortschritt und Status

    Storage Layer
    → liest und schreibt Tracker, Sessions und ItemRecords

    Platform Layer
    → enthält plattformspezifische Funktionen des gewählten Technologie-Stacks

---

## 17. Erwartetes Ergebnis

Erstelle eine lauffähige App mit:

    Mobile-first Layout
    Startseite mit Tabs "Sessions" und "Tracker"
    Übersicht aller Sessions
    Start einer neuen Session mit Tracker-Auswahl
    Tracker-Ansicht für eine Session
    Items mit mehreren Fields
    Field-Typen string, int, float und duration
    Vorbelegung über prefillFromPrevious
    Kein separates previousValue-Feld
    Bearbeitbare Tracker
    Offene Sessions bearbeitbar
    Abgeschlossene Sessions read-only und unveränderlich
    Explizites Abschließen einer Session
    Persistenz passend zum angegebenen Technologie-Stack

Für einen ersten Stand darf die Persistenz einfach sein.

Die Struktur soll aber so geplant sein, dass sie später sauber erweitert werden kann.# Prompt: Mobile Tracking-App umsetzen

Setze eine generische mobile-first Tracking-App mit folgendem Technologie-Stack um:

    <TECHNOLOGIE_STACK_HIER_EINSETZEN>

Das visuelle Design ist nicht entscheidend. Wichtig sind der generelle Aufbau, die Datenstruktur und das Verhalten der App.

Es soll zunächst keine zusätzliche Funktionalität wie Import/Export, Duplizieren, Statistiken oder Sync geben. Der Fokus liegt auf der Kernfunktion: Tracker definieren, Sessions starten, Werte erfassen und speichern.

---

## 1. Begriffe

Die App verwendet folgende fachliche Begriffe:

    Tracker
    Eine Vorlage, die definiert, was getrackt wird.
    Ein Tracker enthält mehrere Items.

    Item
    Eine erfassbare Einheit innerhalb eines Trackers.
    Ein Item enthält mehrere Fields.

    Field
    Ein einzelnes Eingabefeld innerhalb eines Items.
    Ein Field hat einen Typ, zum Beispiel string, int, float oder duration.

    Session
    Ein konkreter Durchlauf eines Trackers.
    Eine Session gehört immer zu genau einem Tracker.

    ItemRecord
    Die gespeicherten Werte eines Items innerhalb einer Session.

---

## 2. Grundidee

Die App dient dazu, wiederkehrende Werte schnell zu erfassen.

Der Nutzer definiert **Tracker**.

Ein Tracker beschreibt einen Tracking-Kontext, zum Beispiel:

    Training

Innerhalb eines Trackers gibt es mehrere **Items**, zum Beispiel:

    Klimmzug 1
    Klimmzug 2
    Liegestütz
    Plank

Jedes Item kann aus einem oder mehreren **Fields** bestehen.

Beispiel:

    Item: Klimmzug 1

    Fields:
    - Wiederholungen: int
    - Zusatzgewicht: float
    - Notiz: string

Für einen Tracker können mehrere **Sessions** erstellt werden.

Eine Session ist ein konkreter Durchlauf dieses Trackers.

Beispiel:

    Tracker: Training

    Session 1
    - Klimmzug 1: 10 Wiederholungen, 20 kg
    - Klimmzug 2: 8 Wiederholungen, 15 kg

    Session 2
    - Klimmzug 1: 11 Wiederholungen, 20 kg
    - Klimmzug 2: 8 Wiederholungen, 17.5 kg

---

## 3. Hauptbereiche der App

Die Startseite besteht aus mindestens zwei Bereichen, zum Beispiel als Tabs:

    [ Sessions ] [ Tracker ]

---

## 4. Tab: Sessions

Dieser Bereich zeigt alle vorhandenen Sessions über alle Tracker hinweg.

Jede Session sollte anzeigen:

    Tracker-Name
    Session-Nummer oder Datum
    Status: offen / abgeschlossen
    Fortschritt oder Anzahl gespeicherter Items
    Kurze Vorschau der gespeicherten Werte

Im Header dieses Tabs gibt es eine Aktion:

    Neue Session starten

Beim Klick darauf wird zuerst ausgewählt, für welchen **Tracker** eine neue Session gestartet werden soll.

Danach öffnet sich die Tracker-Ansicht für diese neue Session.

Ein Klick auf eine bestehende Session öffnet diese Session.

Dabei gilt:

    Offene Sessions können weiter bearbeitet werden.
    Abgeschlossene Sessions sind unveränderlich.
    Abgeschlossene Sessions dürfen geöffnet, aber nicht mehr bearbeitet werden.
    In abgeschlossenen Sessions sind Eingabefelder und Controls deaktiviert oder die Ansicht ist read-only.

---

## 5. Tab: Tracker

Dieser Bereich zeigt alle definierten Tracker.

Für jeden Tracker sollte angezeigt werden:

    Name des Trackers
    Anzahl der Items
    Anzahl der Fields
    Optional: letzte Nutzung

Ein Tracker kann bearbeitet werden.

Für einen ersten Stand reicht ein einfacher Editor, zum Beispiel:

    JSON-Editor

oder:

    Einfaches Formular

Bearbeitbar sein sollten:

    Tracker-Name
    Items
    Fields pro Item
    Field-Typ
    Default-Werte
    Increments
    Einheiten
    Option prefillFromPrevious

Bei bestehenden Sessions gilt:

    Änderungen an einem Tracker gelten ab sofort für neue oder offene Sessions.
    Abgeschlossene Sessions bleiben unveränderlich.
    Für den ersten Stand ist keine Versionierung nötig.

---

## 6. Tracker-Ansicht

Die Tracker-Ansicht ist die eigentliche Eingabemaske für eine einzelne Session eines Trackers.

Sie zeigt die Items dieses Trackers an.

Beispiel:

    Tracker: Training
    Session: offen

    [ Klimmzug 1 ]
    [ Klimmzug 2 ]
    [ Liegestütz ]
    [ Plank ]

Die Items befinden sich in einer scrollbaren Ansicht.

Da die App mobil genutzt werden soll, ist ein Aufbau sinnvoll, bei dem ein Item groß und fokussiert dargestellt wird.

Mögliche Navigation:

    Speichern & weiter
    Nur weiter
    Zurück
    Session speichern / schließen
    Übersicht

Der Nutzer soll schnell von einem Item zum nächsten springen können.

---

## 7. Item

Ein Item ist eine erfassbare Einheit innerhalb eines Trackers.

Beispiel:

    Klimmzug 1

Ein Item kann mehrere Fields enthalten.

Beispiel:

    Klimmzug 1

    Wiederholungen
    [ - ] [ 10 ] [ + ]

    Zusatzgewicht
    [ - ] [ 20.0 kg ] [ + ]

    Notiz
    [ sauberer Satz ]

---

## 8. Field-Typen

Die App unterstützt mindestens folgende Field-Typen:

---

### 8.1 string

Freitext-Eingabe.

Eigenschaften:

    type: string
    defaultValue: optional
    prefillFromPrevious: true/false

Controls:

    Keine zusätzlichen Controls notwendig

---

### 8.2 int

Ganzzahl-Eingabe.

Eigenschaften:

    type: int
    defaultValue
    increment
    unit: optional
    prefillFromPrevious: true/false

Controls:

    Minus-Button
    Input-Feld
    Plus-Button

Beim Drücken von Plus oder Minus wird der Wert verändert.

Wichtig:

    Beim Drücken der Controls soll das Input-Feld nicht fokussiert werden.
    Die mobile Tastatur soll also nicht erscheinen.

---

### 8.3 float

Kommazahl-Eingabe.

Eigenschaften:

    type: float
    defaultValue
    increment
    decimals
    unit: optional
    prefillFromPrevious: true/false

Controls:

    Minus-Button
    Input-Feld
    Plus-Button

Auch hier darf beim Drücken der Controls nicht automatisch die Tastatur geöffnet werden.

---

### 8.4 duration

Zeitdauer-Eingabe.

Eigenschaften:

    type: duration
    defaultValue: optional
    prefillFromPrevious: true/false

Controls:

    Start
    Stop
    Reset optional

Die Duration speichert eine Dauer, nicht nur einen Textwert.

Duration-Werte sollen intern als Zahl gespeichert werden, zum Beispiel als Sekunden oder Millisekunden.

Die Anzeige erfolgt formatiert, zum Beispiel:

    00:01:32

---

## 9. Vorbelegung mit letztem Wert

Es gibt kein separates previousValue-Feld im Datenmodell.

Die Vorbelegung wird ausschließlich über die Field-Option gesteuert:

    prefillFromPrevious: true/false

Wenn `prefillFromPrevious` auf `true` steht, wird beim Erstellen oder Öffnen einer offenen Session der letzte gespeicherte Wert dieses Fields verwendet.

Die Zuordnung erfolgt über:

    Tracker
    Item
    Field-Key
    letzte vorherige Session desselben Trackers

Wichtig:

    Es soll immer der letzte gespeicherte Wert verwendet werden.
    Wenn der letzte gespeicherte Wert null ist, dann wird null übernommen.
    Wenn kein vorheriger Wert existiert, wird der defaultValue verwendet.
    Wenn kein defaultValue existiert, bleibt das Field leer oder null.
    prefillFromPrevious erzeugt keinen separaten gespeicherten previousValue.
    Der übernommene Wert wird erst Teil der aktuellen Session, wenn der Nutzer das Item beziehungsweise die Session speichert.

Wenn `prefillFromPrevious` auf `false` steht, wird der defaultValue verwendet.

---

## 10. Sessions

Eine Session gehört immer zu genau einem Tracker.

Eine Session hat mindestens:

    id
    trackerId
    createdAt
    updatedAt
    status

Mögliche Status:

    open
    completed

Eine Session startet im Status:

    open

Eine Session wird abgeschlossen, sobald der Nutzer sie über eine Speichern-/Schließen-Aktion beendet.

Beispiel für eine Aktion:

    Session speichern / schließen

oder am Ende der Eingabe:

    Speichern & Session abschließen

Sobald eine Session abgeschlossen ist:

    status = completed
    Die Session ist unveränderlich.
    Werte dürfen nicht mehr geändert werden.
    Die Session darf weiterhin geöffnet und angesehen werden.
    Die Session darf nicht mehr in den Bearbeitungsmodus wechseln.

Eine Session gilt nicht automatisch nur deshalb als abgeschlossen, weil alle Items einmal gespeichert wurden.

Der Abschluss erfolgt explizit durch die Speichern-/Schließen-Aktion des Nutzers.

---

## 11. ItemRecords

Ein ItemRecord ist ein gespeicherter Wert für ein Item innerhalb einer Session.

Ein ItemRecord gehört zu:

    sessionId
    trackerId
    itemId

Ein ItemRecord enthält die Werte aller Fields dieses Items.

Beispiel:

    sessionId: session-003
    trackerId: training
    itemId: pullup-1

    values:
      reps: 10
      weight: 20.0
      note: "sauber"

Für ein Item sollte es innerhalb einer Session maximal einen aktuellen ItemRecord geben.

Wenn eine offene Session bearbeitet wird, wird der bestehende ItemRecord aktualisiert.

Wenn eine Session abgeschlossen ist, dürfen ItemRecords nicht mehr verändert werden.

---

## 12. Datenmodell

Das Datenmodell soll mindestens folgende Strukturen abbilden:

    Tracker
    - id
    - name
    - description optional
    - items
    - createdAt
    - updatedAt

    Item
    - id
    - trackerId
    - title
    - order
    - fields

    FieldDefinition
    - id
    - itemId
    - key
    - label
    - type: string | int | float | duration
    - order
    - defaultValue
    - increment
    - decimals
    - unit
    - required optional
    - prefillFromPrevious

    Session
    - id
    - trackerId
    - createdAt
    - updatedAt
    - status: open | completed

    ItemRecord
    - id
    - sessionId
    - trackerId
    - itemId
    - values
    - createdAt
    - updatedAt

Die Persistenz soll passend zum angegebenen Technologie-Stack umgesetzt werden.

Für einen ersten Stand darf eine einfache lokale Persistenz verwendet werden, sofern die Datenstruktur später sauber erweitert werden kann.

---

## 13. App-Flows

---

### 13.1 Flow: Startseite öffnen

    App startet
    → Tab "Sessions" wird angezeigt
    → Alle vorhandenen Sessions werden geladen
    → Sessions werden sinnvoll sortiert, zum Beispiel neueste zuerst

---

### 13.2 Flow: Neue Session starten

    Nutzer klickt "Neue Session starten"
    → Tracker-Auswahl öffnet sich
    → Nutzer wählt einen Tracker
    → Neue Session wird mit status = open erstellt
    → Tracker-Ansicht öffnet sich
    → Erstes Item des Trackers wird angezeigt
    → Fields werden anhand von prefillFromPrevious oder defaultValue initialisiert

---

### 13.3 Flow: Offene Session öffnen

    Nutzer klickt auf eine offene Session
    → Passende Session wird geladen
    → Passender Tracker wird geladen
    → Gespeicherte ItemRecords werden geladen
    → Tracker-Ansicht öffnet sich im Bearbeitungsmodus

Sinnvolle Startposition kann sein:

    erstes noch nicht gespeichertes Item

oder:

    erstes Item

---

### 13.4 Flow: Abgeschlossene Session öffnen

    Nutzer klickt auf eine abgeschlossene Session
    → Passende Session wird geladen
    → Passender Tracker wird geladen
    → Gespeicherte ItemRecords werden geladen
    → Tracker-Ansicht öffnet sich im read-only Modus
    → Eingabefelder und Controls sind deaktiviert
    → Es gibt keine Speichern-Aktion

---

### 13.5 Flow: Item speichern

    Nutzer gibt Werte ein
    → Nutzer klickt "Speichern & weiter"
    → ItemRecord für aktuelles Item wird gespeichert oder aktualisiert
    → Fortschritt der offenen Session wird aktualisiert
    → Nächstes Item wird angezeigt

Wenn das letzte Item erreicht ist:

    → Nutzer kann die Session explizit speichern / schließen
    → Erst dann wird die Session abgeschlossen

---

### 13.6 Flow: Session abschließen

    Nutzer klickt "Session speichern / schließen"
    → Alle aktuell gespeicherten ItemRecords bleiben erhalten
    → Session wird auf status = completed gesetzt
    → Session wird unveränderlich
    → Nutzer wird zur Sessions-Übersicht zurückgeführt oder sieht die Session im read-only Modus

---

### 13.7 Flow: Tracker bearbeiten

    Nutzer öffnet Tab "Tracker"
    → Nutzer wählt einen Tracker
    → Editor öffnet sich
    → Nutzer ändert Items oder Fields
    → Änderungen werden gespeichert

Bei bestehenden Sessions gilt:

    Offene Sessions verwenden die aktuellen Tracker-Definitionen.
    Abgeschlossene Sessions bleiben unveränderlich.
    Für den ersten Stand ist keine Versionierung nötig.

---

## 14. Wichtige UI-Struktur

Die App besteht grob aus diesen Screens:

    HomeScreen
    ├── SessionsTab
    │   ├── Header mit "Neue Session starten"
    │   └── SessionList
    │
    ├── TrackersTab
    │   ├── TrackerList
    │   └── TrackerEditor
    │
    └── TrackerSessionScreen
        ├── Header mit Tracker + Session
        ├── Scrollbare Items
        ├── FieldControls
        └── Navigation / Session abschließen

---

## 15. Wichtige Verhaltensregeln

Die wichtigsten Regeln für die Umsetzung:

    Eine Session gehört zu genau einem Tracker.

    Ein Tracker besteht aus mehreren Items.

    Ein Item besteht aus mehreren Fields.

    Fields werden über prefillFromPrevious optional mit dem letzten gespeicherten Wert vorbelegt.

    Es gibt kein separates previousValue-Feld.

    Wenn der letzte gespeicherte Wert null ist, wird null übernommen.

    Controls für int, float und duration dürfen nicht ungewollt die mobile Tastatur öffnen.

    Offene Sessions können bearbeitet werden.

    Abgeschlossene Sessions sind unveränderlich.

    Eine Session wird nur durch eine explizite Speichern-/Schließen-Aktion abgeschlossen.

    Neue Sessions werden über die Sessions-Übersicht gestartet.

    Beim Starten einer neuen Session muss ein Tracker ausgewählt werden.

    Tracker können auf einer eigenen Übersichtsseite angezeigt und bearbeitet werden.

    Zusätzliche Funktionen wie Import, Export, Sync, Statistiken oder Duplizieren sollen zunächst nicht umgesetzt werden.

---

## 16. Architektur

Die App sollte sauber strukturiert sein.

Empfohlene logische Trennung:

    UI
    → zeigt Screens und Controls

    State Layer
    → hält aktuelle Session, aktuellen Tracker und aktuelle Eingaben

    Domain Logic
    → berechnet Vorbelegungen, Fortschritt und Status

    Storage Layer
    → liest und schreibt Tracker, Sessions und ItemRecords

    Platform Layer
    → enthält plattformspezifische Funktionen des gewählten Technologie-Stacks

---

## 17. Erwartetes Ergebnis

Erstelle eine lauffähige App mit:

    Mobile-first Layout
    Startseite mit Tabs "Sessions" und "Tracker"
    Übersicht aller Sessions
    Start einer neuen Session mit Tracker-Auswahl
    Tracker-Ansicht für eine Session
    Items mit mehreren Fields
    Field-Typen string, int, float und duration
    Vorbelegung über prefillFromPrevious
    Kein separates previousValue-Feld
    Bearbeitbare Tracker
    Offene Sessions bearbeitbar
    Abgeschlossene Sessions read-only und unveränderlich
    Explizites Abschließen einer Session
    Persistenz passend zum angegebenen Technologie-Stack

Für einen ersten Stand darf die Persistenz einfach sein.

Die Struktur soll aber so geplant sein, dass sie später sauber erweitert werden kann.

---

## 18. Umsetzung aus dieser Sitzung

### 18.1 Sessions-Übersicht

Anforderung:

    Die Session-Liste soll deutlich schöner gestaltet sein.

Umsetzung:

    Die Sessions-Übersicht wurde als Karten-Layout mit Header, CTA-Button, Status-Chips, Fortschrittsanzeige und Vorschau aufgebaut.
    Leerzustände wurden ergänzt.
    Die Designsprache wurde an das restliche UI angepasst.

### 18.2 Tracker-Übersicht

Anforderung:

    Die Tracker-Übersicht soll optisch an die Sessions-Übersicht angeglichen werden.

Umsetzung:

    Die Tracker-Liste nutzt jetzt denselben visuellen Aufbau wie die Session-Liste.
    Jeder Tracker wird als Karte mit Name, Beschreibung, Item-/Field-Zähler und Chips angezeigt.
    Ein leerer Zustand und ein prominenter Neuer-Tracker-Button sind vorhanden.

### 18.3 Tracker-Erstellung

Anforderung:

    Neue Tracker sollen nicht mehr nur über einen JSON-Texteditor angelegt werden.

Umsetzung:

    Der Tracker-Editor wurde zu einem strukturierten Formular umgebaut.
    Tracker-Name, Beschreibung, Items und Fields werden separat bearbeitet.
    Der Tracker-Editor startet für neue Tracker leer und zeigt einen erklärenden Hinweis statt eines vorbefüllten Beispiels.

### 18.4 Field-Typen und Eingabe

Anforderung:

    Ich soll zwischen string, integer, decimal und timer wählen können.

Umsetzung:

    Der Field-Typ wird jetzt über sichtbare Auswahloptionen gewählt.
    `String`, `Integer`, `Decimal` und `Timer` sind direkt auswählbar.
    Die UI blendet irrelevante Optionen abhängig vom Typ aus:

    - `String`: keine Increment-/Decimals-Felder
    - `Timer`: keine Increment-/Decimals-Felder
    - `Integer`: nur Increment
    - `Decimal`: Increment und Decimals

### 18.5 Reihenfolge ohne Eingabefeld

Anforderung:

    Das Reihenfolge-Feld soll verschwinden und per Drag-and-Drop ersetzt werden.

Umsetzung:

    Das sichtbare Reihenfolge-Feld wurde entfernt.
    Items und Fields können über kompakte Drag-Handles verschoben werden.
    Die Reihenfolge wird beim Speichern aus der aktuellen UI-Position abgeleitet.
    Eine Scroll-Intercept-Korrektur sorgt dafür, dass Drag-Gesten nicht nur scrollen.

### 18.6 Buttons und visuelles System

Anforderung:

    Alle Buttons sollen konsistent gestaltet sein, inklusive Primary-Buttons.

Umsetzung:

    Es gibt jetzt zentrale Button-Stile für `primary`, `secondary`, `ghost`, `danger` und Tabs.
    Primäre Aktionen sind visuell hervorgehoben, destruktive Aktionen sind klar erkennbar.

### 18.7 Session-Ansicht

Anforderung:

    Alle Items einer Session sollen in einer Ansicht sichtbar sein.

Umsetzung:

    Die Session-Ansicht zeigt alle Items gemeinsam in einer scrollbaren View.
    Unten befindet sich ein Footer mit Zurück und Session speichern / schließen.
    Offene Sessions werden beim Verlassen gespeichert, abgeschlossene Sessions sind read-only.

### 18.8 F-Droid-Kompatibilität

Anforderung:

    Keine Google-Dienste, da die App später über F-Droid veröffentlicht werden soll.

Umsetzung:

    Es werden keine Google Play Services, Firebase oder ähnliche proprietäre SDKs verwendet.
    Die Lösung basiert auf AndroidX und lokaler SQLite-Persistenz.

### 18.9 Build / Packaging

Anforderung:

    Die App soll lokal gebaut und als APK ausgegeben werden können.

Umsetzung:

    Der Gradle Wrapper wurde hinzugefügt.
    Android SDK und Build-Tools wurden lokal vorbereitet.
    Ein Debug-APK-Build ist reproduzierbar und erzeugt `app/build/outputs/apk/debug/app-debug.apk`.

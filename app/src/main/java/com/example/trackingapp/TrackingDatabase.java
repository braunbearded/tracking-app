package com.example.trackingapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

final class TrackingDatabase extends SQLiteOpenHelper {
    static final Object NO_PREVIOUS = new Object();

    TrackingDatabase(Context context) {
        super(context, "tracking.sqlite", null, 1);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE trackers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,description TEXT,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,trackerId INTEGER NOT NULL,title TEXT NOT NULL,sortOrder INTEGER NOT NULL,FOREIGN KEY(trackerId) REFERENCES trackers(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE fields(id INTEGER PRIMARY KEY AUTOINCREMENT,itemId INTEGER NOT NULL,fieldKey TEXT NOT NULL,label TEXT NOT NULL,type TEXT NOT NULL,sortOrder INTEGER NOT NULL,defaultValue TEXT,incrementValue REAL,decimals INTEGER,unit TEXT,required INTEGER NOT NULL,prefillFromPrevious INTEGER NOT NULL,FOREIGN KEY(itemId) REFERENCES items(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,trackerId INTEGER NOT NULL,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL,status TEXT NOT NULL CHECK(status IN ('open','completed')),FOREIGN KEY(trackerId) REFERENCES trackers(id))");
        db.execSQL("CREATE TABLE item_records(id INTEGER PRIMARY KEY AUTOINCREMENT,sessionId INTEGER NOT NULL,trackerId INTEGER NOT NULL,itemId INTEGER NOT NULL,valuesJson TEXT NOT NULL,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL,UNIQUE(sessionId,itemId),FOREIGN KEY(sessionId) REFERENCES sessions(id),FOREIGN KEY(itemId) REFERENCES items(id))");
        seed(db);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    private long now() {
        return System.currentTimeMillis();
    }

    private void seed(SQLiteDatabase db) {
        long trackerId = insertTracker(db, "Training", "Beispiel-Tracker");
        long pullUpItemId = insertItem(db, trackerId, "Klimmzug 1", 0);
        field(db, pullUpItemId, "reps", "Wiederholungen", "int", 0, "8", 1, 0, "", true);
        field(db, pullUpItemId, "weight", "Zusatzgewicht", "float", 1, "0", 2.5, 1, "kg", true);
        field(db, pullUpItemId, "note", "Notiz", "string", 2, "", 1, 0, "", false);

        long plankItemId = insertItem(db, trackerId, "Plank", 1);
        field(db, plankItemId, "duration", "Dauer", "duration", 0, "60000", 1, 0, "", true);
        field(db, plankItemId, "note", "Notiz", "string", 1, "", 1, 0, "", false);
    }

    long insertTracker(SQLiteDatabase db, String name, String desc) {
        ContentValues values = new ContentValues();
        long now = now();
        values.put("name", name);
        values.put("description", desc);
        values.put("createdAt", now);
        values.put("updatedAt", now);
        return db.insert("trackers", null, values);
    }

    long insertItem(SQLiteDatabase db, long trackerId, String title, int order) {
        ContentValues values = new ContentValues();
        values.put("trackerId", trackerId);
        values.put("title", title);
        values.put("sortOrder", order);
        return db.insert("items", null, values);
    }

    void field(
            SQLiteDatabase db,
            long itemId,
            String key,
            String label,
            String type,
            int order,
            String def,
            double inc,
            int dec,
            String unit,
            boolean prefillFromPrevious) {
        ContentValues values = new ContentValues();
        values.put("itemId", itemId);
        values.put("fieldKey", key);
        values.put("label", label);
        values.put("type", type);
        values.put("sortOrder", order);
        values.put("defaultValue", def);
        values.put("incrementValue", inc);
        values.put("decimals", dec);
        values.put("unit", unit);
        values.put("required", 0);
        values.put("prefillFromPrevious", prefillFromPrevious ? 1 : 0);
        db.insert("fields", null, values);
    }

    List<Tracker> trackers() {
        SQLiteDatabase db = getReadableDatabase();
        List<Tracker> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT id FROM trackers ORDER BY updatedAt DESC", null);
        try {
            while (cursor.moveToNext()) {
                list.add(readTracker(db, cursor.getLong(0)));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    Tracker readTracker(long id) {
        return readTracker(getReadableDatabase(), id);
    }

    Tracker readTracker(SQLiteDatabase db, long id) {
        Cursor trackerCursor = db.rawQuery(
                "SELECT id,name,description,createdAt,updatedAt FROM trackers WHERE id=?",
                new String[]{String.valueOf(id)});
        try {
            if (!trackerCursor.moveToFirst()) {
                return null;
            }

            Tracker tracker = new Tracker();
            tracker.id = trackerCursor.getLong(0);
            tracker.name = trackerCursor.getString(1);
            tracker.description = trackerCursor.getString(2);
            tracker.createdAt = trackerCursor.getLong(3);
            tracker.updatedAt = trackerCursor.getLong(4);

            Cursor itemCursor = db.rawQuery(
                    "SELECT id,title,sortOrder FROM items WHERE trackerId=? ORDER BY sortOrder,id",
                    new String[]{String.valueOf(id)});
            try {
                while (itemCursor.moveToNext()) {
                    Item item = new Item();
                    item.id = itemCursor.getLong(0);
                    item.trackerId = id;
                    item.title = itemCursor.getString(1);
                    item.order = itemCursor.getInt(2);

                    Cursor fieldCursor = db.rawQuery(
                            "SELECT id,fieldKey,label,type,sortOrder,defaultValue,incrementValue,decimals,unit,required,prefillFromPrevious FROM fields WHERE itemId=? ORDER BY sortOrder,id",
                            new String[]{String.valueOf(item.id)});
                    try {
                        while (fieldCursor.moveToNext()) {
                            FieldDefinition definition = new FieldDefinition();
                            definition.id = fieldCursor.getLong(0);
                            definition.itemId = item.id;
                            definition.key = fieldCursor.getString(1);
                            definition.label = fieldCursor.getString(2);
                            definition.type = fieldCursor.getString(3);
                            definition.order = fieldCursor.getInt(4);
                            definition.defaultValue = fieldCursor.getString(5);
                            definition.increment = fieldCursor.getDouble(6);
                            definition.decimals = fieldCursor.getInt(7);
                            definition.unit = fieldCursor.getString(8);
                            definition.required = fieldCursor.getInt(9) == 1;
                            definition.prefillFromPrevious = fieldCursor.getInt(10) == 1;
                            item.fields.add(definition);
                        }
                    } finally {
                        fieldCursor.close();
                    }

                    tracker.items.add(item);
                }
            } finally {
                itemCursor.close();
            }

            return tracker;
        } finally {
            trackerCursor.close();
        }
    }

    List<Session> sessions() {
        List<Session> sessions = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,trackerId,createdAt,updatedAt,status FROM sessions ORDER BY createdAt DESC",
                null);
        try {
            while (cursor.moveToNext()) {
                Session session = new Session();
                session.id = cursor.getLong(0);
                session.trackerId = cursor.getLong(1);
                session.createdAt = cursor.getLong(2);
                session.updatedAt = cursor.getLong(3);
                session.status = cursor.getString(4);
                sessions.add(session);
            }
        } finally {
            cursor.close();
        }
        return sessions;
    }

    Session session(long id) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,trackerId,createdAt,updatedAt,status FROM sessions WHERE id=?",
                new String[]{String.valueOf(id)});
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            Session session = new Session();
            session.id = cursor.getLong(0);
            session.trackerId = cursor.getLong(1);
            session.createdAt = cursor.getLong(2);
            session.updatedAt = cursor.getLong(3);
            session.status = cursor.getString(4);
            return session;
        } finally {
            cursor.close();
        }
    }

    long createSession(long trackerId) {
        ContentValues values = new ContentValues();
        long now = now();
        values.put("trackerId", trackerId);
        values.put("createdAt", now);
        values.put("updatedAt", now);
        values.put("status", "open");
        return getWritableDatabase().insert("sessions", null, values);
    }

    void complete(long sessionId) {
        ContentValues values = new ContentValues();
        values.put("status", "completed");
        values.put("updatedAt", now());
        getWritableDatabase().update("sessions", values, "id=?", new String[]{String.valueOf(sessionId)});
    }

    Map<Long, ItemRecord> records(long sessionId) {
        Map<Long, ItemRecord> records = new HashMap<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,sessionId,trackerId,itemId,valuesJson,createdAt,updatedAt FROM item_records WHERE sessionId=?",
                new String[]{String.valueOf(sessionId)});
        try {
            while (cursor.moveToNext()) {
                ItemRecord record = new ItemRecord();
                record.id = cursor.getLong(0);
                record.sessionId = cursor.getLong(1);
                record.trackerId = cursor.getLong(2);
                record.itemId = cursor.getLong(3);
                record.valuesJson = cursor.getString(4);
                record.createdAt = cursor.getLong(5);
                record.updatedAt = cursor.getLong(6);
                records.put(record.itemId, record);
            }
        } finally {
            cursor.close();
        }
        return records;
    }

    void saveRecord(Session session, long itemId, Map<String, Object> values) {
        if (!"open".equals(session.status)) {
            return;
        }

        long now = now();
        ContentValues valuesToSave = new ContentValues();
        valuesToSave.put("sessionId", session.id);
        valuesToSave.put("trackerId", session.trackerId);
        valuesToSave.put("itemId", itemId);
        valuesToSave.put("valuesJson", JsonUtil.stringify(values));
        valuesToSave.put("updatedAt", now);
        valuesToSave.put("createdAt", now);
        getWritableDatabase().insertWithOnConflict(
                "item_records",
                null,
                valuesToSave,
                SQLiteDatabase.CONFLICT_REPLACE);

        ContentValues sessionValues = new ContentValues();
        sessionValues.put("updatedAt", now);
        getWritableDatabase().update("sessions", sessionValues, "id=?", new String[]{String.valueOf(session.id)});
    }

    Object previousValue(long trackerId, long beforeSessionId, long beforeSessionCreatedAt, long itemId, String key) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT r.valuesJson FROM item_records r JOIN sessions s ON s.id=r.sessionId WHERE r.trackerId=? AND r.itemId=? AND s.createdAt<? AND s.id<>? ORDER BY s.createdAt DESC LIMIT 1",
                new String[]{
                        String.valueOf(trackerId),
                        String.valueOf(itemId),
                        String.valueOf(beforeSessionCreatedAt),
                        String.valueOf(beforeSessionId)
                });
        try {
            if (!cursor.moveToFirst()) {
                return NO_PREVIOUS;
            }

            JSONObject object = new JSONObject(cursor.getString(0));
            if (!object.has(key)) {
                return NO_PREVIOUS;
            }
            return object.isNull(key) ? null : object.get(key);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        } finally {
            cursor.close();
        }
    }

    int recordCount(long sessionId) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM item_records WHERE sessionId=?",
                new String[]{String.valueOf(sessionId)});
        try {
            cursor.moveToFirst();
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
    }
}

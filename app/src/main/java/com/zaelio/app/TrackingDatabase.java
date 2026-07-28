package com.zaelio.app;

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
        super(context, "tracking.sqlite", null, 5);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE trackers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,description TEXT,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE fields(id INTEGER PRIMARY KEY AUTOINCREMENT,trackerId INTEGER NOT NULL,itemTitle TEXT,itemOrder INTEGER NOT NULL DEFAULT 0,fieldKey TEXT NOT NULL,label TEXT NOT NULL,type TEXT NOT NULL,sortOrder INTEGER NOT NULL,defaultValue TEXT,incrementValue REAL,unit TEXT,inputSize TEXT NOT NULL DEFAULT 'standard',required INTEGER NOT NULL,prefillFromPrevious INTEGER NOT NULL,FOREIGN KEY(trackerId) REFERENCES trackers(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,trackerId INTEGER NOT NULL,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL,FOREIGN KEY(trackerId) REFERENCES trackers(id))");
        db.execSQL("CREATE TABLE field_records(id INTEGER PRIMARY KEY AUTOINCREMENT,sessionId INTEGER NOT NULL,trackerId INTEGER NOT NULL,fieldId INTEGER NOT NULL,fieldKey TEXT NOT NULL,valuesJson TEXT NOT NULL,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL,UNIQUE(sessionId,fieldId),FOREIGN KEY(sessionId) REFERENCES sessions(id),FOREIGN KEY(fieldId) REFERENCES fields(id))");
        seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            migrateSessionsTable(db);
        }
        if (oldVersion < 3) {
            migrateToFieldsOnly(db);
        }
        if (oldVersion < 4) {
            migrateFieldItemColumns(db);
        }
        if (oldVersion < 5) {
            migrateFieldInputSize(db);
        }
    }

    private void migrateFieldItemColumns(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE fields ADD COLUMN itemTitle TEXT");
        db.execSQL("ALTER TABLE fields ADD COLUMN itemOrder INTEGER NOT NULL DEFAULT 0");
        db.execSQL("UPDATE fields SET itemTitle=label,itemOrder=sortOrder WHERE itemTitle IS NULL");
    }

    private void migrateFieldInputSize(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE fields ADD COLUMN inputSize TEXT NOT NULL DEFAULT 'standard'");
    }

    private void migrateSessionsTable(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(false);
        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE sessions_new(id INTEGER PRIMARY KEY AUTOINCREMENT,trackerId INTEGER NOT NULL,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL,FOREIGN KEY(trackerId) REFERENCES trackers(id))");
            db.execSQL("INSERT INTO sessions_new(id,trackerId,createdAt,updatedAt) SELECT id,trackerId,createdAt,updatedAt FROM sessions");
            db.execSQL("DROP TABLE sessions");
            db.execSQL("ALTER TABLE sessions_new RENAME TO sessions");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.setForeignKeyConstraintsEnabled(true);
        }
    }

    private void migrateToFieldsOnly(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(false);
        db.beginTransaction();
        try {
            Map<Long, List<OldField>> fieldsByItemId = new HashMap<>();
            Cursor oldFields = db.rawQuery(
                    "SELECT f.id,f.itemId,f.fieldKey,f.label,f.type,f.sortOrder,f.defaultValue,f.incrementValue,f.unit,f.required,f.prefillFromPrevious,i.trackerId FROM fields f JOIN items i ON i.id=f.itemId ORDER BY i.trackerId,f.sortOrder,f.id",
                    null);
            try {
                while (oldFields.moveToNext()) {
                    OldField field = new OldField();
                    field.id = oldFields.getLong(0);
                    field.itemId = oldFields.getLong(1);
                    field.key = oldFields.getString(2);
                    field.label = oldFields.getString(3);
                    field.type = oldFields.getString(4);
                    field.order = oldFields.getInt(5);
                    field.defaultValue = oldFields.getString(6);
                    field.increment = oldFields.getDouble(7);
                    field.unit = oldFields.getString(8);
                    field.required = oldFields.getInt(9) == 1;
                    field.prefillFromPrevious = oldFields.getInt(10) == 1;
                    field.trackerId = oldFields.getLong(11);
                    List<OldField> fields = fieldsByItemId.get(field.itemId);
                    if (fields == null) {
                        fields = new ArrayList<>();
                        fieldsByItemId.put(field.itemId, fields);
                    }
                    fields.add(field);
                }
            } finally {
                oldFields.close();
            }

            db.execSQL("CREATE TABLE fields_new(id INTEGER PRIMARY KEY AUTOINCREMENT,trackerId INTEGER NOT NULL,fieldKey TEXT NOT NULL,label TEXT NOT NULL,type TEXT NOT NULL,sortOrder INTEGER NOT NULL,defaultValue TEXT,incrementValue REAL,unit TEXT,required INTEGER NOT NULL,prefillFromPrevious INTEGER NOT NULL,FOREIGN KEY(trackerId) REFERENCES trackers(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE field_records_new(id INTEGER PRIMARY KEY AUTOINCREMENT,sessionId INTEGER NOT NULL,trackerId INTEGER NOT NULL,fieldId INTEGER NOT NULL,fieldKey TEXT NOT NULL,valuesJson TEXT NOT NULL,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL,UNIQUE(sessionId,fieldId),FOREIGN KEY(sessionId) REFERENCES sessions(id),FOREIGN KEY(fieldId) REFERENCES fields_new(id))");

            Cursor fieldCursor = db.rawQuery(
                    "SELECT f.id,f.itemId,f.fieldKey,f.label,f.type,f.sortOrder,f.defaultValue,f.incrementValue,f.unit,f.required,f.prefillFromPrevious,i.trackerId FROM fields f JOIN items i ON i.id=f.itemId ORDER BY i.trackerId,f.sortOrder,f.id",
                    null);
            try {
                while (fieldCursor.moveToNext()) {
                    ContentValues values = new ContentValues();
                    values.put("id", fieldCursor.getLong(0));
                    values.put("trackerId", fieldCursor.getLong(12));
                    values.put("fieldKey", fieldCursor.getString(2));
                    values.put("label", fieldCursor.getString(3));
                    values.put("type", fieldCursor.getString(4));
                    values.put("sortOrder", fieldCursor.getInt(5));
                    values.put("defaultValue", fieldCursor.getString(6));
                    values.put("incrementValue", fieldCursor.getDouble(7));
                    values.put("unit", fieldCursor.getString(8));
                    values.put("required", fieldCursor.getInt(9));
                    values.put("prefillFromPrevious", fieldCursor.getInt(10));
                    db.insert("fields_new", null, values);
                }
            } finally {
                fieldCursor.close();
            }

            Cursor recordCursor = db.rawQuery(
                    "SELECT id,sessionId,trackerId,itemId,valuesJson,createdAt,updatedAt FROM item_records",
                    null);
            try {
                while (recordCursor.moveToNext()) {
                    long sessionId = recordCursor.getLong(1);
                    long trackerId = recordCursor.getLong(2);
                    long itemId = recordCursor.getLong(3);
                    String valuesJson = recordCursor.getString(4);
                    long createdAt = recordCursor.getLong(5);
                    long updatedAt = recordCursor.getLong(6);

                    List<OldField> fields = fieldsByItemId.get(itemId);
                    if (fields == null) {
                        continue;
                    }

                    JSONObject object = new JSONObject(valuesJson);
                    for (OldField field : fields) {
                        Map<String, Object> singleValue = new HashMap<>();
                        singleValue.put(field.key, object.has(field.key) && !object.isNull(field.key) ? object.get(field.key) : null);

                        ContentValues values = new ContentValues();
                        values.put("sessionId", sessionId);
                        values.put("trackerId", trackerId);
                        values.put("fieldId", field.id);
                        values.put("fieldKey", field.key);
                        values.put("valuesJson", JsonUtil.stringify(singleValue));
                        values.put("createdAt", createdAt);
                        values.put("updatedAt", updatedAt);
                        db.insert("field_records_new", null, values);
                    }
                }
            } finally {
                recordCursor.close();
            }

            db.execSQL("DROP TABLE item_records");
            db.execSQL("DROP TABLE fields");
            db.execSQL("DROP TABLE items");
            db.execSQL("ALTER TABLE fields_new RENAME TO fields");
            db.execSQL("ALTER TABLE field_records_new RENAME TO field_records");
            db.setTransactionSuccessful();
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        } finally {
            db.endTransaction();
            db.setForeignKeyConstraintsEnabled(true);
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private void seed(SQLiteDatabase db) {
        long trackerId = insertTracker(db, "Training", "Beispiel-Tracker");
        field(db, trackerId, "reps", "Wiederholungen", "int", 0, "8", 1, "", true);
        field(db, trackerId, "weight", "Zusatzgewicht", "float", 1, "0", 2.5, "kg", true);
        field(db, trackerId, "note", "Notiz", "string", 2, "", 1, "", false);
        field(db, trackerId, "duration", "Dauer", "duration", 3, "60000", 1, "", true);
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

    long insertField(
            SQLiteDatabase db,
            long trackerId,
            String key,
            String label,
            String type,
            int order,
            String def,
            double inc,
            String unit,
            boolean prefillFromPrevious) {
        ContentValues values = new ContentValues();
        values.put("trackerId", trackerId);
        values.put("fieldKey", key);
        values.put("label", label);
        values.put("type", type);
        values.put("sortOrder", order);
        values.put("defaultValue", def);
        values.put("incrementValue", inc);
        values.put("unit", unit);
        values.put("inputSize", "standard");
        values.put("required", 0);
        values.put("prefillFromPrevious", prefillFromPrevious ? 1 : 0);
        return db.insert("fields", null, values);
    }

    void field(
            SQLiteDatabase db,
            long trackerId,
            String key,
            String label,
            String type,
            int order,
            String def,
            double inc,
            String unit,
            boolean prefillFromPrevious) {
        insertField(db, trackerId, key, label, type, order, def, inc, unit, prefillFromPrevious);
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

            Cursor fieldCursor = db.rawQuery(
                    "SELECT id,trackerId,fieldKey,label,type,sortOrder,defaultValue,incrementValue,unit,required,prefillFromPrevious,itemTitle,itemOrder,inputSize FROM fields WHERE trackerId=? ORDER BY itemOrder,sortOrder,id",
                    new String[]{String.valueOf(id)});
            try {
                while (fieldCursor.moveToNext()) {
                    FieldDefinition definition = new FieldDefinition();
                    definition.id = fieldCursor.getLong(0);
                    definition.trackerId = fieldCursor.getLong(1);
                    definition.key = fieldCursor.getString(2);
                    definition.label = fieldCursor.getString(3);
                    definition.type = fieldCursor.getString(4);
                    definition.order = fieldCursor.getInt(5);
                    definition.defaultValue = fieldCursor.getString(6);
                    definition.increment = fieldCursor.getDouble(7);
                    definition.unit = fieldCursor.getString(8);
                    definition.required = fieldCursor.getInt(9) == 1;
                    definition.prefillFromPrevious = fieldCursor.getInt(10) == 1;
                    definition.inputSize = fieldCursor.getString(13);
                    tracker.fields.add(definition);

                    String itemTitle = fieldCursor.getString(11);
                    int itemOrder = fieldCursor.getInt(12);
                    Item item = findItem(tracker.items, itemOrder, itemTitle);
                    if (item == null) {
                        item = new Item();
                        item.id = tracker.items.size() + 1;
                        item.trackerId = definition.trackerId;
                        item.title = itemTitle == null || itemTitle.trim().isEmpty() ? definition.label : itemTitle;
                        item.order = itemOrder;
                        tracker.items.add(item);
                    }
                    item.fields.add(definition);
                }
            } finally {
                fieldCursor.close();
            }

            return tracker;
        } finally {
            trackerCursor.close();
        }
    }

    private Item findItem(List<Item> items, int order, String title) {
        String normalizedTitle = title == null ? "" : title;
        for (Item item : items) {
            String itemTitle = item.title == null ? "" : item.title;
            if (item.order == order && itemTitle.equals(normalizedTitle)) {
                return item;
            }
        }
        return null;
    }

    List<Session> sessions() {
        List<Session> sessions = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,trackerId,createdAt,updatedAt FROM sessions ORDER BY createdAt DESC",
                null);
        try {
            while (cursor.moveToNext()) {
                Session session = new Session();
                session.id = cursor.getLong(0);
                session.trackerId = cursor.getLong(1);
                session.createdAt = cursor.getLong(2);
                session.updatedAt = cursor.getLong(3);
                sessions.add(session);
            }
        } finally {
            cursor.close();
        }
        return sessions;
    }

    Session session(long id) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,trackerId,createdAt,updatedAt FROM sessions WHERE id=?",
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
        return getWritableDatabase().insert("sessions", null, values);
    }

    void deleteSession(long sessionId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("field_records", "sessionId=?", new String[]{String.valueOf(sessionId)});
            db.delete("sessions", "id=?", new String[]{String.valueOf(sessionId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void deleteTracker(long trackerId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("field_records", "trackerId=?", new String[]{String.valueOf(trackerId)});
            db.delete("sessions", "trackerId=?", new String[]{String.valueOf(trackerId)});
            db.delete("fields", "trackerId=?", new String[]{String.valueOf(trackerId)});
            db.delete("trackers", "id=?", new String[]{String.valueOf(trackerId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    Map<Long, ItemRecord> records(long sessionId) {
        Map<Long, ItemRecord> records = new HashMap<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,sessionId,trackerId,fieldId,valuesJson,createdAt,updatedAt FROM field_records WHERE sessionId=?",
                new String[]{String.valueOf(sessionId)});
        try {
            while (cursor.moveToNext()) {
                ItemRecord record = new ItemRecord();
                record.id = cursor.getLong(0);
                record.sessionId = cursor.getLong(1);
                record.trackerId = cursor.getLong(2);
                record.fieldId = cursor.getLong(3);
                record.valuesJson = cursor.getString(4);
                record.createdAt = cursor.getLong(5);
                record.updatedAt = cursor.getLong(6);
                records.put(record.fieldId, record);
            }
        } finally {
            cursor.close();
        }
        return records;
    }

    void saveRecord(Session session, long fieldId, Map<String, Object> values) {
        long now = now();
        ContentValues valuesToSave = new ContentValues();
        valuesToSave.put("sessionId", session.id);
        valuesToSave.put("trackerId", session.trackerId);
        valuesToSave.put("fieldId", fieldId);
        valuesToSave.put("fieldKey", values.isEmpty() ? "" : values.keySet().iterator().next());
        valuesToSave.put("valuesJson", JsonUtil.stringify(values));
        valuesToSave.put("updatedAt", now);
        valuesToSave.put("createdAt", now);
        getWritableDatabase().insertWithOnConflict(
                "field_records",
                null,
                valuesToSave,
                SQLiteDatabase.CONFLICT_REPLACE);

        ContentValues sessionValues = new ContentValues();
        sessionValues.put("updatedAt", now);
        getWritableDatabase().update("sessions", sessionValues, "id=?", new String[]{String.valueOf(session.id)});
    }

    Object previousValue(long trackerId, long fieldId, String key) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT valuesJson FROM field_records WHERE trackerId=? AND fieldId=? ORDER BY updatedAt DESC LIMIT 1",
                new String[]{String.valueOf(trackerId), String.valueOf(fieldId)});
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
                "SELECT COUNT(*) FROM field_records WHERE sessionId=?",
                new String[]{String.valueOf(sessionId)});
        try {
            cursor.moveToFirst();
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
    }

    private static final class OldField {
        long id;
        long itemId;
        long trackerId;
        String key;
        String label;
        String type;
        String defaultValue;
        String unit;
        double increment;
        int order;
        boolean required;
        boolean prefillFromPrevious;
    }
}

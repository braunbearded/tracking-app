package com.example.trackingapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class BackupJsonRepository {
    private BackupJsonRepository() {
    }

    static String exportAll(TrackingDatabase helper) throws JSONException {
        return export(helper, true, true);
    }

    static String exportTrackers(TrackingDatabase helper) throws JSONException {
        return export(helper, true, false);
    }

    static String exportSessions(TrackingDatabase helper) throws JSONException {
        return export(helper, true, true);
    }

    private static String export(TrackingDatabase helper, boolean includeTrackers, boolean includeSessions) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("type", "tracking-app-backup");
        root.put("version", 1);

        JSONArray trackers = new JSONArray();
        if (includeTrackers) for (Tracker tracker : helper.trackers()) {
            JSONObject trackerJson = new JSONObject();
            trackerJson.put("id", tracker.id);
            trackerJson.put("name", tracker.name);
            trackerJson.put("description", tracker.description == null ? "" : tracker.description);
            trackerJson.put("createdAt", tracker.createdAt);
            trackerJson.put("updatedAt", tracker.updatedAt);

            JSONArray fields = new JSONArray();
            for (Item item : tracker.items) {
                for (FieldDefinition field : item.fields) {
                    JSONObject fieldJson = new JSONObject();
                    fieldJson.put("id", field.id);
                    fieldJson.put("itemTitle", item.title == null ? "" : item.title);
                    fieldJson.put("itemOrder", item.order);
                    fieldJson.put("key", field.key);
                    fieldJson.put("label", field.label);
                    fieldJson.put("type", field.type);
                    fieldJson.put("order", field.order);
                    fieldJson.put("defaultValue", field.defaultValue == null ? JSONObject.NULL : field.defaultValue);
                    fieldJson.put("increment", field.increment);
                    fieldJson.put("unit", field.unit == null ? "" : field.unit);
                    fieldJson.put("required", field.required);
                    fieldJson.put("prefillFromPrevious", field.prefillFromPrevious);
                    fields.put(fieldJson);
                }
            }
            trackerJson.put("fields", fields);
            trackers.put(trackerJson);
        }
        root.put("trackers", trackers);

        JSONArray sessions = new JSONArray();
        if (includeSessions) for (Session session : helper.sessions()) {
            JSONObject sessionJson = new JSONObject();
            sessionJson.put("id", session.id);
            sessionJson.put("trackerId", session.trackerId);
            sessionJson.put("createdAt", session.createdAt);
            sessionJson.put("updatedAt", session.updatedAt);
            JSONArray records = new JSONArray();
            for (ItemRecord record : helper.records(session.id).values()) {
                JSONObject recordJson = new JSONObject();
                recordJson.put("fieldId", record.fieldId);
                recordJson.put("values", new JSONObject(record.valuesJson));
                recordJson.put("createdAt", record.createdAt);
                recordJson.put("updatedAt", record.updatedAt);
                records.put(recordJson);
            }
            sessionJson.put("records", records);
            sessions.put(sessionJson);
        }
        root.put("sessions", sessions);
        return root.toString(2);
    }

    static int importAll(TrackingDatabase helper, String json) throws JSONException {
        return importData(helper, json, true, true);
    }

    static int importTrackers(TrackingDatabase helper, String json) throws JSONException {
        return importData(helper, json, true, false);
    }

    static int importSessions(TrackingDatabase helper, String json) throws JSONException {
        return importData(helper, json, true, true);
    }

    private static int importData(TrackingDatabase helper, String json, boolean includeTrackers, boolean includeSessions) throws JSONException {
        JSONObject root = new JSONObject(json);
        SQLiteDatabase db = helper.getWritableDatabase();
        Map<Long, Long> trackerIds = new HashMap<>();
        Map<Long, Long> fieldIds = new HashMap<>();
        int imported = 0;
        long now = System.currentTimeMillis();

        db.beginTransaction();
        try {
            JSONArray trackers = root.optJSONArray("trackers");
            if (trackers == null) {
                trackers = new JSONArray();
                trackers.put(root);
            }

            if (includeTrackers) for (int i = 0; i < trackers.length(); i++) {
                JSONObject tracker = trackers.getJSONObject(i);
                ContentValues trackerValues = new ContentValues();
                trackerValues.put("name", tracker.optString("name", "Importierter Tracker"));
                trackerValues.put("description", tracker.optString("description", ""));
                trackerValues.put("createdAt", tracker.optLong("createdAt", now));
                trackerValues.put("updatedAt", tracker.optLong("updatedAt", now));
                long newTrackerId = db.insert("trackers", null, trackerValues);
                long oldTrackerId = tracker.optLong("id", newTrackerId);
                trackerIds.put(oldTrackerId, newTrackerId);
                imported++;

                JSONArray fields = tracker.optJSONArray("fields");
                if (fields == null) {
                    fields = new JSONArray();
                }
                for (int j = 0; j < fields.length(); j++) {
                    JSONObject field = fields.getJSONObject(j);
                    ContentValues values = new ContentValues();
                    values.put("trackerId", newTrackerId);
                    values.put("itemTitle", field.optString("itemTitle", field.optString("label", field.optString("key", ""))));
                    values.put("itemOrder", field.optInt("itemOrder", j));
                    values.put("fieldKey", field.getString("key"));
                    values.put("label", field.optString("label", field.getString("key")));
                    values.put("type", field.optString("type", "string"));
                    values.put("sortOrder", field.optInt("order", j));
                    if (field.has("defaultValue") && !field.isNull("defaultValue")) {
                        values.put("defaultValue", String.valueOf(field.get("defaultValue")));
                    }
                    values.put("incrementValue", field.optDouble("increment", 1));
                    values.put("unit", field.optString("unit", ""));
                    values.put("required", field.optBoolean("required", false) ? 1 : 0);
                    values.put("prefillFromPrevious", field.optBoolean("prefillFromPrevious", false) ? 1 : 0);
                    long newFieldId = db.insert("fields", null, values);
                    fieldIds.put(field.optLong("id", newFieldId), newFieldId);
                }
            }

            JSONArray sessions = root.optJSONArray("sessions");
            if (includeSessions && sessions != null) {
                for (int i = 0; i < sessions.length(); i++) {
                    JSONObject session = sessions.getJSONObject(i);
                    Long newTrackerId = trackerIds.get(session.optLong("trackerId"));
                    if (newTrackerId == null) {
                        continue;
                    }
                    ContentValues sessionValues = new ContentValues();
                    sessionValues.put("trackerId", newTrackerId);
                    sessionValues.put("createdAt", session.optLong("createdAt", now));
                    sessionValues.put("updatedAt", session.optLong("updatedAt", now));
                    long newSessionId = db.insert("sessions", null, sessionValues);

                    JSONArray records = session.optJSONArray("records");
                    if (records == null) {
                        continue;
                    }
                    for (int j = 0; j < records.length(); j++) {
                        JSONObject record = records.getJSONObject(j);
                        Long newFieldId = fieldIds.get(record.optLong("fieldId"));
                        if (newFieldId == null) {
                            continue;
                        }
                        JSONObject valuesJson = record.optJSONObject("values");
                        ContentValues recordValues = new ContentValues();
                        recordValues.put("sessionId", newSessionId);
                        recordValues.put("trackerId", newTrackerId);
                        recordValues.put("fieldId", newFieldId);
                        recordValues.put("fieldKey", valuesJson == null || !valuesJson.keys().hasNext() ? "" : valuesJson.keys().next());
                        recordValues.put("valuesJson", valuesJson == null ? "{}" : valuesJson.toString());
                        recordValues.put("createdAt", record.optLong("createdAt", now));
                        recordValues.put("updatedAt", record.optLong("updatedAt", now));
                        db.insert("field_records", null, recordValues);
                    }
                }
            }

            db.setTransactionSuccessful();
            return imported;
        } finally {
            db.endTransaction();
        }
    }
}

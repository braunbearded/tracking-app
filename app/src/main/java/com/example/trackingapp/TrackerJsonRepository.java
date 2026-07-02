package com.example.trackingapp;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class TrackerJsonRepository {
    static void updateTracker(TrackingDatabase helper, long id, String json) throws JSONException {
        saveTracker(helper, id, json, false);
    }

    static long saveTracker(TrackingDatabase helper, long id, String json, boolean createNew) throws JSONException {
        SQLiteDatabase db = helper.getWritableDatabase();
        JSONObject root = new JSONObject(json);
        long now = System.currentTimeMillis();

        db.beginTransaction();
        try {
            long trackerId = id;
            if (createNew) {
                ContentValues trackerValues = new ContentValues();
                trackerValues.put("name", root.getString("name"));
                trackerValues.put("description", root.optString("description", ""));
                trackerValues.put("createdAt", now);
                trackerValues.put("updatedAt", now);
                trackerId = db.insert("trackers", null, trackerValues);
            } else {
                ContentValues trackerValues = new ContentValues();
                trackerValues.put("name", root.getString("name"));
                trackerValues.put("description", root.optString("description", ""));
                trackerValues.put("updatedAt", now);
                db.update("trackers", trackerValues, "id=?", new String[]{String.valueOf(id)});
                db.delete("items", "trackerId=?", new String[]{String.valueOf(id)});
            }

            JSONArray items = root.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject itemJson = items.getJSONObject(i);
                ContentValues itemValues = new ContentValues();
                itemValues.put("trackerId", trackerId);
                itemValues.put("title", itemJson.getString("title"));
                itemValues.put("sortOrder", itemJson.optInt("order", i));
                long itemId = db.insert("items", null, itemValues);

                JSONArray fields = itemJson.getJSONArray("fields");
                for (int f = 0; f < fields.length(); f++) {
                    JSONObject fieldJson = fields.getJSONObject(f);
                    ContentValues fieldValues = new ContentValues();
                    fieldValues.put("itemId", itemId);
                    fieldValues.put("fieldKey", fieldJson.getString("key"));
                    fieldValues.put("label", fieldJson.optString("label", fieldJson.getString("key")));
                    fieldValues.put("type", fieldJson.getString("type"));
                    fieldValues.put("sortOrder", fieldJson.optInt("order", f));
                    if (fieldJson.has("defaultValue") && !fieldJson.isNull("defaultValue")) {
                        fieldValues.put("defaultValue", String.valueOf(fieldJson.get("defaultValue")));
                    }
                    fieldValues.put("incrementValue", fieldJson.optDouble("increment", 1));
                    fieldValues.put("decimals", fieldJson.optInt("decimals", 1));
                    fieldValues.put("unit", fieldJson.optString("unit", ""));
                    fieldValues.put("required", fieldJson.optBoolean("required", false) ? 1 : 0);
                    fieldValues.put("prefillFromPrevious", fieldJson.optBoolean("prefillFromPrevious", false) ? 1 : 0);
                    db.insert("fields", null, fieldValues);
                }
            }

            db.setTransactionSuccessful();
            return trackerId;
        } finally {
            db.endTransaction();
        }
    }
}

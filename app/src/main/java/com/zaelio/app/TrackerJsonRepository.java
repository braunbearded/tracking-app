package com.zaelio.app;

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
                db.delete("field_records", "trackerId=?", new String[]{String.valueOf(id)});
                db.delete("fields", "trackerId=?", new String[]{String.valueOf(id)});
            }

            JSONArray fields = root.optJSONArray("fields");
            if (fields == null) {
                fields = new JSONArray();
                JSONArray items = root.optJSONArray("items");
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject itemJson = items.getJSONObject(i);
                        JSONArray itemFields = itemJson.optJSONArray("fields");
                        if (itemFields == null) {
                            itemFields = new JSONArray();
                            itemFields.put(itemJson);
                        }
                        for (int j = 0; j < itemFields.length(); j++) {
                            JSONObject fieldJson = itemFields.getJSONObject(j);
                            if (!fieldJson.has("label") && itemJson.has("title")) {
                                fieldJson.put("label", itemJson.optString("title", fieldJson.optString("key", "")));
                            }
                            fieldJson.put("itemTitle", itemJson.optString("title", fieldJson.optString("label", fieldJson.optString("key", ""))));
                            fieldJson.put("itemOrder", itemJson.optInt("order", i));
                            if (!fieldJson.has("order")) {
                                fieldJson.put("order", j);
                            }
                            fields.put(fieldJson);
                        }
                    }
                }
            }
            for (int i = 0; i < fields.length(); i++) {
                JSONObject fieldJson = fields.getJSONObject(i);
                ContentValues fieldValues = new ContentValues();
                fieldValues.put("trackerId", trackerId);
                fieldValues.put("itemTitle", fieldJson.optString("itemTitle", fieldJson.optString("label", fieldJson.getString("key"))));
                fieldValues.put("itemOrder", fieldJson.optInt("itemOrder", i));
                fieldValues.put("fieldKey", fieldJson.getString("key"));
                fieldValues.put("label", fieldJson.optString("label", fieldJson.getString("key")));
                fieldValues.put("type", fieldJson.getString("type"));
                fieldValues.put("sortOrder", fieldJson.optInt("order", i));
                if (fieldJson.has("defaultValue") && !fieldJson.isNull("defaultValue")) {
                    fieldValues.put("defaultValue", String.valueOf(fieldJson.get("defaultValue")));
                }
                fieldValues.put("incrementValue", fieldJson.optDouble("increment", 1));
                fieldValues.put("unit", fieldJson.optString("unit", ""));
                fieldValues.put("inputSize", fieldJson.optString("inputSize", "standard"));
                fieldValues.put("required", fieldJson.optBoolean("required", false) ? 1 : 0);
                fieldValues.put("prefillFromPrevious", fieldJson.optBoolean("prefillFromPrevious", false) ? 1 : 0);
                db.insert("fields", null, fieldValues);
            }

            db.setTransactionSuccessful();
            return trackerId;
        } finally {
            db.endTransaction();
        }
    }
}

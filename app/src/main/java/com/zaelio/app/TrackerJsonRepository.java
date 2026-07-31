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
            }
            for (int i = 0; i < fields.length(); i++) {
                db.insert("fields", null, JsonUtil.fieldValuesFromJson(fields.getJSONObject(i), trackerId, i));
            }

            db.setTransactionSuccessful();
            return trackerId;
        } finally {
            db.endTransaction();
        }
    }
}

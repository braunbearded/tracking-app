package com.zaelio.app;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class JsonUtil {
    static Map<String, Object> toMap(String json) {
        try {
            Map<String, Object> out = new LinkedHashMap<>();
            if (json == null || json.trim().isEmpty()) {
                return out;
            }

            JSONObject object = new JSONObject(json);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                out.put(key, object.isNull(key) ? null : object.get(key));
            }
            return out;
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static String stringify(Map<String, Object> values) {
        try {
            JSONObject object = new JSONObject();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                object.put(entry.getKey(), entry.getValue());
            }
            return object.toString();
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static String trackerToJson(Tracker tracker) {
        try {
            JSONObject root = new JSONObject();
            root.put("name", tracker.name);
            root.put("description", tracker.description == null ? "" : tracker.description);

            JSONArray fields = new JSONArray();
            for (FieldDefinition field : tracker.fields) {
                JSONObject fieldJson = new JSONObject();
                fieldJson.put("key", field.key);
                fieldJson.put("label", field.label);
                fieldJson.put("type", field.type);
                fieldJson.put("order", field.order);
                fieldJson.put("defaultValue", field.defaultValue == null ? JSONObject.NULL : field.defaultValue);
                fieldJson.put("increment", field.increment);
                fieldJson.put("unit", field.unit == null ? "" : field.unit);
                fieldJson.put("inputSize", field.inputSize == null ? "standard" : field.inputSize);
                fieldJson.put("required", field.required);
                fieldJson.put("prefillFromPrevious", field.prefillFromPrevious);
                fields.put(fieldJson);
            }
            root.put("fields", fields);
            return root.toString(2);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static String newTrackerTemplateJson() {
        try {
            JSONObject root = new JSONObject();
            root.put("name", "Neuer Tracker");
            root.put("description", "");

            JSONArray fields = new JSONArray();
            JSONObject fieldJson = new JSONObject();
            fieldJson.put("key", "value");
            fieldJson.put("label", "Wert");
            fieldJson.put("type", "string");
            fieldJson.put("order", 0);
            fieldJson.put("defaultValue", "");
            fieldJson.put("increment", 1);
            fieldJson.put("unit", "");
            fieldJson.put("inputSize", "standard");
            fieldJson.put("required", false);
            fieldJson.put("prefillFromPrevious", false);
            fields.put(fieldJson);

            root.put("fields", fields);
            return root.toString(2);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

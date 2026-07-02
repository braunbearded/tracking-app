package com.example.trackingapp;

import java.util.*;
import org.json.*;

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
        } catch (JSONException e) { throw new IllegalArgumentException(e); }
    }

    static String stringify(Map<String, Object> values) {
        try {
            JSONObject object = new JSONObject();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                object.put(entry.getKey(), entry.getValue());
            }
            return object.toString();
        } catch (JSONException e) { throw new IllegalArgumentException(e); }
    }

    static String trackerToJson(Tracker tracker) {
        try {
            JSONObject root = new JSONObject();
            root.put("name", tracker.name);
            root.put("description", tracker.description == null ? "" : tracker.description);

            JSONArray items = new JSONArray();
            for (Item item : tracker.items) {
                JSONObject itemJson = new JSONObject();
                itemJson.put("title", item.title);
                itemJson.put("order", item.order);

                JSONArray fields = new JSONArray();
                for (FieldDefinition field : item.fields) {
                    JSONObject fieldJson = new JSONObject();
                    fieldJson.put("key", field.key);
                    fieldJson.put("label", field.label);
                    fieldJson.put("type", field.type);
                    fieldJson.put("order", field.order);
                    fieldJson.put("defaultValue", field.defaultValue == null ? JSONObject.NULL : field.defaultValue);
                    fieldJson.put("increment", field.increment);
                    fieldJson.put("decimals", field.decimals);
                    fieldJson.put("unit", field.unit == null ? "" : field.unit);
                    fieldJson.put("required", field.required);
                    fieldJson.put("prefillFromPrevious", field.prefillFromPrevious);
                    fields.put(fieldJson);
                }
                itemJson.put("fields", fields);
                items.put(itemJson);
            }
            root.put("items", items);
            return root.toString(2);
        } catch (JSONException e) { throw new IllegalArgumentException(e); }
    }

    static String newTrackerTemplateJson() {
        try {
            JSONObject root = new JSONObject();
            root.put("name", "Neuer Tracker");
            root.put("description", "");

            JSONArray items = new JSONArray();
            JSONObject itemJson = new JSONObject();
            itemJson.put("title", "Neues Item");
            itemJson.put("order", 0);

            JSONArray fields = new JSONArray();
            JSONObject fieldJson = new JSONObject();
            fieldJson.put("key", "value");
            fieldJson.put("label", "Wert");
            fieldJson.put("type", "string");
            fieldJson.put("order", 0);
            fieldJson.put("defaultValue", "");
            fieldJson.put("increment", 1);
            fieldJson.put("decimals", 1);
            fieldJson.put("unit", "");
            fieldJson.put("required", false);
            fieldJson.put("prefillFromPrevious", false);
            fields.put(fieldJson);

            itemJson.put("fields", fields);
            items.put(itemJson);
            root.put("items", items);
            return root.toString(2);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

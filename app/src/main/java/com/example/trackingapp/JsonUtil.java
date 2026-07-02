package com.example.trackingapp;

import org.json.*;
import java.util.*;

final class JsonUtil {
    static Map<String, Object> toMap(String json) {
        try {
        Map<String, Object> out = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) return out;
        JSONObject object = new JSONObject(json);
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (object.isNull(key)) out.put(key, null); else out.put(key, object.get(key));
        }
        return out;
        } catch (JSONException e) { throw new IllegalArgumentException(e); }
    }
    static String stringify(Map<String, Object> values) {
        try {
        JSONObject object = new JSONObject();
        for (Map.Entry<String, Object> entry : values.entrySet()) object.put(entry.getKey(), entry.getValue());
        return object.toString();
        } catch (JSONException e) { throw new IllegalArgumentException(e); }
    }
    static String trackerToJson(Tracker tracker) {
        try {
        JSONObject root = new JSONObject();
        root.put("name", tracker.name); root.put("description", tracker.description == null ? "" : tracker.description);
        JSONArray items = new JSONArray();
        for (Item item : tracker.items) {
            JSONObject itemJson = new JSONObject(); itemJson.put("title", item.title); itemJson.put("order", item.order);
            JSONArray fields = new JSONArray();
            for (FieldDefinition field : item.fields) {
                JSONObject f = new JSONObject(); f.put("key", field.key); f.put("label", field.label); f.put("type", field.type);
                f.put("order", field.order); f.put("defaultValue", field.defaultValue == null ? JSONObject.NULL : field.defaultValue);
                f.put("increment", field.increment); f.put("decimals", field.decimals); f.put("unit", field.unit == null ? "" : field.unit);
                f.put("required", field.required); f.put("prefillFromPrevious", field.prefillFromPrevious); fields.put(f);
            }
            itemJson.put("fields", fields); items.put(itemJson);
        }
        root.put("items", items); return root.toString(2);
        } catch (JSONException e) { throw new IllegalArgumentException(e); }
    }
}

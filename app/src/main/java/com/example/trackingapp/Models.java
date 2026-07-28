package com.example.trackingapp;

import java.util.ArrayList;
import java.util.List;

final class Tracker {
    long id;
    String name;
    String description;
    long createdAt;
    long updatedAt;
    List<FieldDefinition> fields = new ArrayList<>();
    List<Item> items = new ArrayList<>();
}

final class FieldDefinition {
    long id;
    long trackerId;
    String key;
    String label;
    String type;
    String defaultValue;
    String unit;
    double increment = 1;
    int order;
    boolean required;
    int decimals = 1;
    boolean prefillFromPrevious;
}

final class Item {
    long id;
    long trackerId;
    String title;
    int order;
    long createdAt;
    long updatedAt;
    List<FieldDefinition> fields = new ArrayList<>();
}

final class Session {
    long id;
    long trackerId;
    long createdAt;
    long updatedAt;
}

final class ItemRecord extends FieldRecord {
}

class FieldRecord {
    long id;
    long sessionId;
    long trackerId;
    long fieldId;
    long createdAt;
    long updatedAt;
    String valuesJson;
}

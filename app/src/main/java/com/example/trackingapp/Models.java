package com.example.trackingapp;

import java.util.*;

final class Tracker {
    long id;
    String name;
    String description;
    long createdAt;
    long updatedAt;
    List<Item> items = new ArrayList<>();
}

final class Item {
    long id;
    long trackerId;
    String title;
    int order;
    List<FieldDefinition> fields = new ArrayList<>();
}

final class FieldDefinition {
    long id;
    long itemId;
    String key;
    String label;
    String type;
    String defaultValue;
    String unit;
    double increment = 1;
    int order;
    int decimals = 1;
    boolean required;
    boolean prefillFromPrevious;
}

final class Session {
    long id;
    long trackerId;
    long createdAt;
    long updatedAt;
}

final class ItemRecord {
    long id;
    long sessionId;
    long trackerId;
    long itemId;
    long createdAt;
    long updatedAt;
    String valuesJson;
}

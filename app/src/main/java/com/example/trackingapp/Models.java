package com.example.trackingapp;

import java.util.*;

final class Tracker { long id; String name, description; long createdAt, updatedAt; List<Item> items = new ArrayList<>(); }
final class Item { long id, trackerId; String title; int order; List<FieldDefinition> fields = new ArrayList<>(); }
final class FieldDefinition { long id, itemId; String key, label, type, defaultValue, unit; double increment = 1; int order, decimals = 1; boolean required, prefillFromPrevious; }
final class Session { long id, trackerId, createdAt, updatedAt; String status; }
final class ItemRecord { long id, sessionId, trackerId, itemId, createdAt, updatedAt; String valuesJson; }

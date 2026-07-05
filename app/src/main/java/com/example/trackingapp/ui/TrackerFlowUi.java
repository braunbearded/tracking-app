package com.example.trackingapp;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trackingapp.theme.ThemeStore;
import com.example.trackingapp.ui.AppUi;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public final class TrackerFlowUi {
    private final Activity activity;
    private final TrackingDatabase db;
    private final ThemeStore theme;
    private final AppUi ui;
    private final Handler handler;
    private final Runnable backToSessions;
    private final Runnable backToTrackers;
    private final Map<String, Long> timers = new HashMap<>();
    private LinearLayout root;

    public TrackerFlowUi(Activity activity, TrackingDatabase db, ThemeStore theme, AppUi ui,
                         Handler handler, Runnable backToSessions, Runnable backToTrackers) {
        this.activity = activity;
        this.db = db;
        this.theme = theme;
        this.ui = ui;
        this.handler = handler;
        this.backToSessions = backToSessions;
        this.backToTrackers = backToTrackers;
    }

    public void clearTimers() {
        timers.clear();
    }

    public void chooseTracker() {
        List<Tracker> trackers = db.trackers();
        BottomSheetDialog dialog = new BottomSheetDialog(activity);

        LinearLayout sheet = new LinearLayout(activity);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(ui.px(16), ui.px(16), ui.px(16), ui.px(20));
        sheet.setBackgroundColor(theme.surfaceColor());

        TextView title = ui.tv("Tracker auswählen", 18);
        title.setPadding(0, 0, 0, ui.px(4));
        sheet.addView(title);

        TextView subtitle = new TextView(activity);
        subtitle.setText(trackers.isEmpty()
                ? "Lege zuerst einen Tracker an."
                : "Wähle einen Tracker für die neue Session.");
        subtitle.setTextSize(ui.sp(14));
        subtitle.setTextColor(theme.secondaryTextColor());
        subtitle.setPadding(0, 0, 0, ui.px(16));
        sheet.addView(subtitle);

        if (trackers.isEmpty()) {
            Button create = ui.primaryButton("Neuen Tracker anlegen");
            create.setOnClickListener(v -> {
                dialog.dismiss();
                createTracker();
            });
            sheet.addView(create, new LinearLayout.LayoutParams(-1, -2));
        } else {
            for (Tracker tracker : trackers) {
                View item = selectionRow(
                        tracker.name,
                        tracker.description == null || tracker.description.trim().isEmpty()
                                ? "Ohne Beschreibung"
                                : tracker.description);
                item.setOnClickListener(v -> {
                    dialog.dismiss();
                    openSession(db.createSession(tracker.id));
                });
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                rowLp.bottomMargin = ui.px(10);
                sheet.addView(item, rowLp);
            }
        }

        Button cancel = ui.secondaryButton("Abbrechen");
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(-1, -2);
        cancelLp.topMargin = ui.px(4);
        sheet.addView(cancel, cancelLp);

        dialog.setContentView(sheet);
        dialog.show();
    }

    private View selectionRow(String title, String subtitle) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(ui.px(16), ui.px(14), ui.px(16), ui.px(14));
        row.setBackground(ui.makeRoundedCard(theme.surfaceAltColor(), theme.borderColor()));

        LinearLayout text = new LinearLayout(activity);
        text.setOrientation(LinearLayout.VERTICAL);

        TextView rowTitle = new TextView(activity);
        rowTitle.setText(title);
        rowTitle.setTextSize(ui.sp(16));
        rowTitle.setTypeface(Typeface.DEFAULT_BOLD);
        rowTitle.setTextColor(theme.primaryTextColor());
        text.addView(rowTitle);

        TextView rowSubtitle = new TextView(activity);
        rowSubtitle.setText(subtitle);
        rowSubtitle.setTextSize(ui.sp(13));
        rowSubtitle.setTextColor(theme.mutedTextColor());
        rowSubtitle.setPadding(0, ui.px(2), 0, 0);
        text.addView(rowSubtitle);

        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(text, textLp);

        TextView arrow = new TextView(activity);
        arrow.setText("›");
        arrow.setTextSize(ui.sp(22));
        arrow.setTextColor(theme.mutedTextColor());
        arrow.setPadding(ui.px(10), 0, 0, 0);
        row.addView(arrow);

        return row;
    }

    public void createTracker() {
        openTrackerEditor(-1, templateTracker(), true);
    }

    public void editTracker(long id) {
        Tracker tracker = db.readTracker(id);
        if (tracker == null) {
            Toast.makeText(activity, "Tracker nicht gefunden", Toast.LENGTH_SHORT).show();
            backToTrackers.run();
            return;
        }
        openTrackerEditor(id, tracker, false);
    }

    public void openSession(long sessionId) {
        Session session = db.session(sessionId);
        if (session == null) {
            Toast.makeText(activity, "Session nicht gefunden", Toast.LENGTH_SHORT).show();
            backToSessions.run();
            return;
        }

        Tracker tracker = db.readTracker(session.trackerId);
        if (tracker == null || tracker.items.isEmpty()) {
            Toast.makeText(activity, "Tracker enthält keine Items", Toast.LENGTH_SHORT).show();
            backToSessions.run();
            return;
        }

        Map<Long, ItemRecord> records = db.records(sessionId);
        int startIndex = 0;
        for (int i = 0; i < tracker.items.size(); i++) {
            if (!records.containsKey(tracker.items.get(i).id)) {
                startIndex = i;
                break;
            }
        }
        showItem(session, tracker, startIndex);
    }

    private void openTrackerEditor(long id, Tracker tracker, boolean isNew) {
        if (!isNew && db.readTracker(id) == null) {
            Toast.makeText(activity, "Tracker nicht gefunden", Toast.LENGTH_SHORT).show();
            backToTrackers.run();
            return;
        }

        base();

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(ui.px(16), ui.px(16), ui.px(16), ui.px(0));
        header.setBackgroundColor(theme.backgroundColor());

        View hero = heroCard(
                isNew ? "NEUER TRACKER" : "TRACKER BEARBEITEN",
                isNew ? "Tracker anlegen" : "Tracker bearbeiten",
                "Name, Beschreibung, Items und Fields direkt im Formular pflegen.",
                isNew ? "Neu" : "Bestehend");
        header.addView(hero);

        root.addView(header);

        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(ui.px(16), ui.px(0), ui.px(16), ui.px(16));
        scrollView.addView(body);

        TrackerEditorForm form = buildTrackerEditorForm(tracker);

        LinearLayout formCard = ui.contentCard();
        ui.addSectionHeader(formCard, "GRUNDDATEN", "Name und Beschreibung",
                "Name und Beschreibung bilden die Basis für neue Sessions.");

        formCard.addView(form.nameInput);
        formCard.addView(form.descriptionInput);
        body.addView(formCard);

        Button addItem = ui.primaryButton("Item hinzufügen");
        LinearLayout.LayoutParams addItemLp = new LinearLayout.LayoutParams(-1, -2);
        addItemLp.bottomMargin = ui.px(16);
        body.addView(addItem, addItemLp);

        LinearLayout itemsContainer = new LinearLayout(activity);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        body.addView(itemsContainer, new LinearLayout.LayoutParams(-1, -2));

        if (tracker.items.isEmpty()) {
            LinearLayout empty = ui.contentCard();
            ui.addSectionHeader(empty, "ITEMS", "Noch keine Items angelegt",
                    "Tippe auf \"Item hinzufügen\" und lege dann darunter die Fields an.");

            itemsContainer.addView(empty);
        } else {
            for (Item item : tracker.items) {
                addItemEditor(itemsContainer, form.items, item);
            }
        }

        addItem.setOnClickListener(v -> addItemEditor(itemsContainer, form.items, null));

        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(activity);
        footer.setWeightSum(2);

        Button back = ui.secondaryButton("Zurück");
        Button save = ui.primaryButton("Tracker speichern");
        footer.addView(back, new LinearLayout.LayoutParams(0, -2, 1));
        footer.addView(save, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(footer);

        back.setOnClickListener(v -> backToTrackers.run());
        save.setOnClickListener(v -> {
            try {
                String json = trackerEditorToJson(form);
                if (isNew) {
                    TrackerJsonRepository.saveTracker(db, -1, json, true);
                } else {
                    TrackerJsonRepository.updateTracker(db, id, json);
                }
                Toast.makeText(activity, "Gespeichert", Toast.LENGTH_SHORT).show();
                backToTrackers.run();
            } catch (Exception e) {
                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showItem(Session session, Tracker tracker, int index) {
        base();
        boolean readOnly = "completed".equals(session.status);
        ScrollView scrollView = new ScrollView(activity);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(ui.px(16), ui.px(8), ui.px(16), ui.px(16));
        scrollView.addView(box);

        View hero = heroCard(
                "SESSION",
                tracker.name,
                "Erfasse die Werte für diese Session und speichere Änderungen unterwegs.",
                session.status.equals("open") ? "Offen" : "Abgeschlossen");
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, -2);
        heroLp.leftMargin = ui.px(16);
        heroLp.topMargin = ui.px(16);
        heroLp.rightMargin = ui.px(16);
        root.addView(hero, heroLp);

        Map<Long, Map<String, View>> inputsByItem = new LinkedHashMap<>();
        for (int itemIndex = 0; itemIndex < tracker.items.size(); itemIndex++) {
            Item item = tracker.items.get(itemIndex);
            Map<String, Object> values = initialValues(session, item);
            Map<String, View> inputs = new HashMap<>();
            inputsByItem.put(item.id, inputs);

            LinearLayout card = ui.contentCard();
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
            cardLp.bottomMargin = ui.px(20);

            ui.addSectionHeader(card, "ITEM " + (itemIndex + 1), item.title,
                    readOnly ? "Read-only. Werte sind unveränderlich." : "Änderungen werden beim Zurück- oder Weitergehen gespeichert.");
            card.addView(summaryCard(readOnly, values, item));

            for (FieldDefinition field : item.fields) {
                fieldControl(card, field, values, inputs, readOnly);
            }

            box.addView(card, cardLp);
        }

        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(activity);
        footer.setWeightSum(2);

        Button back = ui.secondaryButton("Zurück");
        Button close = ui.primaryButton(readOnly ? "Übersicht" : "Session speichern / schließen");
        footer.addView(back, new LinearLayout.LayoutParams(0, -2, 1));
        footer.addView(close, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(footer);

        back.setOnClickListener(v -> {
            if (!readOnly) {
                saveSessionDraft(session, tracker, inputsByItem);
            }
            backToSessions.run();
        });

        close.setOnClickListener(v -> {
            if (!readOnly) {
                saveSessionDraft(session, tracker, inputsByItem);
                db.complete(session.id);
            }
            backToSessions.run();
        });
    }

    private View heroCard(String eyebrowText, String titleText, String subtitleText, String chipText) {
        LinearLayout hero = ui.contentCard();
        ui.addSectionHeader(hero, eyebrowText, titleText, subtitleText);

        if (chipText != null && !chipText.isEmpty()) {
            hero.addView(ui.chip(chipText, theme.accentSoftColor(), theme.accentColor()));
        }

        return hero;
    }

    private void saveSessionDraft(Session session, Tracker tracker, Map<Long, Map<String, View>> inputsByItem) {
        if (!"open".equals(session.status)) {
            return;
        }

        for (Item item : tracker.items) {
            Map<String, View> inputs = inputsByItem.get(item.id);
            if (inputs != null) {
                db.saveRecord(session, item.id, readInputs(item, inputs));
            }
        }
    }

    private Map<String, Object> initialValues(Session session, Item item) {
        Map<Long, ItemRecord> records = db.records(session.id);
        if (records.containsKey(item.id)) {
            return JsonUtil.toMap(records.get(item.id).valuesJson);
        }

        Map<String, Object> values = new LinkedHashMap<>();
        for (FieldDefinition field : item.fields) {
            Object value = TrackingDatabase.NO_PREVIOUS;
            if (field.prefillFromPrevious) {
                value = db.previousValue(session.trackerId, session.id, session.createdAt, item.id, field.key);
            }
            if (!field.prefillFromPrevious || value == TrackingDatabase.NO_PREVIOUS) {
                value = parse(field.defaultValue, field.type);
            }
            values.put(field.key, value);
        }
        return values;
    }

    private Object parse(String value, String type) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            if ("int".equals(type) || "duration".equals(type)) {
                return Long.parseLong(value);
            }
            if ("float".equals(type)) {
                return Double.parseDouble(value);
            }
        } catch (Exception ignored) {
        }
        return value;
    }

    private void fieldControl(
            LinearLayout box,
            FieldDefinition field,
            Map<String, Object> values,
            Map<String, View> inputs,
            boolean readOnly) {
        box.addView(ui.tv(field.label + (field.unit == null || field.unit.isEmpty() ? "" : " (" + field.unit + ")"), 16));

        Object value = values.get(field.key);
        if ("string".equals(field.type)) {
            EditText editText = new EditText(activity);
            editText.setText(value == null ? "" : String.valueOf(value));
            editText.setTextColor(theme.primaryTextColor());
            editText.setHintTextColor(theme.mutedTextColor());
            editText.setEnabled(!readOnly);
            box.addView(editText);
            inputs.put(field.key, editText);
            return;
        }

        if ("duration".equals(field.type)) {
            TextView display = ui.tv(formatMs(toLong(value)), 24);
            display.setTag(toLong(value));

            LinearLayout row = new LinearLayout(activity);
            Button start = ui.secondaryButton("Start");
            Button stop = ui.secondaryButton("Stop");
            Button reset = ui.ghostButton("Reset");
            row.addView(start);
            row.addView(stop);
            row.addView(reset);
            box.addView(display);
            box.addView(row);

            start.setEnabled(!readOnly);
            stop.setEnabled(!readOnly);
            reset.setEnabled(!readOnly);

            start.setOnClickListener(v -> {
                long current = (Long) display.getTag();
                timers.put(field.key, System.currentTimeMillis() - current);
                tick(display, field.key);
            });
            stop.setOnClickListener(v -> timers.remove(field.key));
            reset.setOnClickListener(v -> {
                timers.remove(field.key);
                display.setTag(0L);
                display.setText(formatMs(0));
            });

            inputs.put(field.key, display);
            return;
        }

        LinearLayout row = new LinearLayout(activity);
        Button minus = ui.secondaryButton("−");
        Button plus = ui.primaryButton("+");
        EditText editText = new EditText(activity);
        editText.setText(value == null ? "" : String.valueOf(value));
        editText.setInputType("int".equals(field.type)
                ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED
                : InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        editText.setTextColor(theme.primaryTextColor());
        editText.setHintTextColor(theme.mutedTextColor());
        editText.setEnabled(!readOnly);
        minus.setEnabled(!readOnly);
        plus.setEnabled(!readOnly);

        View.OnClickListener adjust = v -> {
            hideKeyboard(editText);
            double current = editText.getText().toString().isEmpty() ? 0 : Double.parseDouble(editText.getText().toString());
            current += v == plus ? field.increment : -field.increment;
            editText.setText("int".equals(field.type)
                    ? String.valueOf(Math.round(current))
                    : String.format(Locale.US, "%." + field.decimals + "f", current));
        };
        minus.setOnClickListener(adjust);
        plus.setOnClickListener(adjust);

        row.addView(minus);
        row.addView(editText, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(plus);
        box.addView(row);
        inputs.put(field.key, editText);
    }

    private void tick(TextView display, String key) {
        Long startedAt = timers.get(key);
        if (startedAt == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - startedAt;
        display.setTag(elapsed);
        display.setText(formatMs(elapsed));
        handler.postDelayed(() -> tick(display, key), 500);
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        view.clearFocus();
    }

    private Map<String, Object> readInputs(Item item, Map<String, View> inputs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (FieldDefinition field : item.fields) {
            View view = inputs.get(field.key);
            if (view instanceof EditText) {
                String value = ((EditText) view).getText().toString();
                values.put(field.key, parse(value, field.type));
            } else if (view instanceof TextView) {
                values.put(field.key, (Long) view.getTag());
            }
        }
        return values;
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatMs(long millis) {
        long seconds = millis / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }

    private View summaryCard(boolean readOnly, Map<String, Object> values, Item item) {
        LinearLayout card = ui.contentCard();
        ui.addSectionHeader(card, "ÜBERSICHT",
                readOnly ? "Abgeschlossener Stand" : "Aktueller Session-Stand",
                readOnly
                        ? "Read-only. Werte sind unveränderlich."
                        : "Änderungen werden beim Zurück- oder Weitergehen gespeichert.");

        TextView body = new TextView(activity);
        body.setText(summaryText(values, item));
        body.setTextSize(ui.sp(14));
        body.setTextColor(theme.primaryTextColor());
        card.addView(body);

        return card;
    }

    private String summaryText(Map<String, Object> values, Item item) {
        StringBuilder builder = new StringBuilder();
        for (FieldDefinition field : item.fields) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(field.label).append(": ");

            Object value = values.get(field.key);
            if (value == null || String.valueOf(value).isEmpty()) {
                builder.append("—");
            } else if ("duration".equals(field.type)) {
                builder.append(formatMs(toLong(value)));
            } else if ("float".equals(field.type)) {
                builder.append(String.format(Locale.US, "%." + field.decimals + "f", toDouble(value)));
            } else {
                builder.append(String.valueOf(value));
            }

            if (field.unit != null && !field.unit.isEmpty() && value != null && !"string".equals(field.type)) {
                builder.append(" ").append(field.unit);
            }
        }
        return builder.toString();
    }

    private Tracker templateTracker() {
        Tracker tracker = new Tracker();
        tracker.name = "";
        tracker.description = "";
        return tracker;
    }

    private TrackerEditorForm buildTrackerEditorForm(Tracker tracker) {
        TrackerEditorForm form = new TrackerEditorForm();
        form.nameInput = labeledInput("Tracker-Name", tracker.name == null ? "" : tracker.name,
                InputType.TYPE_CLASS_TEXT);
        form.descriptionInput = labeledInput("Beschreibung", tracker.description == null ? "" : tracker.description,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        form.descriptionInput.setMinLines(2);
        form.descriptionInput.setGravity(Gravity.TOP);
        return form;
    }

    private ItemEditorViews addItemEditor(LinearLayout container, List<ItemEditorViews> itemEditors, Item item) {
        ItemEditorViews views = new ItemEditorViews();

        if (itemEditors.isEmpty() && container.getChildCount() == 1) {
            container.removeAllViews();
        }

        LinearLayout card = ui.contentCard();
        ui.addSectionHeader(card, "ITEM", item == null ? "Neues Item" : item.title,
                "Drag and drop zum Umordnen. Felder werden darunter gepflegt.");

        View drag = dragHandle();
        Button remove = ui.dangerButton("Entfernen");
        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.addView(new View(activity), new LinearLayout.LayoutParams(0, -2, 1));
        actionRow.addView(drag);
        actionRow.addView(remove);
        card.addView(actionRow);

        views.titleInput = labeledInput("Titel", item == null ? "" : item.title, InputType.TYPE_CLASS_TEXT);
        card.addView(views.titleInput);

        LinearLayout fieldsHeader = new LinearLayout(activity);
        fieldsHeader.setOrientation(LinearLayout.HORIZONTAL);
        fieldsHeader.setPadding(0, ui.px(8), 0, ui.px(8));
        TextView fieldsTitle = ui.tv("Fields", 16);
        fieldsTitle.setPadding(0, 0, 0, 0);
        fieldsHeader.addView(fieldsTitle, new LinearLayout.LayoutParams(0, -2, 1));

        Button addField = ui.primaryButton("Field hinzufügen");
        fieldsHeader.addView(addField);
        card.addView(fieldsHeader);

        LinearLayout fieldsContainer = new LinearLayout(activity);
        fieldsContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(fieldsContainer);
        views.fieldsContainer = fieldsContainer;

        if (item != null) {
            for (FieldDefinition field : item.fields) {
                addFieldEditor(fieldsContainer, views.fields, field);
            }
        }

        addField.setOnClickListener(v -> addFieldEditor(fieldsContainer, views.fields, null));
        remove.setOnClickListener(v -> {
            container.removeView(card);
            views.removed = true;
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = ui.px(16);
        container.addView(card, lp);
        views.card = card;
        views.container = container;
        itemEditors.add(views);
        card.setTag(views);
        drag.setOnLongClickListener(v -> {
            startCardDrag(card);
            return true;
        });
        drag.setOnTouchListener((v, event) -> {
            ViewParent parent = v.getParent();
            while (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
                if (parent instanceof View) {
                    parent = ((View) parent).getParent();
                } else {
                    break;
                }
            }
            return false;
        });
        configureItemDragTarget(container, card, itemEditors);
        return views;
    }

    private FieldEditorViews addFieldEditor(LinearLayout container, List<FieldEditorViews> fieldEditors, FieldDefinition field) {
        FieldEditorViews views = new FieldEditorViews();

        LinearLayout row = ui.contentCard();
        ui.addSectionHeader(row, "FIELD", field == null ? "Neues Field" : field.label,
                "Typ, Default und Sichtbarkeit werden hier festgelegt.");
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.bottomMargin = ui.px(12);

        View drag = dragHandle();
        Button remove = ui.dangerButton("Entfernen");
        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.addView(new View(activity), new LinearLayout.LayoutParams(0, -2, 1));
        actionRow.addView(drag);
        actionRow.addView(remove);
        row.addView(actionRow);

        views.keyInput = labeledInput("Key", field == null ? "" : field.key, InputType.TYPE_CLASS_TEXT);
        views.labelInput = labeledInput("Label", field == null ? "" : field.label, InputType.TYPE_CLASS_TEXT);
        views.defaultValueInput = labeledInput("Default", field == null ? "" : String.valueOf(field.defaultValue == null ? "" : field.defaultValue), InputType.TYPE_CLASS_TEXT);
        views.unitInput = labeledInput("Unit", field == null ? "" : field.unit, InputType.TYPE_CLASS_TEXT);
        views.incrementInput = labeledInput("Increment", field == null ? "1" : String.valueOf(field.increment), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        views.decimalsInput = labeledInput("Decimals", field == null ? "1" : String.valueOf(field.decimals), InputType.TYPE_CLASS_NUMBER);

        row.addView(views.keyInput);
        row.addView(views.labelInput);
        row.addView(views.defaultValueInput);

        RadioGroup typeGroup = new RadioGroup(activity);
        typeGroup.setOrientation(RadioGroup.HORIZONTAL);
        typeGroup.setPadding(0, ui.px(4), 0, ui.px(4));
        typeGroup.setTag("type");
        views.typeGroup = typeGroup;

        RadioButton stringType = radioType("String", "string", field == null || field.type == null || "string".equals(field.type));
        RadioButton intType = radioType("Integer", "int", field != null && "int".equals(field.type));
        RadioButton floatType = radioType("Decimal", "float", field != null && "float".equals(field.type));
        RadioButton durationType = radioType("Timer", "duration", field != null && "duration".equals(field.type));
        typeGroup.addView(stringType);
        typeGroup.addView(intType);
        typeGroup.addView(floatType);
        typeGroup.addView(durationType);

        LinearLayout optionsRow = new LinearLayout(activity);
        optionsRow.setOrientation(LinearLayout.VERTICAL);
        optionsRow.addView(wrapLabeledView("Type", typeGroup));

        CheckBox required = new CheckBox(activity);
        required.setText("Required");
        required.setChecked(field != null && field.required);
        required.setTextColor(theme.primaryTextColor());
        views.requiredCheck = required;
        optionsRow.addView(required);
        row.addView(optionsRow);

        CheckBox prefill = new CheckBox(activity);
        prefill.setText("Prefill from previous");
        prefill.setChecked(field != null && field.prefillFromPrevious);
        prefill.setTextColor(theme.primaryTextColor());
        views.prefillCheck = prefill;
        row.addView(prefill);

        LinearLayout numericRow = new LinearLayout(activity);
        numericRow.setOrientation(LinearLayout.HORIZONTAL);
        numericRow.setWeightSum(2);

        LinearLayout incrementWrap = wrapLabeledView("Increment", views.incrementInput);
        LinearLayout decimalsWrap = wrapLabeledView("Decimals", views.decimalsInput);
        numericRow.addView(incrementWrap, new LinearLayout.LayoutParams(0, -2, 1));
        numericRow.addView(decimalsWrap, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(numericRow);

        views.incrementWrap = incrementWrap;
        views.decimalsWrap = decimalsWrap;
        views.numericRow = numericRow;
        updateFieldEditorControls(views, selectedType(typeGroup));
        typeGroup.setOnCheckedChangeListener((group, checkedId) -> updateFieldEditorControls(views, selectedType(group)));

        remove.setOnClickListener(v -> {
            container.removeView(row);
            views.removed = true;
        });

        container.addView(row, rowLp);
        views.row = row;
        views.container = container;
        fieldEditors.add(views);
        row.setTag(views);
        drag.setOnLongClickListener(v -> {
            startCardDrag(row);
            return true;
        });
        drag.setOnTouchListener((v, event) -> {
            ViewParent parent = v.getParent();
            while (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
                if (parent instanceof View) {
                    parent = ((View) parent).getParent();
                } else {
                    break;
                }
            }
            return false;
        });
        configureFieldDragTarget(container, row, fieldEditors);
        return views;
    }

    private LinearLayout wrapLabeledView(String label, View view) {
        LinearLayout group = new LinearLayout(activity);
        group.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(activity);
        title.setText(label);
        title.setTextSize(ui.sp(12));
        title.setTextColor(theme.mutedTextColor());
        title.setPadding(0, 0, 0, ui.px(4));
        group.addView(title);
        group.addView(view);
        return group;
    }

    private EditText labeledInput(String label, String value, int inputType) {
        EditText input = new EditText(activity);
        input.setText(value == null ? "" : value);
        input.setHint(label);
        input.setInputType(inputType);
        input.setPadding(ui.px(12), ui.px(12), ui.px(12), ui.px(12));
        input.setBackground(ui.makeRoundedCard(theme.surfaceColor(), theme.borderColor()));
        input.setTextColor(theme.primaryTextColor());
        input.setHintTextColor(theme.mutedTextColor());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = ui.px(12);
        input.setLayoutParams(lp);
        return input;
    }

    private String trackerEditorToJson(TrackerEditorForm form) throws Exception {
        JSONObject root = new JSONObject();
        root.put("name", form.nameInput.getText().toString().trim());
        root.put("description", form.descriptionInput.getText().toString().trim());

        JSONArray items = new JSONArray();
        int itemOrder = 0;
        for (ItemEditorViews itemViews : form.items) {
            if (itemViews.removed) {
                continue;
            }

            JSONObject item = new JSONObject();
            item.put("title", itemViews.titleInput.getText().toString().trim());
            item.put("order", itemOrder++);

            JSONArray fields = new JSONArray();
            int fieldOrder = 0;
            for (FieldEditorViews fieldViews : itemViews.fields) {
                if (fieldViews.removed) {
                    continue;
                }

                JSONObject field = new JSONObject();
                field.put("key", fieldViews.keyInput.getText().toString().trim());
                field.put("label", fieldViews.labelInput.getText().toString().trim());
                field.put("type", selectedType(fieldViews.typeGroup));
                field.put("order", fieldOrder++);

                String defaultValue = fieldViews.defaultValueInput.getText().toString().trim();
                field.put("defaultValue", defaultValue.isEmpty() ? JSONObject.NULL : defaultValue);
                field.put("increment", parseDoubleSafe(fieldViews.incrementInput.getText().toString(), 1));
                field.put("decimals", parseIntSafe(fieldViews.decimalsInput.getText().toString(), 1));
                field.put("unit", fieldViews.unitInput.getText().toString().trim());
                field.put("required", fieldViews.requiredCheck.isChecked());
                field.put("prefillFromPrevious", fieldViews.prefillCheck.isChecked());
                fields.put(field);
            }
            item.put("fields", fields);
            items.put(item);
        }

        root.put("items", items);
        return root.toString(2);
    }

    private int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseDoubleSafe(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private RadioButton radioType(String label, String value, boolean checked) {
        RadioButton button = new RadioButton(activity);
        button.setId(View.generateViewId());
        button.setText(label);
        button.setTag(value);
        button.setChecked(checked);
        button.setTextSize(ui.sp(14));
        button.setTextColor(theme.primaryTextColor());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = ui.px(12);
        button.setLayoutParams(lp);
        return button;
    }

    private String selectedType(RadioGroup group) {
        int checkedId = group.getCheckedRadioButtonId();
        if (checkedId == -1) {
            return "string";
        }
        RadioButton checked = group.findViewById(checkedId);
        Object tag = checked == null ? null : checked.getTag();
        return tag == null ? "string" : String.valueOf(tag);
    }

    private void updateFieldEditorControls(FieldEditorViews views, String type) {
        boolean showIncrement = "int".equals(type) || "float".equals(type);
        boolean showDecimals = "float".equals(type);

        if (views.incrementWrap != null) {
            views.incrementWrap.setVisibility(showIncrement ? View.VISIBLE : View.GONE);
        }
        if (views.decimalsWrap != null) {
            views.decimalsWrap.setVisibility(showDecimals ? View.VISIBLE : View.GONE);
        }
    }

    private View dragHandle() {
        LinearLayout handle = new LinearLayout(activity);
        handle.setOrientation(LinearLayout.VERTICAL);
        handle.setGravity(Gravity.CENTER);
        handle.setPadding(ui.px(10), ui.px(8), ui.px(10), ui.px(8));
        handle.setBackground(ui.makeRoundedCard(theme.surfaceAltColor(), theme.borderColor()));
        handle.setContentDescription("Verschieben");
        handle.setClickable(true);
        handle.setFocusable(true);

        for (int i = 0; i < 3; i++) {
            View bar = new View(activity);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ui.px(14), ui.px(2));
            if (i < 2) {
                lp.bottomMargin = ui.px(3);
            }
            bar.setLayoutParams(lp);
            bar.setBackgroundColor(theme.mutedTextColor());
            handle.addView(bar);
        }

        return handle;
    }

    private void startCardDrag(View view) {
        ClipData data = ClipData.newPlainText("tracker-editor-drag", "card");
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view.startDragAndDrop(data, shadow, view, 0);
        } else {
            view.startDrag(data, shadow, view, 0);
        }
    }

    private void configureItemDragTarget(LinearLayout container, View target, List<ItemEditorViews> itemEditors) {
        target.setOnDragListener((v, event) -> handleDrop(container, target, itemEditors, event));
        container.setOnDragListener((v, event) -> handleContainerDrop(container, itemEditors, event));
    }

    private void configureFieldDragTarget(LinearLayout container, View target, List<FieldEditorViews> fieldEditors) {
        target.setOnDragListener((v, event) -> handleDrop(container, target, fieldEditors, event));
        container.setOnDragListener((v, event) -> handleContainerDrop(container, fieldEditors, event));
    }

    private boolean handleDrop(LinearLayout container, View target, List<?> list, android.view.DragEvent event) {
        if (!(event.getLocalState() instanceof View)) {
            return false;
        }

        View dragged = (View) event.getLocalState();
        if (event.getAction() == android.view.DragEvent.ACTION_DROP) {
            if (dragged == target || dragged.getParent() != container) {
                return true;
            }
            moveViewBefore(container, dragged, target);
            moveListBefore(list, dragged.getTag(), target.getTag());
            return true;
        }

        return true;
    }

    private boolean handleContainerDrop(LinearLayout container, List<?> list, android.view.DragEvent event) {
        if (!(event.getLocalState() instanceof View)) {
            return false;
        }

        View dragged = (View) event.getLocalState();
        if (event.getAction() == android.view.DragEvent.ACTION_DROP) {
            if (dragged.getParent() != container) {
                return true;
            }
            moveViewToEnd(container, dragged);
            moveListToEnd(list, dragged.getTag());
            return true;
        }

        return true;
    }

    private void moveViewBefore(LinearLayout container, View dragged, View target) {
        int fromIndex = container.indexOfChild(dragged);
        int toIndex = container.indexOfChild(target);
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) {
            return;
        }
        container.removeView(dragged);
        if (fromIndex < toIndex) {
            toIndex--;
        }
        container.addView(dragged, toIndex);
    }

    private void moveViewToEnd(LinearLayout container, View dragged) {
        int fromIndex = container.indexOfChild(dragged);
        if (fromIndex < 0) {
            return;
        }
        container.removeView(dragged);
        container.addView(dragged);
    }

    private void moveListBefore(List<?> list, Object dragged, Object target) {
        if (dragged == null || target == null || dragged == target) {
            return;
        }

        int fromIndex = list.indexOf(dragged);
        int toIndex = list.indexOf(target);
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) {
            return;
        }

        Object value = list.remove(fromIndex);
        if (fromIndex < toIndex) {
            toIndex--;
        }
        ((List) list).add(toIndex, value);
    }

    private void moveListToEnd(List<?> list, Object dragged) {
        if (dragged == null) {
            return;
        }

        int fromIndex = list.indexOf(dragged);
        if (fromIndex < 0) {
            return;
        }

        Object value = list.remove(fromIndex);
        ((List) list).add(value);
    }

    private void base() {
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(theme.backgroundColor());
        activity.setContentView(root);
    }

    private static final class TrackerEditorForm {
        EditText nameInput;
        EditText descriptionInput;
        final List<ItemEditorViews> items = new ArrayList<>();
    }

    private static final class ItemEditorViews {
        LinearLayout card;
        LinearLayout container;
        EditText titleInput;
        LinearLayout fieldsContainer;
        final List<FieldEditorViews> fields = new ArrayList<>();
        boolean removed;
    }

    private static final class FieldEditorViews {
        LinearLayout row;
        LinearLayout container;
        LinearLayout numericRow;
        LinearLayout incrementWrap;
        LinearLayout decimalsWrap;
        EditText keyInput;
        EditText labelInput;
        EditText defaultValueInput;
        EditText unitInput;
        EditText incrementInput;
        EditText decimalsInput;
        RadioGroup typeGroup;
        CheckBox requiredCheck;
        CheckBox prefillCheck;
        boolean removed;
    }
}

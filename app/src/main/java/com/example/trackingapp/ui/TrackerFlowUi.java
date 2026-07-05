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
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.PopupMenu;
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

        if (trackers.isEmpty()) {
            Button create = ui.primaryButton("Neuen Tracker anlegen");
            create.setOnClickListener(v -> {
                dialog.dismiss();
                createTracker();
            });
            sheet.addView(create, new LinearLayout.LayoutParams(-1, -2));
        } else {
            for (Tracker tracker : trackers) {
                View item = selectionRow(tracker.name == null || tracker.name.trim().isEmpty() ? "Unbenannter Tracker" : tracker.name);
                item.setOnClickListener(v -> {
                    dialog.dismiss();
                    long sessionId = db.createSession(tracker.id);
                    if (sessionId == -1) {
                        Toast.makeText(activity, "Session konnte nicht angelegt werden", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    openSession(sessionId);
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

    private View selectionRow(String title) {
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

        showItem(session, tracker);
    }

    private void openTrackerEditor(long id, Tracker tracker, boolean isNew) {
        if (!isNew && db.readTracker(id) == null) {
            Toast.makeText(activity, "Tracker nicht gefunden", Toast.LENGTH_SHORT).show();
            backToTrackers.run();
            return;
        }

        base();
        root.addView(ui.appBar(isNew ? "Neuer Tracker" : "Tracker bearbeiten", false, null, !isNew, v -> showTrackerMenu(v, id, tracker.name)));

        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(ui.px(16), ui.px(16), ui.px(16), ui.px(16));
        scrollView.addView(body);

        TrackerEditorForm form = buildTrackerEditorForm(tracker);
        LinearLayout formCard = ui.contentCard();
        ui.addSectionHeader(formCard, null, "Grunddaten", null);

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

        final long[] trackerIdRef = new long[]{id};
        final Runnable[] persistRef = new Runnable[1];
        Runnable scheduleSave = () -> {
            if (persistRef[0] != null) {
                persistRef[0].run();
            }
        };
        persistRef[0] = () -> {
            try {
                String json = trackerEditorToJson(form);
                if (trackerIdRef[0] == -1) {
                    trackerIdRef[0] = TrackerJsonRepository.saveTracker(db, -1, json, true);
                    if (trackerIdRef[0] == -1) {
                        throw new IllegalStateException("Tracker konnte nicht gespeichert werden");
                    }
                } else {
                    TrackerJsonRepository.updateTracker(db, trackerIdRef[0], json);
                }
            } catch (Exception e) {
                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        if (tracker.items.isEmpty()) {
            LinearLayout empty = ui.contentCard();
            ui.addSectionHeader(empty, "ITEMS", "Items", null);

            itemsContainer.addView(empty);
        } else {
            for (Item item : tracker.items) {
                addItemEditor(itemsContainer, form.items, item, scheduleSave);
            }
        }

        addItem.setOnClickListener(v -> {
            addItemEditor(itemsContainer, form.items, null, scheduleSave);
            scheduleSave.run();
        });

        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(footerButton("Zurück", backToTrackers));
        attachTrackerAutosave(form, scheduleSave);
    }

    private void showItem(Session session, Tracker tracker) {
        base();
        Map<Long, Map<String, View>> inputsByItem = new LinkedHashMap<>();
        root.addView(ui.appBar(tracker.name == null || tracker.name.trim().isEmpty() ? "Session" : tracker.name,
                false, null, true, v -> showSessionMenu(v, session)));
        ScrollView scrollView = new ScrollView(activity);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(ui.px(16), ui.px(16), ui.px(16), ui.px(16));
        scrollView.addView(box);

        for (int itemIndex = 0; itemIndex < tracker.items.size(); itemIndex++) {
            Item item = tracker.items.get(itemIndex);
            Map<String, Object> values = initialValues(session, item);
            Map<String, View> inputs = new HashMap<>();
            inputsByItem.put(item.id, inputs);

            LinearLayout card = ui.contentCard();
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
            cardLp.bottomMargin = ui.px(20);

            ui.addSectionHeader(card, null, item.title, null);

            for (FieldDefinition field : item.fields) {
                fieldControl(card, field, values, inputs, false, () -> saveSessionItem(session, item, inputs));
            }

            box.addView(card, cardLp);
        }

        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(footerButton("Zurück", () -> {
            saveSessionItems(session, tracker, inputsByItem);
            clearTimers();
            backToSessions.run();
        }));
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
                value = db.previousValue(session.trackerId, item.id, field.key);
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
            boolean readOnly,
            Runnable onChange) {
        box.addView(ui.tv(field.label + (field.unit == null || field.unit.isEmpty() ? "" : " (" + field.unit + ")"), 16));

        Object value = values.get(field.key);
        if ("string".equals(field.type)) {
            EditText editText = new EditText(activity);
            editText.setText(value == null ? "" : String.valueOf(value));
            editText.setTextColor(theme.primaryTextColor());
            editText.setHintTextColor(theme.mutedTextColor());
            editText.setEnabled(!readOnly);
            if (!readOnly) {
                watchTextChange(editText, onChange);
            }
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
                tick(display, field.key, onChange);
                onChange.run();
            });
            stop.setOnClickListener(v -> {
                timers.remove(field.key);
                onChange.run();
            });
            reset.setOnClickListener(v -> {
                timers.remove(field.key);
                display.setTag(0L);
                display.setText(formatMs(0));
                onChange.run();
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
            onChange.run();
        };
        minus.setOnClickListener(adjust);
        plus.setOnClickListener(adjust);
        if (!readOnly) {
            watchTextChange(editText, onChange);
        }

        row.addView(minus);
        row.addView(editText, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(plus);
        box.addView(row);
        inputs.put(field.key, editText);
    }

    private void tick(TextView display, String key, Runnable onChange) {
        Long startedAt = timers.get(key);
        if (startedAt == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - startedAt;
        display.setTag(elapsed);
        display.setText(formatMs(elapsed));
        if (onChange != null) {
            onChange.run();
        }
        handler.postDelayed(() -> tick(display, key, onChange), 500);
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

    private void attachTrackerAutosave(TrackerEditorForm form, Runnable scheduleSave) {
        watchTextChange(form.nameInput, scheduleSave);
        watchTextChange(form.descriptionInput, scheduleSave);
    }

    private ItemEditorViews addItemEditor(LinearLayout container, List<ItemEditorViews> itemEditors, Item item, Runnable scheduleSave) {
        ItemEditorViews views = new ItemEditorViews();

        if (itemEditors.isEmpty() && container.getChildCount() == 1) {
            container.removeAllViews();
        }

        LinearLayout card = ui.contentCard();
        ui.addSectionHeader(card, null, item == null ? "Neues Item" : item.title, null);

        View drag = dragHandle();
        Button remove = ui.dangerButton("Entfernen");
        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.addView(new View(activity), new LinearLayout.LayoutParams(0, -2, 1));
        actionRow.addView(drag);
        actionRow.addView(remove);
        card.addView(actionRow);

        views.titleInput = labeledInput("Titel", item == null ? "" : item.title, InputType.TYPE_CLASS_TEXT);
        watchTextChange(views.titleInput, scheduleSave);
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
                addFieldEditor(fieldsContainer, views.fields, field, scheduleSave);
            }
        }

        addField.setOnClickListener(v -> {
            addFieldEditor(fieldsContainer, views.fields, null, scheduleSave);
            scheduleSave.run();
        });
        remove.setOnClickListener(v -> {
            container.removeView(card);
            views.removed = true;
            scheduleSave.run();
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
        configureItemDragTarget(container, card, itemEditors, scheduleSave);
        return views;
    }

    private FieldEditorViews addFieldEditor(LinearLayout container, List<FieldEditorViews> fieldEditors, FieldDefinition field, Runnable scheduleSave) {
        FieldEditorViews views = new FieldEditorViews();

        LinearLayout row = ui.contentCard();
        ui.addSectionHeader(row, null, field == null ? "Neues Field" : field.label, null);
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
        watchTextChange(views.keyInput, scheduleSave);
        watchTextChange(views.labelInput, scheduleSave);
        watchTextChange(views.defaultValueInput, scheduleSave);
        watchTextChange(views.unitInput, scheduleSave);
        watchTextChange(views.incrementInput, scheduleSave);
        watchTextChange(views.decimalsInput, scheduleSave);

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
        required.setOnCheckedChangeListener((buttonView, isChecked) -> scheduleSave.run());
        optionsRow.addView(required);
        row.addView(optionsRow);

        CheckBox prefill = new CheckBox(activity);
        prefill.setText("Prefill from previous");
        prefill.setChecked(field != null && field.prefillFromPrevious);
        prefill.setTextColor(theme.primaryTextColor());
        views.prefillCheck = prefill;
        prefill.setOnCheckedChangeListener((buttonView, isChecked) -> scheduleSave.run());
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
        typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateFieldEditorControls(views, selectedType(group));
            scheduleSave.run();
        });

        remove.setOnClickListener(v -> {
            container.removeView(row);
            views.removed = true;
            scheduleSave.run();
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
        configureFieldDragTarget(container, row, fieldEditors, scheduleSave);
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

    private void watchTextChange(EditText input, Runnable onChange) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onChange.run();
            }
        });
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

    private void configureItemDragTarget(LinearLayout container, View target, List<ItemEditorViews> itemEditors, Runnable onChange) {
        target.setOnDragListener((v, event) -> handleDrop(container, target, itemEditors, event, onChange));
        container.setOnDragListener((v, event) -> handleContainerDrop(container, itemEditors, event, onChange));
    }

    private void configureFieldDragTarget(LinearLayout container, View target, List<FieldEditorViews> fieldEditors, Runnable onChange) {
        target.setOnDragListener((v, event) -> handleDrop(container, target, fieldEditors, event, onChange));
        container.setOnDragListener((v, event) -> handleContainerDrop(container, fieldEditors, event, onChange));
    }

    private boolean handleDrop(LinearLayout container, View target, List<?> list, android.view.DragEvent event, Runnable onChange) {
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
            if (onChange != null) {
                onChange.run();
            }
            return true;
        }

        return true;
    }

    private boolean handleContainerDrop(LinearLayout container, List<?> list, android.view.DragEvent event, Runnable onChange) {
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
            if (onChange != null) {
                onChange.run();
            }
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

    private void saveSessionItem(Session session, Item item, Map<String, View> inputs) {
        db.saveRecord(session, item.id, readInputs(item, inputs));
    }

    private void saveSessionItems(Session session, Tracker tracker, Map<Long, Map<String, View>> inputsByItem) {
        for (Item item : tracker.items) {
            Map<String, View> inputs = inputsByItem.get(item.id);
            if (inputs != null) {
                db.saveRecord(session, item.id, readInputs(item, inputs));
            }
        }
    }

    private void showTrackerMenu(View anchor, long trackerId, String trackerName) {
        PopupMenu menu = new PopupMenu(activity, anchor, Gravity.END);
        if (trackerId != -1) {
            menu.getMenu().add(0, 1, 0, "Tracker löschen");
        }
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                confirmDeleteTracker(trackerId, trackerName);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showSessionMenu(View anchor, Session session) {
        PopupMenu menu = new PopupMenu(activity, anchor, Gravity.END);
        menu.getMenu().add(0, 2, 0, "Session löschen");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 2) {
                confirmDeleteSession(session.id);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void confirmDeleteTracker(long trackerId, String trackerName) {
        new android.app.AlertDialog.Builder(activity)
                .setTitle("Tracker löschen")
                .setMessage((trackerName == null || trackerName.trim().isEmpty() ? "Diesen Tracker" : trackerName) + " wirklich löschen?")
                .setPositiveButton("Löschen", (dialog, which) -> {
                    db.deleteTracker(trackerId);
                    clearTimers();
                    backToTrackers.run();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void confirmDeleteSession(long sessionId) {
        new android.app.AlertDialog.Builder(activity)
                .setTitle("Session löschen")
                .setMessage("Diese Session wirklich löschen?")
                .setPositiveButton("Löschen", (dialog, which) -> {
                    db.deleteSession(sessionId);
                    clearTimers();
                    backToSessions.run();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private LinearLayout footerButton(String text, Runnable onClick) {
        LinearLayout footer = new LinearLayout(activity);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(ui.px(16), ui.px(8), ui.px(16), ui.px(16));

        Button button = ui.secondaryButton(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        button.setLayoutParams(lp);
        button.setOnClickListener(v -> {
            if (onClick != null) {
                onClick.run();
            }
        });
        footer.addView(button);
        return footer;
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

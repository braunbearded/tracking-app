package com.zaelio.app;

import android.app.Activity;
import android.content.ClipData;
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

import com.zaelio.app.theme.ThemeStore;
import com.zaelio.app.ui.AppUi;

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
    private final FieldInputUi fieldInputUi;
    private LinearLayout root;
    private View dropIndicator;

    public TrackerFlowUi(Activity activity, TrackingDatabase db, ThemeStore theme, AppUi ui,
                         Handler handler, Runnable backToSessions, Runnable backToTrackers) {
        this.activity = activity;
        this.db = db;
        this.theme = theme;
        this.ui = ui;
        this.handler = handler;
        this.backToSessions = backToSessions;
        this.backToTrackers = backToTrackers;
        this.fieldInputUi = new FieldInputUi(activity, theme, ui, handler, timers);
    }

    public void clearTimers() {
        timers.clear();
    }

    public void chooseTracker() {
        List<Tracker> trackers = db.trackers();
        base();
        root.addView(ui.appBar("Tracker auswählen", true, backToSessions, false, null));

        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(ui.px(16), ui.px(12), ui.px(16), ui.px(16));
        scrollView.addView(box);

        TextView intro = new TextView(activity);
        intro.setText("Wähle einen Tracker für die neue Session.");
        intro.setTextSize(ui.sp(14));
        intro.setTextColor(theme.secondaryTextColor());
        intro.setPadding(ui.px(4), ui.px(4), ui.px(4), ui.px(12));
        box.addView(intro);

        if (trackers.isEmpty()) {
            Button create = ui.primaryButton("Neuen Tracker anlegen");
            create.setOnClickListener(v -> createTracker());
            box.addView(create, new LinearLayout.LayoutParams(-1, -2));
        } else {
            for (Tracker tracker : trackers) {
                View item = selectionRow(tracker.name == null || tracker.name.trim().isEmpty() ? "Unbenannter Tracker" : tracker.name);
                item.setOnClickListener(v -> {
                    long sessionId = db.createSession(tracker.id);
                    if (sessionId == -1) {
                        Toast.makeText(activity, "Session konnte nicht angelegt werden", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    openSession(sessionId);
                });
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                rowLp.bottomMargin = ui.px(10);
                box.addView(item, rowLp);
            }
        }

        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
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
                fieldInputUi.fieldControl(card, field, values, inputs, false, () -> saveSessionItem(session, item, inputs));
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
        Map<String, Object> values = new LinkedHashMap<>();
        for (FieldDefinition field : item.fields) {
            if (records.containsKey(field.id)) {
                values.putAll(JsonUtil.toMap(records.get(field.id).valuesJson));
                continue;
            }

            Object value = TrackingDatabase.NO_PREVIOUS;
            if (field.prefillFromPrevious) {
                value = db.previousValue(session.trackerId, field.id, field.key);
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
        Button duplicate = ui.secondaryButton("Duplizieren");
        Button remove = ui.dangerButton("Entfernen");
        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.addView(drag);
        actionRow.addView(new View(activity), new LinearLayout.LayoutParams(0, -2, 1));
        actionRow.addView(duplicate);
        actionRow.addView(remove);
        card.addView(actionRow);

        views.titleInput = labeledInput("Titel", item == null ? "" : item.title, InputType.TYPE_CLASS_TEXT);
        watchTextChange(views.titleInput, scheduleSave);
        card.addView(views.titleInput);

        LinearLayout fieldsHeader = new LinearLayout(activity);
        fieldsHeader.setOrientation(LinearLayout.HORIZONTAL);
        fieldsHeader.setPadding(0, ui.px(8), 0, ui.px(8));
        TextView fieldsTitle = ui.tv("Felder", 16);
        fieldsTitle.setPadding(0, 0, 0, 0);
        fieldsHeader.addView(fieldsTitle, new LinearLayout.LayoutParams(0, -2, 1));

        Button addField = ui.primaryButton("Feld hinzufügen");
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
        } else {
            addFieldEditor(fieldsContainer, views.fields, null, scheduleSave);
        }

        addField.setOnClickListener(v -> {
            addFieldEditor(fieldsContainer, views.fields, null, scheduleSave);
            scheduleSave.run();
        });
        duplicate.setOnClickListener(v -> {
            addItemEditor(container, itemEditors, itemFromViews(views), scheduleSave);
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
        ui.addSectionHeader(row, null, field == null ? "Neues Feld" : field.label, null);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.bottomMargin = ui.px(12);

        View drag = dragHandle();
        Button moveUp = ui.secondaryButton("↑");
        Button moveDown = ui.secondaryButton("↓");
        Button duplicate = ui.secondaryButton("Duplizieren");
        Button remove = ui.dangerButton("Entfernen");
        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);
        actionRow.addView(drag);
        actionRow.addView(new View(activity), new LinearLayout.LayoutParams(0, -2, 1));
        actionRow.addView(moveUp, compactActionButtonLp());
        actionRow.addView(moveDown, compactActionButtonLp());
        actionRow.addView(duplicate);
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

        row.addView(views.labelInput);
        row.addView(views.defaultValueInput);

        RadioGroup typeGroup = new RadioGroup(activity);
        typeGroup.setOrientation(RadioGroup.VERTICAL);
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

        moveUp.setOnClickListener(v -> {
            moveFieldEditor(container, fieldEditors, views, -1);
            scheduleSave.run();
        });
        moveDown.setOnClickListener(v -> {
            moveFieldEditor(container, fieldEditors, views, 1);
            scheduleSave.run();
        });
        duplicate.setOnClickListener(v -> {
            addFieldEditor(container, fieldEditors, fieldFromViews(views), scheduleSave);
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

    private Item itemFromViews(ItemEditorViews views) {
        Item item = new Item();
        item.title = views.titleInput.getText().toString();
        for (FieldEditorViews fieldViews : views.fields) {
            if (!fieldViews.removed) {
                item.fields.add(fieldFromViews(fieldViews));
            }
        }
        return item;
    }

    private FieldDefinition fieldFromViews(FieldEditorViews views) {
        FieldDefinition field = new FieldDefinition();
        field.key = views.keyInput.getText().toString();
        field.label = views.labelInput.getText().toString();
        field.defaultValue = views.defaultValueInput.getText().toString();
        field.unit = views.unitInput.getText().toString();
        field.increment = parseDoubleSafe(views.incrementInput.getText().toString(), 1);
        field.decimals = parseIntSafe(views.decimalsInput.getText().toString(), 1);
        field.type = selectedType(views.typeGroup);
        field.inputSize = "standard";
        field.required = views.requiredCheck.isChecked();
        field.prefillFromPrevious = views.prefillCheck.isChecked();
        return field;
    }

    private LinearLayout.LayoutParams compactActionButtonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ui.px(48), ui.px(48));
        lp.rightMargin = ui.px(8);
        return lp;
    }

    private void moveFieldEditor(LinearLayout container, List<FieldEditorViews> fieldEditors, FieldEditorViews views, int direction) {
        if (views == null || views.row == null) {
            return;
        }

        int fromIndex = container.indexOfChild(views.row);
        int toIndex = fromIndex + direction;
        if (fromIndex < 0 || toIndex < 0 || toIndex >= container.getChildCount()) {
            return;
        }

        container.removeView(views.row);
        container.addView(views.row, toIndex);

        int listFromIndex = fieldEditors.indexOf(views);
        int listToIndex = listFromIndex + direction;
        if (listFromIndex >= 0 && listToIndex >= 0 && listToIndex < fieldEditors.size()) {
            fieldEditors.remove(listFromIndex);
            fieldEditors.add(listToIndex, views);
        }
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
        List<String> usedFieldKeys = new ArrayList<>();
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

                String itemTitle = itemViews.titleInput.getText().toString().trim();
                String fieldLabel = fieldViews.labelInput.getText().toString().trim();
                if (fieldLabel.isEmpty()) {
                    fieldLabel = itemTitle.isEmpty() ? "Field " + (fieldOrder + 1) : itemTitle;
                }
                String fieldKey = uniqueFieldKey(fieldLabel, usedFieldKeys);

                JSONObject field = new JSONObject();
                field.put("key", fieldKey);
                field.put("label", fieldLabel);
                field.put("type", selectedType(fieldViews.typeGroup));
                field.put("order", fieldOrder++);

                String defaultValue = fieldViews.defaultValueInput.getText().toString().trim();
                field.put("defaultValue", defaultValue.isEmpty() ? JSONObject.NULL : defaultValue);
                field.put("increment", parseDoubleSafe(fieldViews.incrementInput.getText().toString(), 1));
                field.put("decimals", parseIntSafe(fieldViews.decimalsInput.getText().toString(), 1));
                field.put("unit", fieldViews.unitInput.getText().toString().trim());
                field.put("inputSize", "standard");
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

    private String uniqueFieldKey(String label, List<String> usedKeys) {
        String base = label.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.isEmpty()) {
            base = "field";
        }

        String candidate = base;
        int suffix = 2;
        while (usedKeys.contains(candidate)) {
            candidate = base + "_" + suffix++;
        }
        usedKeys.add(candidate);
        return candidate;
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
        return selectedRadioValue(group, "string");
    }

    private String selectedRadioValue(RadioGroup group, String fallback) {
        int checkedId = group.getCheckedRadioButtonId();
        if (checkedId == -1) {
            return fallback;
        }
        RadioButton checked = group.findViewById(checkedId);
        Object tag = checked == null ? null : checked.getTag();
        return tag == null ? fallback : String.valueOf(tag);
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
        view.setAlpha(0.35f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view.startDragAndDrop(data, shadow, view, 0);
        } else {
            view.startDrag(data, shadow, view, 0);
        }
    }

    private void configureItemDragTarget(LinearLayout container, View target, List<ItemEditorViews> itemEditors, Runnable onChange) {
        View.OnDragListener listener = (v, event) -> handleDrop(container, target, itemEditors, v, event, onChange);
        setDragListenerDeep(target, listener);
        container.setOnDragListener((v, event) -> handleContainerDrop(container, itemEditors, v, event, onChange));
    }

    private void configureFieldDragTarget(LinearLayout container, View target, List<FieldEditorViews> fieldEditors, Runnable onChange) {
        View.OnDragListener listener = (v, event) -> handleDrop(container, target, fieldEditors, v, event, onChange);
        setDragListenerDeep(target, listener);
        container.setOnDragListener((v, event) -> handleContainerDrop(container, fieldEditors, v, event, onChange));
    }

    private void setDragListenerDeep(View view, View.OnDragListener listener) {
        view.setOnDragListener(listener);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setDragListenerDeep(group.getChildAt(i), listener);
            }
        }
    }

    private boolean handleDrop(LinearLayout container, View target, List<?> list, View eventView, android.view.DragEvent event, Runnable onChange) {
        if (!(event.getLocalState() instanceof View)) {
            return false;
        }

        if (event.getAction() == android.view.DragEvent.ACTION_DRAG_LOCATION
                || event.getAction() == android.view.DragEvent.ACTION_DRAG_ENTERED) {
            View dragged = (View) event.getLocalState();
            autoScrollDuringDrag(eventView, container, event);
            if (dragged.getParent() == container) {
                showDropIndicatorBefore(container, target);
            }
            return true;
        }
        if (event.getAction() == android.view.DragEvent.ACTION_DRAG_ENDED) {
            restoreDraggedView(event);
            hideDropIndicator();
            return true;
        }
        if (event.getAction() == android.view.DragEvent.ACTION_DRAG_STARTED
                || event.getAction() == android.view.DragEvent.ACTION_DRAG_EXITED) {
            return true;
        }

        View dragged = (View) event.getLocalState();
        if (event.getAction() == android.view.DragEvent.ACTION_DROP) {
            restoreDraggedView(event);
            hideDropIndicator();
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

    private boolean handleContainerDrop(LinearLayout container, List<?> list, View eventView, android.view.DragEvent event, Runnable onChange) {
        if (!(event.getLocalState() instanceof View)) {
            return false;
        }

        if (event.getAction() == android.view.DragEvent.ACTION_DRAG_LOCATION
                || event.getAction() == android.view.DragEvent.ACTION_DRAG_ENTERED) {
            View dragged = (View) event.getLocalState();
            autoScrollDuringDrag(eventView, container, event);
            if (dragged.getParent() == container) {
                showDropIndicator(container, container.getChildCount());
            }
            return true;
        }
        if (event.getAction() == android.view.DragEvent.ACTION_DRAG_ENDED) {
            restoreDraggedView(event);
            hideDropIndicator();
            return true;
        }
        if (event.getAction() == android.view.DragEvent.ACTION_DRAG_STARTED
                || event.getAction() == android.view.DragEvent.ACTION_DRAG_EXITED) {
            return true;
        }

        View dragged = (View) event.getLocalState();
        if (event.getAction() == android.view.DragEvent.ACTION_DROP) {
            restoreDraggedView(event);
            hideDropIndicator();
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

    private void restoreDraggedView(android.view.DragEvent event) {
        if (event.getLocalState() instanceof View) {
            ((View) event.getLocalState()).setAlpha(1f);
        }
    }

    private void showDropIndicatorBefore(LinearLayout container, View target) {
        hideDropIndicator();
        showDropIndicator(container, container.indexOfChild(target));
    }

    private void showDropIndicator(LinearLayout container, int index) {
        if (container == null) {
            return;
        }
        if (dropIndicator == null) {
            dropIndicator = new View(activity);
            dropIndicator.setBackground(ui.makeRoundedCard(theme.accentSoftColor(), theme.accentColor()));
        }

        ViewParent parent = dropIndicator.getParent();
        if (parent instanceof LinearLayout) {
            ((LinearLayout) parent).removeView(dropIndicator);
        }

        int safeIndex = Math.max(0, Math.min(index, container.getChildCount()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, ui.px(28));
        lp.leftMargin = ui.px(8);
        lp.rightMargin = ui.px(8);
        lp.topMargin = ui.px(6);
        lp.bottomMargin = ui.px(14);
        container.addView(dropIndicator, safeIndex, lp);
    }

    private void hideDropIndicator() {
        if (dropIndicator == null) {
            return;
        }
        ViewParent parent = dropIndicator.getParent();
        if (parent instanceof LinearLayout) {
            ((LinearLayout) parent).removeView(dropIndicator);
        }
    }

    private void autoScrollDuringDrag(View eventView, View scrollAnchor, android.view.DragEvent event) {
        ScrollView scrollView = findParentScrollView(scrollAnchor);
        if (scrollView == null || eventView == null) {
            return;
        }

        int[] eventViewLocation = new int[2];
        int[] scrollViewLocation = new int[2];
        eventView.getLocationOnScreen(eventViewLocation);
        scrollView.getLocationOnScreen(scrollViewLocation);

        int pointerY = eventViewLocation[1] + Math.round(event.getY());
        int topEdge = scrollViewLocation[1] + ui.px(72);
        int bottomEdge = scrollViewLocation[1] + scrollView.getHeight() - ui.px(72);
        int step = ui.px(18);

        if (pointerY < topEdge) {
            scrollView.smoothScrollBy(0, -step);
        } else if (pointerY > bottomEdge) {
            scrollView.smoothScrollBy(0, step);
        }
    }

    private ScrollView findParentScrollView(View view) {
        ViewParent parent = view == null ? null : view.getParent();
        while (parent != null) {
            if (parent instanceof ScrollView) {
                return (ScrollView) parent;
            }
            if (parent instanceof View) {
                parent = ((View) parent).getParent();
            } else {
                break;
            }
        }
        return null;
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
        Map<String, Object> values = readInputs(item, inputs);
        for (FieldDefinition field : item.fields) {
            Map<String, Object> fieldValue = new LinkedHashMap<>();
            fieldValue.put(field.key, values.get(field.key));
            db.saveRecord(session, field.id, fieldValue);
        }
    }

    private void saveSessionItems(Session session, Tracker tracker, Map<Long, Map<String, View>> inputsByItem) {
        for (Item item : tracker.items) {
            Map<String, View> inputs = inputsByItem.get(item.id);
            if (inputs != null) {
                saveSessionItem(session, item, inputs);
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

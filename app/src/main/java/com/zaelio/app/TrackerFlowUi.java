package com.zaelio.app;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Handler;
import android.text.InputType;
import android.text.Editable;
import android.widget.Filter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.zaelio.app.theme.ThemeStore;
import com.zaelio.app.ui.AppUi;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
    private final BackActionSetter setBackAction;
    private final Map<String, Long> timers = new HashMap<>();
    private final FieldInputUi fieldInputUi;
    private LinearLayout root;

    public TrackerFlowUi(Activity activity, TrackingDatabase db, ThemeStore theme, AppUi ui,
                         Handler handler, Runnable backToSessions, Runnable backToTrackers,
                         BackActionSetter setBackAction) {
        this.activity = activity;
        this.db = db;
        this.theme = theme;
        this.ui = ui;
        this.handler = handler;
        this.backToSessions = backToSessions;
        this.backToTrackers = backToTrackers;
        this.setBackAction = setBackAction;
        this.fieldInputUi = new FieldInputUi(activity, theme, ui, handler, timers);
    }

    public void clearTimers() {
        timers.clear();
    }

    public void chooseTracker() {
        List<Tracker> trackers = db.trackers();
        setBackAction.accept(backToSessions);
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

        setBackAction.accept(backToTrackers);
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
        formCard.setPadding(ui.px(12), ui.px(8), ui.px(8), ui.px(8));
        ui.addSectionHeader(formCard, null, "Grunddaten", null);

        formCard.addView(outlinedInput("Tracker-Name", form.nameInput));
        TextInputLayout descriptionInput = outlinedInput("Beschreibung", form.descriptionInput);
        ((LinearLayout.LayoutParams) descriptionInput.getLayoutParams()).bottomMargin = ui.px(4);
        formCard.addView(descriptionInput);
        body.addView(formCard);

        LinearLayout itemsHeader = new LinearLayout(activity);
        itemsHeader.setOrientation(LinearLayout.HORIZONTAL);
        itemsHeader.setGravity(Gravity.CENTER_VERTICAL);
        itemsHeader.setPadding(ui.px(12), ui.px(8), ui.px(8), ui.px(8));
        itemsHeader.setBackground(ui.makeRoundedCard(theme.surfaceAltColor(), theme.borderColor()));

        TextView itemsTitle = new TextView(activity);
        itemsTitle.setText("Items");
        itemsTitle.setTextSize(ui.sp(16));
        itemsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        itemsTitle.setTextColor(theme.primaryTextColor());
        itemsHeader.addView(itemsTitle);

        TextView itemsCount = new TextView(activity);
        itemsCount.setTextSize(ui.sp(12));
        itemsCount.setTextColor(theme.accentColor());
        itemsCount.setGravity(Gravity.CENTER);
        itemsCount.setPadding(ui.px(8), ui.px(2), ui.px(8), ui.px(2));
        itemsCount.setBackground(ui.makeRoundedCard(theme.accentSoftColor(), theme.accentSoftColor()));
        LinearLayout.LayoutParams itemCountLp = new LinearLayout.LayoutParams(-2, -2);
        itemCountLp.leftMargin = ui.px(8);
        itemsHeader.addView(itemsCount, itemCountLp);
        itemsHeader.addView(new View(activity), new LinearLayout.LayoutParams(0, 1, 1));

        Button addItem = ui.primaryButton("Item hinzufügen");
        itemsHeader.addView(addItem, new LinearLayout.LayoutParams(-2, ui.px(48)));
        LinearLayout.LayoutParams itemsHeaderLp = new LinearLayout.LayoutParams(-1, -2);
        itemsHeaderLp.topMargin = ui.px(12);
        itemsHeaderLp.bottomMargin = ui.px(12);
        body.addView(itemsHeader, itemsHeaderLp);

        LinearLayout itemsContainer = new LinearLayout(activity);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        body.addView(itemsContainer, new LinearLayout.LayoutParams(-1, -2));

        final Runnable[] updateItemsHeaderRef = new Runnable[1];
        updateItemsHeaderRef[0] = () -> {
            int count = 0;
            for (ItemEditorViews itemViews : form.items) {
                if (!itemViews.removed) {
                    count++;
                }
            }
            itemsCount.setText(String.valueOf(count));
        };

        final long[] trackerIdRef = new long[]{id};
        final Runnable[] persistRef = new Runnable[1];
        Runnable scheduleSave = () -> {
            if (updateItemsHeaderRef[0] != null) {
                updateItemsHeaderRef[0].run();
            }
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

        if (!tracker.items.isEmpty()) {
            for (Item item : tracker.items) {
                addItemEditor(itemsContainer, form.items, item, scheduleSave);
            }
        }
        updateItemsHeaderRef[0].run();

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

        Runnable back = () -> {
            saveSessionItems(session, tracker, inputsByItem);
            clearTimers();
            backToSessions.run();
        };
        setBackAction.accept(back);
        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(footerButton("Zurück", back));
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
        card.setPadding(ui.px(12), ui.px(8), ui.px(8), ui.px(8));
        View reorder = reorderHandle();
        TextView menu = iconAction("⋮");
        ImageView expand = expandAction();

        views.titleInput = labeledInput("Item-Name", item == null ? "" : item.title, InputType.TYPE_CLASS_TEXT);

        LinearLayout summaryRow = new LinearLayout(activity);
        summaryRow.setOrientation(LinearLayout.HORIZONTAL);
        summaryRow.setGravity(Gravity.CENTER_VERTICAL);
        summaryRow.setMinimumHeight(ui.px(56));
        LinearLayout summaryText = new LinearLayout(activity);
        summaryText.setOrientation(LinearLayout.VERTICAL);
        views.summaryTitle = new TextView(activity);
        views.summaryTitle.setTextSize(ui.sp(16));
        views.summaryTitle.setTypeface(Typeface.DEFAULT_BOLD);
        views.summaryTitle.setTextColor(theme.primaryTextColor());
        views.summaryMeta = new TextView(activity);
        views.summaryMeta.setTextSize(ui.sp(13));
        views.summaryMeta.setTextColor(theme.secondaryTextColor());
        summaryText.addView(views.summaryTitle);
        summaryText.addView(views.summaryMeta);
        views.summaryText = summaryText;
        views.summaryInput = outlinedInput("Item-Name", views.titleInput);
        FrameLayout summarySlot = new FrameLayout(activity);
        summarySlot.addView(summaryText, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL));
        summarySlot.addView(views.summaryInput, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL));
        summaryRow.addView(reorder, new LinearLayout.LayoutParams(ui.px(28), ui.px(56)));
        summaryRow.addView(summarySlot, new LinearLayout.LayoutParams(0, -2, 1));
        summaryRow.addView(menu, iconActionLp());
        summaryRow.addView(expand, iconActionLp());
        card.addView(summaryRow);
        expandReorderTouchArea(card, summaryRow, reorder);

        LinearLayout editor = new LinearLayout(activity);
        editor.setOrientation(LinearLayout.VERTICAL);
        views.editor = editor;

        LinearLayout fieldsHeader = new LinearLayout(activity);
        fieldsHeader.setOrientation(LinearLayout.HORIZONTAL);
        fieldsHeader.setGravity(Gravity.CENTER_VERTICAL);
        fieldsHeader.setPadding(ui.px(12), ui.px(8), ui.px(8), ui.px(8));
        fieldsHeader.setBackground(ui.makeRoundedCard(theme.surfaceAltColor(), theme.borderColor()));

        TextView fieldsTitle = new TextView(activity);
        fieldsTitle.setText("Felder");
        fieldsTitle.setTextSize(ui.sp(16));
        fieldsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        fieldsTitle.setTextColor(theme.primaryTextColor());
        fieldsHeader.addView(fieldsTitle);

        views.fieldsCount = new TextView(activity);
        views.fieldsCount.setTextSize(ui.sp(12));
        views.fieldsCount.setTextColor(theme.accentColor());
        views.fieldsCount.setGravity(Gravity.CENTER);
        views.fieldsCount.setPadding(ui.px(8), ui.px(2), ui.px(8), ui.px(2));
        views.fieldsCount.setBackground(ui.makeRoundedCard(theme.accentSoftColor(), theme.accentSoftColor()));
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(-2, -2);
        countLp.leftMargin = ui.px(8);
        fieldsHeader.addView(views.fieldsCount, countLp);
        fieldsHeader.addView(new View(activity), new LinearLayout.LayoutParams(0, 1, 1));

        Button addField = ui.primaryButton("Feld hinzufügen");
        fieldsHeader.addView(addField, new LinearLayout.LayoutParams(-2, ui.px(48)));
        LinearLayout.LayoutParams fieldsHeaderLp = new LinearLayout.LayoutParams(-1, -2);
        fieldsHeaderLp.topMargin = ui.px(12);
        fieldsHeaderLp.bottomMargin = ui.px(12);
        editor.addView(fieldsHeader, fieldsHeaderLp);

        LinearLayout fieldsContainer = new LinearLayout(activity);
        fieldsContainer.setOrientation(LinearLayout.VERTICAL);
        editor.addView(fieldsContainer);
        views.fieldsContainer = fieldsContainer;
        card.addView(editor);

        Runnable itemChanged = () -> {
            updateItemSummary(views);
            scheduleSave.run();
        };
        if (item != null) {
            for (FieldDefinition field : item.fields) {
                addFieldEditor(fieldsContainer, views.fields, field, itemChanged);
            }
        } else {
            addFieldEditor(fieldsContainer, views.fields, null, itemChanged);
        }

        watchTextChange(views.titleInput, itemChanged);
        addField.setOnClickListener(v -> {
            addFieldEditor(fieldsContainer, views.fields, null, itemChanged);
            itemChanged.run();
        });
        final LinearLayout[] shellRef = new LinearLayout[1];
        Runnable duplicateAction = () -> {
            addItemEditor(container, itemEditors, itemFromViews(views), scheduleSave);
            scheduleSave.run();
        };
        Runnable removeAction = () -> {
            container.removeView(shellRef[0]);
            updateChildBottomMargins(container, 12, 4);
            views.removed = true;
            scheduleSave.run();
        };
        menu.setOnClickListener(v -> showItemMenu(v, duplicateAction, removeAction));
        View.OnClickListener toggle = v -> toggleItemEditor(views, expand);
        expand.setOnClickListener(toggle);
        summaryRow.setOnClickListener(toggle);
        summarySlot.setOnClickListener(toggle);
        summaryText.setOnClickListener(toggle);
        views.summaryTitle.setOnClickListener(toggle);
        views.summaryMeta.setOnClickListener(toggle);
        LinearLayout shell = reorderShell(reorder, card);
        shellRef[0] = shell;

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = ui.px(12);
        container.addView(shell, lp);
        updateChildBottomMargins(container, 12, 4);
        views.card = shell;
        views.container = container;
        itemEditors.add(views);
        shell.setTag(views);
        attachItemReorder(reorder, container, itemEditors, views, scheduleSave);
        updateItemSummary(views);
        setItemExpanded(views, expand, item == null);
        return views;
    }

    private void showItemMenu(View anchor, Runnable duplicateAction, Runnable removeAction) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        menu.getMenu().add(0, 1, 0, "Kopieren");
        menu.getMenu().add(0, 2, 1, "Löschen");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                duplicateAction.run();
            } else if (item.getItemId() == 2) {
                removeAction.run();
            }
            return true;
        });
        menu.show();
    }

    private void toggleItemEditor(ItemEditorViews views, ImageView expand) {
        setItemExpanded(views, expand, views.editor.getVisibility() != View.VISIBLE);
    }

    private void setItemExpanded(ItemEditorViews views, ImageView expand, boolean expanded) {
        views.summaryText.setVisibility(expanded ? View.GONE : View.VISIBLE);
        views.summaryInput.setVisibility(expanded ? View.VISIBLE : View.GONE);
        views.editor.setVisibility(expanded ? View.VISIBLE : View.GONE);
        expand.setRotation(expanded ? 180f : 0f);
    }

    private void updateItemSummary(ItemEditorViews views) {
        String title = views.titleInput.getText().toString().trim();
        views.summaryTitle.setText(title.isEmpty() ? "Neues Item" : title);
        int count = 0;
        for (FieldEditorViews fieldViews : views.fields) {
            if (!fieldViews.removed) {
                count++;
            }
        }
        String countText = count == 1 ? "1 Feld" : count + " Felder";
        views.summaryMeta.setText(countText);
        if (views.fieldsCount != null) {
            views.fieldsCount.setText(String.valueOf(count));
        }
    }

    private FieldEditorViews addFieldEditor(LinearLayout container, List<FieldEditorViews> fieldEditors, FieldDefinition field, Runnable scheduleSave) {
        FieldEditorViews views = new FieldEditorViews();

        View reorder = reorderHandle();
        TextView menu = iconAction("⋮");
        ImageView expand = expandAction();

        views.keyInput = labeledInput("Key", field == null ? "" : field.key, InputType.TYPE_CLASS_TEXT);
        views.labelInput = labeledInput("Feldname", field == null ? "" : field.label, InputType.TYPE_CLASS_TEXT);
        views.defaultValueInput = labeledInput("Standardwert", field == null ? "" : String.valueOf(field.defaultValue == null ? "" : field.defaultValue), InputType.TYPE_CLASS_TEXT);
        views.unitInput = labeledInput("Einheit", field == null ? "" : field.unit, InputType.TYPE_CLASS_TEXT);
        views.incrementInput = labeledInput("Schrittweite", field == null ? "1" : String.valueOf(field.increment), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        views.decimalsInput = labeledInput("Nachkommastellen", field == null ? "1" : String.valueOf(field.decimals), InputType.TYPE_CLASS_NUMBER);

        TextInputLayout typeLayout = new TextInputLayout(activity);
        typeLayout.setHint("Typ");
        typeLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        typeLayout.setBoxBackgroundColor(theme.surfaceColor());
        typeLayout.setBoxStrokeColor(theme.accentColor());
        typeLayout.setBoxStrokeColorStateList(inputBorderStateList());
        typeLayout.setBoxStrokeWidth(ui.px(1));
        typeLayout.setBoxStrokeWidthFocused(ui.px(2));
        typeLayout.setHintTextColor(inputHintStateList());
        typeLayout.setBoxCornerRadii(ui.px(6), ui.px(6), ui.px(6), ui.px(6));
        typeLayout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        LinearLayout.LayoutParams typeLp = new LinearLayout.LayoutParams(-1, -2);
        typeLp.bottomMargin = ui.px(4);
        typeLayout.setLayoutParams(typeLp);

        MaterialAutoCompleteTextView typeInput = new MaterialAutoCompleteTextView(activity);
        String[] typeLabels = {"Text", "Ganzzahl", "Dezimalzahl", "Timer"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_dropdown_item_1line, typeLabels) {
            private final Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = typeLabels;
                    results.count = typeLabels.length;
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }
            };

            @Override
            public Filter getFilter() {
                return filter;
            }
        };
        typeInput.setAdapter(typeAdapter);
        typeInput.setThreshold(0);
        typeInput.setOnClickListener(v -> typeInput.showDropDown());
        typeInput.setText(typeLabels[typeIndex(field == null ? null : field.type)], false);
        typeInput.setInputType(0);
        typeInput.setTextColor(theme.primaryTextColor());
        typeInput.setHintTextColor(inputHintStateList());
        typeInput.setBackgroundTintList(inputBorderStateList());
        tintCursor(typeInput);
        typeInput.setPadding(ui.px(12), 0, ui.px(12), 0);
        typeLayout.addView(typeInput, new LinearLayout.LayoutParams(-1, ui.px(56)));
        views.typeInput = typeInput;

        MaterialCheckBox required = new MaterialCheckBox(activity);
        required.setText("Pflichtfeld");
        required.setChecked(field != null && field.required);
        styleCheckBox(required);
        views.requiredCheck = required;

        MaterialCheckBox prefill = new MaterialCheckBox(activity);
        prefill.setText("Vorherigen Wert übernehmen");
        prefill.setChecked(field != null && field.prefillFromPrevious);
        styleCheckBox(prefill);
        views.prefillCheck = prefill;

        LinearLayout numericRow = new LinearLayout(activity);
        numericRow.setOrientation(LinearLayout.HORIZONTAL);
        numericRow.setWeightSum(2);
        TextInputLayout incrementWrap = outlinedInput("Schrittweite", views.incrementInput);
        TextInputLayout decimalsWrap = outlinedInput("Nachkommastellen", views.decimalsInput);
        LinearLayout.LayoutParams incrementLp = new LinearLayout.LayoutParams(0, -2, 1);
        incrementLp.rightMargin = ui.px(8);
        numericRow.addView(incrementWrap, incrementLp);
        numericRow.addView(decimalsWrap, new LinearLayout.LayoutParams(0, -2, 1));
        views.incrementWrap = incrementWrap;
        views.decimalsWrap = decimalsWrap;
        views.numericRow = numericRow;

        LinearLayout editor = new LinearLayout(activity);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(0, ui.px(12), 0, 0);
        editor.addView(outlinedInput("Standardwert", views.defaultValueInput));
        editor.addView(typeLayout);
        editor.addView(required);
        editor.addView(prefill);
        TextInputLayout unitInput = outlinedInput("Einheit", views.unitInput);
        ((LinearLayout.LayoutParams) unitInput.getLayoutParams()).bottomMargin = ui.px(4);
        editor.addView(unitInput);
        editor.addView(numericRow);
        views.editor = editor;

        LinearLayout row = ui.contentCard();
        row.setPadding(ui.px(12), ui.px(8), ui.px(8), ui.px(8));
        LinearLayout summaryRow = new LinearLayout(activity);
        summaryRow.setOrientation(LinearLayout.HORIZONTAL);
        summaryRow.setGravity(Gravity.CENTER_VERTICAL);
        summaryRow.setMinimumHeight(ui.px(56));
        LinearLayout summaryText = new LinearLayout(activity);
        summaryText.setOrientation(LinearLayout.VERTICAL);
        views.summaryTitle = new TextView(activity);
        views.summaryTitle.setTextSize(ui.sp(16));
        views.summaryTitle.setTypeface(Typeface.DEFAULT_BOLD);
        views.summaryTitle.setTextColor(theme.primaryTextColor());
        views.summaryMeta = new TextView(activity);
        views.summaryMeta.setTextSize(ui.sp(13));
        views.summaryMeta.setTextColor(theme.secondaryTextColor());
        summaryText.addView(views.summaryTitle);
        summaryText.addView(views.summaryMeta);
        views.summaryText = summaryText;
        views.summaryInput = outlinedInput("Feldname", views.labelInput);
        FrameLayout summarySlot = new FrameLayout(activity);
        summarySlot.addView(summaryText, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL));
        summarySlot.addView(views.summaryInput, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL));
        summaryRow.addView(reorder, new LinearLayout.LayoutParams(ui.px(28), ui.px(56)));
        summaryRow.addView(summarySlot, new LinearLayout.LayoutParams(0, -2, 1));
        summaryRow.addView(menu, iconActionLp());
        summaryRow.addView(expand, iconActionLp());
        row.addView(summaryRow);
        expandReorderTouchArea(row, summaryRow, reorder);
        row.addView(editor);

        Runnable fieldChanged = () -> {
            updateFieldSummary(views);
            scheduleSave.run();
        };
        watchTextChange(views.keyInput, fieldChanged);
        watchTextChange(views.labelInput, fieldChanged);
        watchTextChange(views.defaultValueInput, fieldChanged);
        watchTextChange(views.unitInput, fieldChanged);
        watchTextChange(views.incrementInput, fieldChanged);
        watchTextChange(views.decimalsInput, fieldChanged);
        required.setOnCheckedChangeListener((buttonView, isChecked) -> fieldChanged.run());
        prefill.setOnCheckedChangeListener((buttonView, isChecked) -> fieldChanged.run());
        updateFieldEditorControls(views, selectedType(typeInput));
        updateFieldSummary(views);
        typeInput.setOnItemClickListener((parent, view, position, id) -> {
            updateFieldEditorControls(views, selectedType(typeInput));
            fieldChanged.run();
        });

        final LinearLayout[] shellRef = new LinearLayout[1];
        Runnable duplicateAction = () -> {
            addFieldEditor(container, fieldEditors, fieldFromViews(views), scheduleSave);
            scheduleSave.run();
        };
        Runnable removeAction = () -> {
            container.removeView(shellRef[0]);
            updateChildBottomMargins(container, 12, 4);
            views.removed = true;
            scheduleSave.run();
        };
        menu.setOnClickListener(v -> showFieldMenu(v, duplicateAction, removeAction));
        View.OnClickListener toggle = v -> toggleFieldEditor(views, expand);
        expand.setOnClickListener(toggle);
        summaryRow.setOnClickListener(toggle);
        summarySlot.setOnClickListener(toggle);
        summaryText.setOnClickListener(toggle);
        views.summaryTitle.setOnClickListener(toggle);
        views.summaryMeta.setOnClickListener(toggle);
        LinearLayout shell = reorderShell(reorder, row);
        shellRef[0] = shell;

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.bottomMargin = ui.px(12);
        container.addView(shell, rowLp);
        updateChildBottomMargins(container, 12, 4);
        views.row = shell;
        views.container = container;
        fieldEditors.add(views);
        shell.setTag(views);
        attachFieldReorder(reorder, container, fieldEditors, views, scheduleSave);
        setFieldExpanded(views, expand, field == null);
        return views;
    }

    private void showFieldMenu(View anchor, Runnable duplicateAction, Runnable removeAction) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        menu.getMenu().add(0, 1, 0, "Kopieren");
        menu.getMenu().add(0, 2, 1, "Löschen");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                duplicateAction.run();
            } else if (item.getItemId() == 2) {
                removeAction.run();
            }
            return true;
        });
        menu.show();
    }

    private void toggleFieldEditor(FieldEditorViews views, ImageView expand) {
        setFieldExpanded(views, expand, views.editor.getVisibility() != View.VISIBLE);
    }

    private void setFieldExpanded(FieldEditorViews views, ImageView expand, boolean expanded) {
        views.summaryText.setVisibility(expanded ? View.GONE : View.VISIBLE);
        views.summaryInput.setVisibility(expanded ? View.VISIBLE : View.GONE);
        views.editor.setVisibility(expanded ? View.VISIBLE : View.GONE);
        expand.setRotation(expanded ? 180f : 0f);
    }

    private void updateFieldSummary(FieldEditorViews views) {
        String label = views.labelInput.getText().toString().trim();
        if (label.isEmpty()) {
            label = "Neues Feld";
        }
        views.summaryTitle.setText(label);
        String type = selectedType(views.typeInput);
        String typeLabel = "string".equals(type) ? "Text" : "int".equals(type) ? "Ganzzahl" : "float".equals(type) ? "Dezimalzahl" : "Timer";
        String unit = views.unitInput.getText().toString().trim();
        views.summaryMeta.setText(unit.isEmpty() ? typeLabel : typeLabel + " · " + unit);
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
        field.type = selectedType(views.typeInput);
        field.required = views.requiredCheck.isChecked();
        field.prefillFromPrevious = views.prefillCheck.isChecked();
        return field;
    }

    private LinearLayout reorderShell(View reorder, View content) {
        return (LinearLayout) content;
    }

    private View reorderHandle() {
        TextView handle = new TextView(activity);
        handle.setText("⠿");
        handle.setTextSize(ui.sp(24));
        handle.setGravity(Gravity.CENTER);
        handle.setTextColor(theme.mutedTextColor());
        handle.setContentDescription("Verschieben");
        handle.setClickable(true);
        handle.setFocusable(true);
        return handle;
    }

    private void expandReorderTouchArea(View parent, View row, View handle) {
        parent.post(() -> {
            Rect rect = new Rect();
            handle.getHitRect(rect);
            rect.offset(row.getLeft(), row.getTop());
            rect.left = 0;
            rect.top = row.getTop();
            rect.bottom = row.getBottom();
            rect.right = Math.max(rect.right, ui.px(56));
            parent.setTouchDelegate(new TouchDelegate(rect, handle));
        });
    }

    private void attachItemReorder(View handle, LinearLayout container, List<ItemEditorViews> editors, ItemEditorViews views, Runnable onChange) {
        attachReorder(handle, views.card, new ReorderMove() {
            @Override
            public int distance(int direction) {
                return reorderDistance(container, views.card, direction);
            }

            @Override
            public int run(int direction) {
                return moveItemEditor(container, editors, views, direction);
            }
        }, onChange);
    }

    private void attachFieldReorder(View handle, LinearLayout container, List<FieldEditorViews> editors, FieldEditorViews views, Runnable onChange) {
        attachReorder(handle, views.row, new ReorderMove() {
            @Override
            public int distance(int direction) {
                return reorderDistance(container, views.row, direction);
            }

            @Override
            public int run(int direction) {
                return moveFieldEditor(container, editors, views, direction);
            }
        }, onChange);
    }

    private void attachReorder(View handle, View movedView, ReorderMove move, Runnable onChange) {
        final float[] startY = new float[1];
        final float[] consumedY = new float[1];
        final float[] oldElevation = new float[1];
        handle.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                disallowParentIntercept(v, true);
                startY[0] = event.getRawY();
                consumedY[0] = 0;
                oldElevation[0] = movedView.getElevation();
                movedView.setElevation(oldElevation[0] + ui.px(8));
                movedView.setAlpha(0.82f);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float delta = event.getRawY() - startY[0] - consumedY[0];
                int direction = delta > 0 ? 1 : -1;
                while (delta != 0) {
                    int distance = move.distance(direction);
                    if (distance <= 0 || Math.abs(delta) <= distance / 2f) {
                        break;
                    }
                    distance = move.run(direction);
                    if (distance <= 0) {
                        break;
                    }
                    consumedY[0] += direction * distance;
                    delta = event.getRawY() - startY[0] - consumedY[0];
                    direction = delta > 0 ? 1 : -1;
                    onChange.run();
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                movedView.setElevation(oldElevation[0]);
                movedView.setAlpha(1f);
                disallowParentIntercept(v, false);
                return true;
            }
            return false;
        });
    }

    private void disallowParentIntercept(View view, boolean disallow) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
            parent = parent.getParent();
        }
    }

    private TextView iconAction(String text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(ui.sp(24));
        view.setTextColor(theme.mutedTextColor());
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private ImageView expandAction() {
        ImageView view = new ImageView(activity);
        view.setImageResource(R.drawable.ic_expand_more_24);
        view.setColorFilter(theme.mutedTextColor());
        view.setScaleType(ImageView.ScaleType.CENTER);
        view.setMinimumHeight(ui.px(56));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private LinearLayout.LayoutParams iconActionLp() {
        return new LinearLayout.LayoutParams(ui.px(36), ui.px(56));
    }

    private LinearLayout.LayoutParams compactActionButtonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ui.px(48), ui.px(48));
        lp.rightMargin = ui.px(8);
        return lp;
    }

    private LinearLayout.LayoutParams weightedActionButtonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ui.px(48), 1);
        lp.leftMargin = ui.px(8);
        return lp;
    }

    private int moveItemEditor(LinearLayout container, List<ItemEditorViews> itemEditors, ItemEditorViews views, int direction) {
        if (views == null || views.card == null) {
            return 0;
        }

        int fromIndex = container.indexOfChild(views.card);
        int toIndex = fromIndex + direction;
        if (fromIndex < 0 || toIndex < 0 || toIndex >= container.getChildCount()) {
            return 0;
        }

        View sibling = container.getChildAt(toIndex);
        int distance = viewHeightWithMargins(sibling);
        container.removeView(sibling);
        container.addView(sibling, direction > 0 ? fromIndex : toIndex + 1);
        animateReorderSibling(sibling, views.card, direction);
        updateChildBottomMargins(container, 12, 4);

        int listFromIndex = itemEditors.indexOf(views);
        int listToIndex = listFromIndex + direction;
        if (listFromIndex >= 0 && listToIndex >= 0 && listToIndex < itemEditors.size()) {
            itemEditors.remove(listFromIndex);
            itemEditors.add(listToIndex, views);
        }
        return distance;
    }

    private int moveFieldEditor(LinearLayout container, List<FieldEditorViews> fieldEditors, FieldEditorViews views, int direction) {
        if (views == null || views.row == null) {
            return 0;
        }

        int fromIndex = container.indexOfChild(views.row);
        int toIndex = fromIndex + direction;
        if (fromIndex < 0 || toIndex < 0 || toIndex >= container.getChildCount()) {
            return 0;
        }

        View sibling = container.getChildAt(toIndex);
        int distance = viewHeightWithMargins(sibling);
        container.removeView(sibling);
        container.addView(sibling, direction > 0 ? fromIndex : toIndex + 1);
        animateReorderSibling(sibling, views.row, direction);
        updateChildBottomMargins(container, 12, 4);

        int listFromIndex = fieldEditors.indexOf(views);
        int listToIndex = listFromIndex + direction;
        if (listFromIndex >= 0 && listToIndex >= 0 && listToIndex < fieldEditors.size()) {
            fieldEditors.remove(listFromIndex);
            fieldEditors.add(listToIndex, views);
        }
        return distance;
    }

    private int reorderDistance(LinearLayout container, View movedView, int direction) {
        int fromIndex = container.indexOfChild(movedView);
        int toIndex = fromIndex + direction;
        if (fromIndex < 0 || toIndex < 0 || toIndex >= container.getChildCount()) {
            return 0;
        }
        return viewHeightWithMargins(container.getChildAt(toIndex));
    }

    private int viewHeightWithMargins(View view) {
        int height = view.getHeight();
        if (view.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
            height += lp.topMargin + lp.bottomMargin;
        }
        return height;
    }

    private void animateReorderSibling(View sibling, View movedView, int direction) {
        int distance = Math.max(viewHeightWithMargins(movedView), viewHeightWithMargins(sibling));
        sibling.animate().cancel();
        sibling.setTranslationY(direction > 0 ? distance : -distance);
        sibling.animate().translationY(0).setDuration(140).start();
    }

    private void updateChildBottomMargins(LinearLayout container, int normalDp, int lastDp) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                lp.bottomMargin = ui.px(i == container.getChildCount() - 1 ? lastDp : normalDp);
                child.setLayoutParams(lp);
            }
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

    private void styleCheckBox(MaterialCheckBox checkBox) {
        checkBox.setUseMaterialThemeColors(false);
        checkBox.setTextColor(theme.primaryTextColor());
        checkBox.setButtonTintList(checkBoxStateList());
        checkBox.setMinHeight(ui.px(40));
        checkBox.setMinimumHeight(ui.px(40));
        checkBox.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, ui.px(40));
        checkBox.setLayoutParams(lp);
    }

    private ColorStateList checkBoxStateList() {
        int accent = theme.accentColor();
        int normal = theme.darkMode() ? theme.secondaryTextColor() : theme.borderColor();
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{android.R.attr.state_hovered},
                        new int[]{android.R.attr.state_focused},
                        new int[]{android.R.attr.state_enabled},
                        new int[]{}
                },
                new int[]{accent, accent, accent, normal, normal});
    }

    private ColorStateList inputHintStateList() {
        int accent = theme.accentColor();
        int normal = theme.mutedTextColor();
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_focused},
                        new int[]{android.R.attr.state_hovered},
                        new int[]{android.R.attr.state_enabled},
                        new int[]{}
                },
                new int[]{accent, accent, normal, normal});
    }

    private ColorStateList inputBorderStateList() {
        int accent = theme.accentColor();
        int normal = theme.darkMode() ? theme.secondaryTextColor() : theme.borderColor();
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_focused},
                        new int[]{android.R.attr.state_hovered},
                        new int[]{android.R.attr.state_enabled},
                        new int[]{}
                },
                new int[]{accent, accent, normal, normal});
    }

    private ColorStateList accentStateList() {
        int accent = theme.accentColor();
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_hovered},
                        new int[]{android.R.attr.state_focused},
                        new int[]{android.R.attr.state_enabled},
                        new int[]{}
                },
                new int[]{accent, accent, accent, accent});
    }

    private void tintCursor(EditText input) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            GradientDrawable cursor = new GradientDrawable();
            cursor.setColor(theme.accentColor());
            cursor.setSize(ui.px(2), ui.px(24));
            input.setTextCursorDrawable(cursor);
        }
    }

    private TextInputLayout outlinedInput(String label, EditText input) {
        TextInputLayout layout = new TextInputLayout(activity);
        layout.setHint(label);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxBackgroundColor(theme.surfaceColor());
        layout.setBoxStrokeColor(theme.accentColor());
        layout.setBoxStrokeColorStateList(inputBorderStateList());
        layout.setBoxStrokeWidth(ui.px(1));
        layout.setBoxStrokeWidthFocused(ui.px(2));
        layout.setHintTextColor(inputHintStateList());
        layout.setBoxCornerRadii(ui.px(6), ui.px(6), ui.px(6), ui.px(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = ui.px(12);
        layout.setLayoutParams(lp);
        input.setHint(null);
        input.setHintTextColor(inputHintStateList());
        input.setBackground(null);
        tintCursor(input);
        boolean multiline = input.getMinLines() > 1;
        input.setPadding(ui.px(12), multiline ? ui.px(12) : 0, ui.px(12), multiline ? ui.px(12) : 0);
        layout.addView(input, new LinearLayout.LayoutParams(-1, multiline ? -2 : ui.px(56)));
        return layout;
    }

    private EditText labeledInput(String label, String value, int inputType) {
        EditText input = new TextInputEditText(activity);
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
                field.put("type", selectedType(fieldViews.typeInput));
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

    private int typeIndex(String type) {
        if ("int".equals(type)) {
            return 1;
        }
        if ("float".equals(type)) {
            return 2;
        }
        if ("duration".equals(type)) {
            return 3;
        }
        return 0;
    }

    private String selectedType(MaterialAutoCompleteTextView input) {
        String value = String.valueOf(input.getText());
        if ("Ganzzahl".equals(value)) {
            return "int";
        }
        if ("Dezimalzahl".equals(value)) {
            return "float";
        }
        if ("Timer".equals(value)) {
            return "duration";
        }
        return "string";
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

    interface BackActionSetter {
        void accept(Runnable backAction);
    }

    private interface ReorderMove {
        int distance(int direction);

        int run(int direction);
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
        TextView summaryTitle;
        TextView summaryMeta;
        View summaryText;
        View summaryInput;
        LinearLayout editor;
        LinearLayout fieldsContainer;
        TextView fieldsCount;
        final List<FieldEditorViews> fields = new ArrayList<>();
        boolean removed;
    }

    private static final class FieldEditorViews {
        LinearLayout row;
        LinearLayout container;
        LinearLayout numericRow;
        View incrementWrap;
        View decimalsWrap;
        EditText keyInput;
        EditText labelInput;
        EditText defaultValueInput;
        EditText unitInput;
        EditText incrementInput;
        EditText decimalsInput;
        MaterialAutoCompleteTextView typeInput;
        TextView summaryTitle;
        TextView summaryMeta;
        View summaryText;
        View summaryInput;
        LinearLayout editor;
        MaterialCheckBox requiredCheck;
        MaterialCheckBox prefillCheck;
        boolean removed;
    }
}

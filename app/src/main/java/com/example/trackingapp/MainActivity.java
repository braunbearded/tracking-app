package com.example.trackingapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.os.Build;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private TrackingDatabase db;
    private LinearLayout root;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Long> timers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new TrackingDatabase(this);
        showHome(0);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        timers.clear();
        super.onDestroy();
    }

    private TextView tv(String text, int sp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setPadding(16, 12, 16, 8);
        return view;
    }

    private Button btn(String text) {
        return secondaryButton(text);
    }

    private Button button(String text, int fillColor, int textColor, int strokeColor) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(14);
        button.setPadding(px(14), px(12), px(14), px(12));
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(px(14));
        drawable.setStroke(px(1), strokeColor);
        button.setBackground(drawable);
        return button;
    }

    private Button primaryButton(String text) {
        return button(text, 0xff1d4ed8, Color.WHITE, 0xff1d4ed8);
    }

    private Button secondaryButton(String text) {
        return button(text, 0xffffffff, 0xff1d2939, 0xffd0d5dd);
    }

    private Button ghostButton(String text) {
        return button(text, 0xfff9fafb, 0xff344054, 0xffe4e7ec);
    }

    private Button dangerButton(String text) {
        return button(text, 0xfffff5f5, 0xffb42318, 0xfffecdca);
    }

    private Button tabButton(String text, boolean selected) {
        return button(
                text,
                selected ? 0xffdbeafe : 0xffffffff,
                selected ? 0xff1d4ed8 : 0xff667085,
                selected ? 0xff93c5fd : 0xffd0d5dd);
    }

    private void base() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        setContentView(root);
    }

    private void showHome(int tab) {
        base();

        LinearLayout tabs = new LinearLayout(this);
        tabs.setWeightSum(2);

        Button sessionsTab = tabButton("Sessions", tab == 0);
        Button trackerTab = tabButton("Tracker", tab == 1);
        tabs.addView(sessionsTab, new LinearLayout.LayoutParams(0, -2, 1));
        tabs.addView(trackerTab, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(tabs);

        FrameLayout body = new FrameLayout(this);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));

        sessionsTab.setOnClickListener(v -> showHome(0));
        trackerTab.setOnClickListener(v -> showHome(1));

        if (tab == 0) {
            sessions(body);
        } else {
            trackers(body);
        }
    }

    private void sessions(FrameLayout body) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(16), px(16), px(16), px(20));
        scrollView.addView(box);

        TextView eyebrow = tv("SESSIONS", 12);
        eyebrow.setTextColor(0xff667085);
        eyebrow.setPadding(0, 0, 0, px(4));
        box.addView(eyebrow);

        TextView title = tv("Deine Läufe", 28);
        title.setPadding(0, 0, 0, px(4));
        box.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Alle Sessions über alle Tracker hinweg. Tippe eine Session an, um sie zu öffnen.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xff475467);
        subtitle.setPadding(0, 0, 0, px(16));
        box.addView(subtitle);

        Button add = primaryButton("Neue Session starten");
        box.addView(add, new LinearLayout.LayoutParams(-1, -2));
        ((LinearLayout.LayoutParams) add.getLayoutParams()).bottomMargin = px(18);
        add.setOnClickListener(v -> chooseTracker());

        for (Session session : db.sessions()) {
            Tracker tracker = db.readTracker(session.trackerId);
            if (tracker == null) {
                continue;
            }

            int recordCount = db.recordCount(session.id);
            boolean open = "open".equals(session.status);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(px(16), px(16), px(16), px(16));
            card.setBackground(makeRoundedCard(open ? 0xffffffff : 0xfffcfcfd, 0xffd0d5dd));
            card.setElevation(px(2));

            View statusStripe = new View(this);
            statusStripe.setBackgroundColor(open ? 0xff1d4ed8 : 0xff667085);
            LinearLayout.LayoutParams stripeLp = new LinearLayout.LayoutParams(px(36), px(4));
            stripeLp.bottomMargin = px(12);
            card.addView(statusStripe, stripeLp);

            TextView trackerName = tv(tracker.name, 20);
            trackerName.setPadding(0, 0, 0, px(2));
            card.addView(trackerName);

            LinearLayout metaRow = new LinearLayout(this);
            metaRow.setOrientation(LinearLayout.HORIZONTAL);
            metaRow.setWeightSum(2);
            metaRow.setPadding(0, 0, 0, px(12));

            TextView leftMeta = new TextView(this);
            leftMeta.setText(date(session.createdAt));
            leftMeta.setTextSize(13);
            leftMeta.setTextColor(0xff667085);
            metaRow.addView(leftMeta, new LinearLayout.LayoutParams(0, -2, 1));

            TextView rightMeta = new TextView(this);
            rightMeta.setText(recordCount + "/" + tracker.items.size() + " Items");
            rightMeta.setTextSize(13);
            rightMeta.setTextColor(0xff667085);
            rightMeta.setGravity(Gravity.END);
            metaRow.addView(rightMeta, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(metaRow);

            LinearLayout chipRow = new LinearLayout(this);
            chipRow.setOrientation(LinearLayout.HORIZONTAL);
            chipRow.setPadding(0, 0, 0, px(12));

            chipRow.addView(chip(session.status.equals("open") ? "Offen" : "Abgeschlossen", open ? 0xffdbeafe : 0xffeaecf0, open ? 0xff1d4ed8 : 0xff344054));
            chipRow.addView(chip(recordCount + "/" + tracker.items.size(), 0xffecfdf3, 0xff027a48));
            card.addView(chipRow);

            TextView preview = new TextView(this);
            preview.setText(preview(session.id));
            preview.setTextSize(14);
            preview.setTextColor(0xff344054);
            preview.setLineSpacing(0f, 1.15f);
            preview.setMaxLines(2);
            preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(preview);

            card.setOnClickListener(v -> openSession(session.id));
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
            cardLp.bottomMargin = px(14);
            box.addView(card, cardLp);
        }

        if (db.sessions().isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setPadding(px(16), px(16), px(16), px(16));
            empty.setBackground(makeRoundedCard(0xfff9fafb, 0xffe4e7ec));

            TextView emptyTitle = tv("Noch keine Sessions vorhanden", 18);
            emptyTitle.setPadding(0, 0, 0, px(4));
            empty.addView(emptyTitle);

            TextView emptyBody = new TextView(this);
            emptyBody.setText("Starte eine neue Session, um erste Werte zu erfassen und die Historie aufzubauen.");
            emptyBody.setTextSize(14);
            emptyBody.setTextColor(0xff475467);
            empty.addView(emptyBody);

            LinearLayout.LayoutParams emptyLp = new LinearLayout.LayoutParams(-1, -2);
            emptyLp.topMargin = px(4);
            box.addView(empty, emptyLp);
        }

        body.addView(scrollView);
    }

    private String preview(long sessionId) {
        StringBuilder builder = new StringBuilder();
        for (ItemRecord record : db.records(sessionId).values()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(record.valuesJson);
            if (builder.length() > 90) {
                break;
            }
        }
        return builder.toString();
    }

    private String date(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(millis));
    }

    private int px(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable makeRoundedCard(int fillColor, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(px(18));
        drawable.setStroke(px(1), strokeColor);
        return drawable;
    }

    private TextView chip(String text, int backgroundColor, int textColor) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(12);
        chip.setTextColor(textColor);
        chip.setPadding(px(10), px(6), px(10), px(6));

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(backgroundColor);
        drawable.setCornerRadius(px(999));
        chip.setBackground(drawable);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = px(8);
        chip.setLayoutParams(lp);
        return chip;
    }

    private View summaryCard(boolean readOnly, Map<String, Object> values, Item item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);
        card.setBackgroundColor(0xfff2f4f8);

        TextView title = tv(readOnly ? "Abgeschlossener Stand" : "Aktueller Session-Stand", 16);
        title.setPadding(0, 0, 0, 6);
        card.addView(title);

        TextView body = new TextView(this);
        body.setText(summaryText(values, item));
        body.setTextSize(14);
        body.setTextColor(0xff3a4554);
        card.addView(body);

        TextView hint = new TextView(this);
        hint.setText(readOnly
                ? "Read-only. Werte sind unveränderlich."
                : "Änderungen werden beim Zurück- oder Weitergehen gespeichert.");
        hint.setTextSize(12);
        hint.setTextColor(0xff667085);
        hint.setPadding(0, 8, 0, 0);
        card.addView(hint);

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

    private void chooseTracker() {
        List<Tracker> trackers = db.trackers();
        if (trackers.isEmpty()) {
            Toast.makeText(this, "Keine Tracker vorhanden", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[trackers.size()];
        for (int i = 0; i < trackers.size(); i++) {
            names[i] = trackers.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle("Tracker auswählen")
                .setItems(names, (dialog, which) -> openSession(db.createSession(trackers.get(which).id)))
                .show();
    }

    private void trackers(FrameLayout body) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(16), px(16), px(16), px(20));
        scrollView.addView(box);

        TextView eyebrow = tv("TRACKER", 12);
        eyebrow.setTextColor(0xff667085);
        eyebrow.setPadding(0, 0, 0, px(4));
        box.addView(eyebrow);

        TextView title = tv("Deine Tracker", 28);
        title.setPadding(0, 0, 0, px(4));
        box.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Verwalte Vorlagen, Items und Felder für deine Sessions.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xff475467);
        subtitle.setPadding(0, 0, 0, px(16));
        box.addView(subtitle);

        Button add = primaryButton("Neuer Tracker");
        box.addView(add, new LinearLayout.LayoutParams(-1, -2));
        ((LinearLayout.LayoutParams) add.getLayoutParams()).bottomMargin = px(18);
        add.setOnClickListener(v -> createTracker());

        List<Tracker> trackers = db.trackers();
        if (trackers.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setPadding(px(16), px(16), px(16), px(16));
            empty.setBackground(makeRoundedCard(0xfff9fafb, 0xffe4e7ec));

            TextView emptyTitle = tv("Noch keine Tracker vorhanden", 18);
            emptyTitle.setPadding(0, 0, 0, px(4));
            empty.addView(emptyTitle);

            TextView emptyBody = new TextView(this);
            emptyBody.setText("Lege einen Tracker an, um Items und Fields für deine Sessions zu definieren.");
            emptyBody.setTextSize(14);
            emptyBody.setTextColor(0xff475467);
            empty.addView(emptyBody);

            box.addView(empty);
        } else {
            for (Tracker tracker : trackers) {
                int itemCount = tracker.items.size();
                int fieldCount = 0;
                for (Item item : tracker.items) {
                    fieldCount += item.fields.size();
                }

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(px(16), px(16), px(16), px(16));
                card.setBackground(makeRoundedCard(0xffffffff, 0xffd0d5dd));
                card.setElevation(px(2));

                View stripe = new View(this);
                stripe.setBackgroundColor(0xff7c3aed);
                LinearLayout.LayoutParams stripeLp = new LinearLayout.LayoutParams(px(36), px(4));
                stripeLp.bottomMargin = px(12);
                card.addView(stripe, stripeLp);

                TextView trackerName = tv(tracker.name == null || tracker.name.trim().isEmpty() ? "Unbenannter Tracker" : tracker.name, 20);
                trackerName.setPadding(0, 0, 0, px(2));
                card.addView(trackerName);

                LinearLayout metaRow = new LinearLayout(this);
                metaRow.setOrientation(LinearLayout.HORIZONTAL);
                metaRow.setPadding(0, 0, 0, px(12));

                TextView meta = new TextView(this);
                meta.setText(itemCount + " Items · " + fieldCount + " Fields");
                meta.setTextSize(13);
                meta.setTextColor(0xff667085);
                metaRow.addView(meta);
                card.addView(metaRow);

                LinearLayout chipRow = new LinearLayout(this);
                chipRow.setOrientation(LinearLayout.HORIZONTAL);
                chipRow.setPadding(0, 0, 0, px(12));
                chipRow.addView(chip(itemCount == 0 ? "Leer" : itemCount + " Items", itemCount == 0 ? 0xfffef3c7 : 0xffecfdf3, itemCount == 0 ? 0xffb54708 : 0xff027a48));
                chipRow.addView(chip(fieldCount + " Fields", 0xffeff6ff, 0xff1d4ed8));
                card.addView(chipRow);

                TextView preview = new TextView(this);
                preview.setText(tracker.description == null || tracker.description.trim().isEmpty()
                        ? "Keine Beschreibung vorhanden."
                        : tracker.description);
                preview.setTextSize(14);
                preview.setTextColor(0xff344054);
                preview.setLineSpacing(0f, 1.15f);
                preview.setMaxLines(2);
                preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
                card.addView(preview);

                card.setOnClickListener(v -> editTracker(tracker.id));
                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
                cardLp.bottomMargin = px(14);
                box.addView(card, cardLp);
            }
        }

        body.addView(scrollView);
    }

    private void editTracker(long id) {
        Tracker tracker = db.readTracker(id);
        if (tracker == null) {
            Toast.makeText(this, "Tracker nicht gefunden", Toast.LENGTH_SHORT).show();
            showHome(1);
            return;
        }
        openTrackerEditor(id, tracker, false);
    }

    private void createTracker() {
        openTrackerEditor(-1, templateTracker(), true);
    }

    private Tracker templateTracker() {
        Tracker tracker = new Tracker();
        tracker.name = "";
        tracker.description = "";
        return tracker;
    }

    private void openTrackerEditor(long id, Tracker tracker, boolean isNew) {
        if (!isNew && db.readTracker(id) == null) {
            Toast.makeText(this, "Tracker nicht gefunden", Toast.LENGTH_SHORT).show();
            showHome(1);
            return;
        }

        base();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(px(16), px(16), px(16), px(12));
        header.setBackgroundColor(0xfff8fafc);

        TextView eyebrow = tv(isNew ? "NEUER TRACKER" : "TRACKER BEARBEITEN", 12);
        eyebrow.setTextColor(0xff667085);
        eyebrow.setPadding(0, 0, 0, px(4));
        header.addView(eyebrow);

        TextView title = tv(isNew ? "Tracker anlegen" : "Tracker bearbeiten", 24);
        title.setPadding(0, 0, 0, px(4));
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Name, Beschreibung, Items und Fields direkt im Formular pflegen.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xff475467);
        header.addView(subtitle);

        root.addView(header);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(px(16), px(16), px(16), px(16));
        scrollView.addView(body);

        TrackerEditorForm form = buildTrackerEditorForm(tracker);
        body.addView(form.nameInput);
        body.addView(form.descriptionInput);

        Button addItem = primaryButton("Item hinzufügen");
        LinearLayout.LayoutParams addItemLp = new LinearLayout.LayoutParams(-1, -2);
        addItemLp.bottomMargin = px(16);
        body.addView(addItem, addItemLp);

        LinearLayout itemsContainer = new LinearLayout(this);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        body.addView(itemsContainer, new LinearLayout.LayoutParams(-1, -2));

        if (tracker.items.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setPadding(px(16), px(16), px(16), px(16));
            empty.setBackground(makeRoundedCard(0xfff9fafb, 0xffe4e7ec));

            TextView emptyTitle = tv("Noch keine Items angelegt", 18);
            emptyTitle.setPadding(0, 0, 0, px(4));
            empty.addView(emptyTitle);

            TextView emptyBody = new TextView(this);
            emptyBody.setText("Tippe auf \"Item hinzufügen\" und lege dann darunter die Fields an.");
            emptyBody.setTextSize(14);
            emptyBody.setTextColor(0xff475467);
            empty.addView(emptyBody);

            itemsContainer.addView(empty);
        } else {
            for (Item item : tracker.items) {
                addItemEditor(itemsContainer, form.items, item);
            }
        }

        addItem.setOnClickListener(v -> addItemEditor(itemsContainer, form.items, null));

        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(this);
        footer.setWeightSum(2);

        Button back = secondaryButton("Zurück");
        Button save = primaryButton("Tracker speichern");
        footer.addView(back, new LinearLayout.LayoutParams(0, -2, 1));
        footer.addView(save, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(footer);

        back.setOnClickListener(v -> showHome(1));
        save.setOnClickListener(v -> {
            try {
                String json = trackerEditorToJson(form);
                if (isNew) {
                    TrackerJsonRepository.saveTracker(db, -1, json, true);
                } else {
                    TrackerJsonRepository.updateTracker(db, id, json);
                }
                Toast.makeText(this, "Gespeichert", Toast.LENGTH_SHORT).show();
                showHome(1);
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private TrackerEditorForm buildTrackerEditorForm(Tracker tracker) {
        TrackerEditorForm form = new TrackerEditorForm();
        form.nameInput = labeledInput("Tracker-Name", tracker.name == null ? "" : tracker.name, InputType.TYPE_CLASS_TEXT);
        form.descriptionInput = labeledInput("Beschreibung", tracker.description == null ? "" : tracker.description, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        form.descriptionInput.setMinLines(2);
        form.descriptionInput.setGravity(Gravity.TOP);
        return form;
    }

    private ItemEditorViews addItemEditor(LinearLayout container, List<ItemEditorViews> itemEditors, Item item) {
        ItemEditorViews views = new ItemEditorViews();

        if (itemEditors.isEmpty() && container.getChildCount() == 1) {
            container.removeAllViews();
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(px(16), px(16), px(16), px(16));
        card.setBackground(makeRoundedCard(0xfffafafa, 0xffe4e7ec));

        LinearLayout cardHeader = new LinearLayout(this);
        cardHeader.setOrientation(LinearLayout.HORIZONTAL);
        cardHeader.setWeightSum(2);

        TextView label = tv("Item", 18);
        cardHeader.addView(label, new LinearLayout.LayoutParams(0, -2, 1));

        View drag = dragHandle();
        cardHeader.addView(drag);

        Button remove = dangerButton("Entfernen");
        cardHeader.addView(remove);
        card.addView(cardHeader);

        views.titleInput = labeledInput("Titel", item == null ? "" : item.title, InputType.TYPE_CLASS_TEXT);
        card.addView(views.titleInput);

        LinearLayout fieldsHeader = new LinearLayout(this);
        fieldsHeader.setOrientation(LinearLayout.HORIZONTAL);
        fieldsHeader.setPadding(0, px(8), 0, px(8));
        TextView fieldsTitle = tv("Fields", 16);
        fieldsTitle.setPadding(0, 0, 0, 0);
        fieldsHeader.addView(fieldsTitle, new LinearLayout.LayoutParams(0, -2, 1));

        Button addField = primaryButton("Field hinzufügen");
        fieldsHeader.addView(addField);
        card.addView(fieldsHeader);

        LinearLayout fieldsContainer = new LinearLayout(this);
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
        lp.bottomMargin = px(16);
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

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(px(12), px(12), px(12), px(12));
        row.setBackground(makeRoundedCard(0xffffffff, 0xffe4e7ec));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.bottomMargin = px(12);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setWeightSum(2);

        TextView heading = tv(field == null ? "Neues Field" : field.label, 16);
        heading.setPadding(0, 0, 0, 0);
        topRow.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));

        View drag = dragHandle();
        topRow.addView(drag);

        Button remove = dangerButton("Entfernen");
        topRow.addView(remove);
        row.addView(topRow);

        views.keyInput = labeledInput("Key", field == null ? "" : field.key, InputType.TYPE_CLASS_TEXT);
        views.labelInput = labeledInput("Label", field == null ? "" : field.label, InputType.TYPE_CLASS_TEXT);
        views.defaultValueInput = labeledInput("Default", field == null ? "" : String.valueOf(field.defaultValue == null ? "" : field.defaultValue), InputType.TYPE_CLASS_TEXT);
        views.unitInput = labeledInput("Unit", field == null ? "" : field.unit, InputType.TYPE_CLASS_TEXT);
        views.incrementInput = labeledInput("Increment", field == null ? "1" : String.valueOf(field.increment), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        views.decimalsInput = labeledInput("Decimals", field == null ? "1" : String.valueOf(field.decimals), InputType.TYPE_CLASS_NUMBER);

        row.addView(views.keyInput);
        row.addView(views.labelInput);
        row.addView(views.defaultValueInput);

        RadioGroup typeGroup = new RadioGroup(this);
        typeGroup.setOrientation(RadioGroup.HORIZONTAL);
        typeGroup.setPadding(0, px(4), 0, px(4));
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

        LinearLayout optionsRow = new LinearLayout(this);
        optionsRow.setOrientation(LinearLayout.VERTICAL);
        optionsRow.addView(wrapLabeledView("Type", typeGroup));

        CheckBox required = new CheckBox(this);
        required.setText("Required");
        required.setChecked(field != null && field.required);
        views.requiredCheck = required;
        optionsRow.addView(required);
        row.addView(optionsRow);

        CheckBox prefill = new CheckBox(this);
        prefill.setText("Prefill from previous");
        prefill.setChecked(field != null && field.prefillFromPrevious);
        views.prefillCheck = prefill;
        row.addView(prefill);

        LinearLayout numericRow = new LinearLayout(this);
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
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextSize(12);
        title.setTextColor(0xff667085);
        title.setPadding(0, 0, 0, px(4));
        group.addView(title);
        group.addView(view);
        return group;
    }

    private EditText labeledInput(String label, String value, int inputType) {
        EditText input = new EditText(this);
        input.setText(value == null ? "" : value);
        input.setHint(label);
        input.setInputType(inputType);
        input.setPadding(px(12), px(12), px(12), px(12));
        input.setBackground(makeRoundedCard(0xffffffff, 0xffd0d5dd));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = px(12);
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

    private void openSession(long sessionId) {
        Session session = db.session(sessionId);
        if (session == null) {
            Toast.makeText(this, "Session nicht gefunden", Toast.LENGTH_SHORT).show();
            showHome(0);
            return;
        }

        Tracker tracker = db.readTracker(session.trackerId);
        if (tracker == null || tracker.items.isEmpty()) {
            Toast.makeText(this, "Tracker enthält keine Items", Toast.LENGTH_SHORT).show();
            showHome(0);
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

    private void showItem(Session session, Tracker tracker, int index) {
        base();
        boolean readOnly = "completed".equals(session.status);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(box);

        root.addView(tv(tracker.name + " · " + session.status, 20));

        Map<Long, Map<String, View>> inputsByItem = new LinkedHashMap<>();
        for (int itemIndex = 0; itemIndex < tracker.items.size(); itemIndex++) {
            Item item = tracker.items.get(itemIndex);
            Map<String, Object> values = initialValues(session, item);
            Map<String, View> inputs = new HashMap<>();
            inputsByItem.put(item.id, inputs);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(20, 20, 20, 20);
            card.setBackgroundColor(0xfff2f4f8);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
            cardLp.bottomMargin = 20;

            card.addView(tv((itemIndex + 1) + ". " + item.title, 18));
            card.addView(summaryCard(readOnly, values, item));

            for (FieldDefinition field : item.fields) {
                fieldControl(card, field, values, inputs, readOnly);
            }

            box.addView(card, cardLp);
        }

        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(this);
        footer.setWeightSum(2);

        Button back = secondaryButton("Zurück");
        Button close = primaryButton(readOnly ? "Übersicht" : "Session speichern / schließen");
        footer.addView(back, new LinearLayout.LayoutParams(0, -2, 1));
        footer.addView(close, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(footer);

        back.setOnClickListener(v -> {
            if (!readOnly) {
                saveSessionDraft(session, tracker, inputsByItem);
            }
            showHome(0);
        });

        close.setOnClickListener(v -> {
            if (!readOnly) {
                saveSessionDraft(session, tracker, inputsByItem);
                db.complete(session.id);
            }
            showHome(0);
        });
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
        box.addView(tv(field.label + (field.unit == null || field.unit.isEmpty() ? "" : " (" + field.unit + ")"), 16));

        Object value = values.get(field.key);
        if ("string".equals(field.type)) {
            EditText editText = new EditText(this);
            editText.setText(value == null ? "" : String.valueOf(value));
            editText.setEnabled(!readOnly);
            box.addView(editText);
            inputs.put(field.key, editText);
            return;
        }

        if ("duration".equals(field.type)) {
            TextView display = tv(formatMs(toLong(value)), 24);
            display.setTag(toLong(value));

            LinearLayout row = new LinearLayout(this);
            Button start = secondaryButton("Start");
            Button stop = secondaryButton("Stop");
            Button reset = ghostButton("Reset");
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

        LinearLayout row = new LinearLayout(this);
        Button minus = secondaryButton("−");
        Button plus = primaryButton("+");
        EditText editText = new EditText(this);
        editText.setText(value == null ? "" : String.valueOf(value));
        editText.setInputType("int".equals(field.type)
                ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED
                : InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
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
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
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

    private String formatMs(long millis) {
        long seconds = millis / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }

    private static final class TrackerEditorForm {
        EditText nameInput;
        EditText descriptionInput;
        final List<ItemEditorViews> items = new java.util.ArrayList<>();
    }

    private static final class ItemEditorViews {
        LinearLayout card;
        LinearLayout container;
        EditText titleInput;
        LinearLayout fieldsContainer;
        final List<FieldEditorViews> fields = new java.util.ArrayList<>();
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

    private RadioButton radioType(String label, String value, boolean checked) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setText(label);
        button.setTag(value);
        button.setChecked(checked);
        button.setTextSize(14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = px(12);
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
        LinearLayout handle = new LinearLayout(this);
        handle.setOrientation(LinearLayout.VERTICAL);
        handle.setGravity(Gravity.CENTER);
        handle.setPadding(px(10), px(8), px(10), px(8));
        handle.setBackground(makeRoundedCard(0xfff2f4f7, 0xffd0d5dd));
        handle.setContentDescription("Verschieben");
        handle.setClickable(true);
        handle.setFocusable(true);

        for (int i = 0; i < 3; i++) {
            View bar = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(14), px(2));
            if (i < 2) {
                lp.bottomMargin = px(3);
            }
            bar.setLayoutParams(lp);
            bar.setBackgroundColor(0xff667085);
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
        target.setOnDragListener((v, event) -> handleItemDrop(container, target, itemEditors, event));
        container.setOnDragListener((v, event) -> handleItemContainerDrop(container, itemEditors, event));
    }

    private void configureFieldDragTarget(LinearLayout container, View target, List<FieldEditorViews> fieldEditors) {
        target.setOnDragListener((v, event) -> handleFieldDrop(container, target, fieldEditors, event));
        container.setOnDragListener((v, event) -> handleFieldContainerDrop(container, fieldEditors, event));
    }

    private boolean handleItemDrop(LinearLayout container, View target, List<ItemEditorViews> itemEditors, android.view.DragEvent event) {
        return handleDrop(container, target, itemEditors, event);
    }

    private boolean handleFieldDrop(LinearLayout container, View target, List<FieldEditorViews> fieldEditors, android.view.DragEvent event) {
        return handleDrop(container, target, fieldEditors, event);
    }

    private boolean handleItemContainerDrop(LinearLayout container, List<ItemEditorViews> itemEditors, android.view.DragEvent event) {
        return handleContainerDrop(container, itemEditors, event);
    }

    private boolean handleFieldContainerDrop(LinearLayout container, List<FieldEditorViews> fieldEditors, android.view.DragEvent event) {
        return handleContainerDrop(container, fieldEditors, event);
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
}

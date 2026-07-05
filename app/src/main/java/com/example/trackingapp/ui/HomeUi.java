package com.example.trackingapp;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.trackingapp.theme.ThemeStore;
import com.example.trackingapp.ui.AppUi;

public final class HomeUi {
    private final Activity activity;
    private final TrackingDatabase db;
    private final ThemeStore theme;
    private final AppUi ui;
    private final SessionClick openSession;
    private final TrackerClick editTracker;

    public HomeUi(Activity activity, TrackingDatabase db, ThemeStore theme, AppUi ui,
                  SessionClick openSession, TrackerClick editTracker) {
        this.activity = activity;
        this.db = db;
        this.theme = theme;
        this.ui = ui;
        this.openSession = openSession;
        this.editTracker = editTracker;
    }

    public void renderSessions(FrameLayout body) {
        ScrollView scrollView = createScrollView();
        LinearLayout box = createListBox(scrollView);
        addScreenHeader(box, null, "Deine Läufe",
                "Alle Sessions über alle Tracker hinweg. Tippe eine Session an, um sie zu öffnen.");

        java.util.List<Session> sessions = db.sessions();
        for (Session session : sessions) {
            Tracker tracker = db.readTracker(session.trackerId);
            if (tracker == null) {
                continue;
            }

            int recordCount = db.recordCount(session.id);
            boolean open = "open".equals(session.status);
            LinearLayout card = createCard(theme.surfaceColor());
            addEyebrow(card, "SESSION");

            TextView trackerName = ui.tv(tracker.name, 18);
            trackerName.setPadding(0, 0, 0, ui.px(2));
            card.addView(trackerName);

            TextView trackerDescription = new TextView(activity);
            trackerDescription.setText(tracker.description == null || tracker.description.trim().isEmpty()
                    ? "Ohne Beschreibung"
                    : tracker.description);
            trackerDescription.setTextSize(ui.sp(14));
            trackerDescription.setTextColor(theme.secondaryTextColor());
            trackerDescription.setPadding(0, 0, 0, ui.px(12));
            trackerDescription.setMaxLines(2);
            trackerDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(trackerDescription);

            addMetaRow(card, date(session.createdAt), recordCount + "/" + tracker.items.size() + " Items");
            addChipRow(card,
                    ui.chip(session.status.equals("open") ? "Offen" : "Abgeschlossen",
                            open ? theme.accentSoftColor() : theme.surfaceAltColor(),
                            open ? theme.accentColor() : theme.primaryTextColor()),
                    ui.chip(recordCount + "/" + tracker.items.size(),
                            theme.accentSoftColor(), theme.accentColor()));

            TextView preview = new TextView(activity);
            preview.setText(preview(session.id));
            preview.setTextSize(ui.sp(14));
            preview.setTextColor(theme.primaryTextColor());
            preview.setLineSpacing(0f, 1.15f);
            preview.setMaxLines(2);
            preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(preview);

            card.setOnClickListener(v -> openSession.open(session.id));
            box.addView(card, cardLayoutParams());
        }

        if (sessions.isEmpty()) {
            box.addView(emptyState(
                    "Noch keine Sessions vorhanden",
                    "Starte eine neue Session, um erste Werte zu erfassen und die Historie aufzubauen."));
        }

        body.addView(scrollView);
    }

    public void renderTrackers(FrameLayout body) {
        ScrollView scrollView = createScrollView();
        LinearLayout box = createListBox(scrollView);
        addScreenHeader(box, "TRACKER", "Deine Tracker",
                "Hier verwaltest du die Vorlagen für neue Sessions.");

        java.util.List<Tracker> trackers = db.trackers();
        for (Tracker tracker : trackers) {
            int itemCount = tracker.items.size();
            int fieldCount = 0;
            for (Item item : tracker.items) {
                fieldCount += item.fields.size();
            }

            LinearLayout card = createCard(theme.surfaceColor());
            addEyebrow(card, "TRACKER");

            TextView trackerName = ui.tv(tracker.name == null || tracker.name.trim().isEmpty() ? "Unbenannter Tracker" : tracker.name, 18);
            trackerName.setPadding(0, 0, 0, ui.px(2));
            card.addView(trackerName);

            TextView trackerDescription = new TextView(activity);
            trackerDescription.setText(tracker.description == null || tracker.description.trim().isEmpty()
                    ? "Keine Beschreibung vorhanden."
                    : tracker.description);
            trackerDescription.setTextSize(ui.sp(14));
            trackerDescription.setTextColor(theme.secondaryTextColor());
            trackerDescription.setPadding(0, 0, 0, ui.px(12));
            trackerDescription.setMaxLines(2);
            trackerDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(trackerDescription);

            addMetaRow(card, itemCount + " Items", fieldCount + " Fields");
            addChipRow(card,
                    ui.chip(itemCount == 0 ? "Leer" : itemCount + " Items",
                            itemCount == 0 ? theme.withAlpha(theme.accentColor(), 0x18) : theme.accentSoftColor(),
                            itemCount == 0 ? theme.primaryTextColor() : theme.accentColor()),
                    ui.chip(fieldCount + " Fields", theme.accentSoftColor(), theme.accentColor()));

            TextView preview = new TextView(activity);
            preview.setText(firstItemPreview(tracker));
            preview.setTextSize(ui.sp(14));
            preview.setTextColor(theme.primaryTextColor());
            preview.setLineSpacing(0f, 1.15f);
            preview.setMaxLines(2);
            preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(preview);

            card.setOnClickListener(v -> editTracker.open(tracker.id));
            box.addView(card, cardLayoutParams());
        }

        body.addView(scrollView);
    }

    private ScrollView createScrollView() {
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        return scrollView;
    }

    private LinearLayout createListBox(ScrollView scrollView) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(ui.px(16), ui.px(12), ui.px(16), ui.px(104));
        scrollView.addView(box);
        return box;
    }

    private void addScreenHeader(LinearLayout box, String eyebrowText, String titleText, String subtitleText) {
        if (eyebrowText != null && !eyebrowText.isEmpty()) {
            TextView eyebrow = ui.tv(eyebrowText, 12);
            eyebrow.setTextColor(theme.mutedTextColor());
            eyebrow.setPadding(0, 0, 0, ui.px(4));
            box.addView(eyebrow);
        }

        TextView title = ui.tv(titleText, 28);
        title.setPadding(0, 0, 0, ui.px(4));
        box.addView(title);

        TextView subtitle = new TextView(activity);
        subtitle.setText(subtitleText);
        subtitle.setTextSize(ui.sp(14));
        subtitle.setTextColor(theme.secondaryTextColor());
        subtitle.setPadding(0, 0, 0, ui.px(16));
        box.addView(subtitle);
    }

    private LinearLayout createCard(int fillColor) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(ui.px(16), ui.px(16), ui.px(16), ui.px(16));
        card.setBackground(ui.makeRoundedCard(fillColor, theme.borderColor()));
        card.setElevation(ui.px(1));
        return card;
    }

    private void addEyebrow(LinearLayout card, String text) {
        card.addView(ui.chip(text, theme.surfaceAltColor(), theme.mutedTextColor()));
    }

    private void addMetaRow(LinearLayout card, String left, String right) {
        LinearLayout metaRow = new LinearLayout(activity);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setWeightSum(2);
        metaRow.setPadding(0, 0, 0, ui.px(12));

        TextView leftMeta = new TextView(activity);
        leftMeta.setText(left);
        leftMeta.setTextSize(ui.sp(13));
        leftMeta.setTextColor(theme.mutedTextColor());
        metaRow.addView(leftMeta, new LinearLayout.LayoutParams(0, -2, 1));

        TextView rightMeta = new TextView(activity);
        rightMeta.setText(right);
        rightMeta.setTextSize(ui.sp(13));
        rightMeta.setTextColor(theme.mutedTextColor());
        rightMeta.setGravity(android.view.Gravity.END);
        metaRow.addView(rightMeta, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(metaRow);
    }

    private void addChipRow(LinearLayout card, View... chips) {
        LinearLayout chipRow = new LinearLayout(activity);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(0, 0, 0, ui.px(12));
        for (View chip : chips) {
            chipRow.addView(chip);
        }
        card.addView(chipRow);
    }

    private LinearLayout.LayoutParams cardLayoutParams() {
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.bottomMargin = ui.px(14);
        return cardLp;
    }

    private LinearLayout emptyState(String titleText, String bodyText) {
        LinearLayout empty = new LinearLayout(activity);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setPadding(ui.px(16), ui.px(16), ui.px(16), ui.px(16));
        empty.setBackground(ui.makeRoundedCard(theme.surfaceAltColor(), theme.borderColor()));

        TextView emptyTitle = ui.tv(titleText, 18);
        emptyTitle.setPadding(0, 0, 0, ui.px(4));
        empty.addView(emptyTitle);

        TextView emptyBody = new TextView(activity);
        emptyBody.setText(bodyText);
        emptyBody.setTextSize(ui.sp(14));
        emptyBody.setTextColor(theme.secondaryTextColor());
        empty.addView(emptyBody);

        LinearLayout.LayoutParams emptyLp = new LinearLayout.LayoutParams(-1, -2);
        emptyLp.topMargin = ui.px(4);
        empty.setLayoutParams(emptyLp);
        return empty;
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

    private String firstItemPreview(Tracker tracker) {
        if (tracker.items.isEmpty()) {
            return "Noch keine Items angelegt.";
        }

        StringBuilder builder = new StringBuilder();
        for (Item item : tracker.items) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(item.title == null || item.title.trim().isEmpty() ? "Ohne Titel" : item.title);
            if (builder.length() > 90) {
                break;
            }
        }
        return builder.toString();
    }

    private String date(long millis) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                .format(new java.util.Date(millis));
    }

    public interface SessionClick {
        void open(long sessionId);
    }

    public interface TrackerClick {
        void open(long trackerId);
    }
}

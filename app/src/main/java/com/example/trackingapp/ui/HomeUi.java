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
        box.addView(screenHeader(null, "Deine Läufe"));

        java.util.List<Session> sessions = db.sessions();
        for (Session session : sessions) {
            Tracker tracker = db.readTracker(session.trackerId);
            if (tracker == null) {
                continue;
            }

            int recordCount = db.recordCount(session.id);
            boolean open = "open".equals(session.status);
            LinearLayout card = createCard();
            ui.addSectionHeader(card, null, tracker.name, null);

            card.addView(ui.metaRow(date(session.createdAt), recordCount + "/" + tracker.items.size() + " Items"));
            card.addView(ui.chipRow(
                    ui.chip(session.status.equals("open") ? "Offen" : "Abgeschlossen",
                            open ? theme.accentSoftColor() : theme.surfaceAltColor(),
                            open ? theme.accentColor() : theme.primaryTextColor()),
                    ui.chip(recordCount + "/" + tracker.items.size(),
                            theme.accentSoftColor(), theme.accentColor())));

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
            box.addView(emptyState("Noch keine Sessions vorhanden", null));
        }

        body.addView(scrollView);
    }

    public void renderTrackers(FrameLayout body) {
        ScrollView scrollView = createScrollView();
        LinearLayout box = createListBox(scrollView);
        box.addView(screenHeader("TRACKER", "Tracker"));

        java.util.List<Tracker> trackers = db.trackers();
        for (Tracker tracker : trackers) {
            int itemCount = tracker.items.size();
            int fieldCount = 0;
            for (Item item : tracker.items) {
                fieldCount += item.fields.size();
            }

            LinearLayout card = createCard();
            ui.addSectionHeader(card, null, tracker.name == null || tracker.name.trim().isEmpty() ? "Unbenannter Tracker" : tracker.name, null);

            card.addView(ui.metaRow(itemCount + " Items", fieldCount + " Fields"));
            card.addView(ui.chipRow(
                    ui.chip(itemCount == 0 ? "Leer" : itemCount + " Items",
                            itemCount == 0 ? theme.withAlpha(theme.accentColor(), 0x18) : theme.accentSoftColor(),
                            itemCount == 0 ? theme.primaryTextColor() : theme.accentColor()),
                    ui.chip(fieldCount + " Fields", theme.accentSoftColor(), theme.accentColor())));

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

    private LinearLayout screenHeader(String eyebrowText, String titleText) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(ui.px(16), ui.px(16), ui.px(16), ui.px(16));
        card.setBackground(ui.makeRoundedCard(theme.surfaceColor(), theme.borderColor()));
        card.setElevation(ui.px(1));
        ui.addSectionHeader(card, eyebrowText, titleText, null);
        return card;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(ui.px(16), ui.px(16), ui.px(16), ui.px(16));
        card.setBackground(ui.makeRoundedCard(theme.surfaceColor(), theme.borderColor()));
        card.setElevation(ui.px(1));
        return card;
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

        if (bodyText != null && !bodyText.isEmpty()) {
            TextView emptyBody = new TextView(activity);
            emptyBody.setText(bodyText);
            emptyBody.setTextSize(ui.sp(14));
            emptyBody.setTextColor(theme.secondaryTextColor());
            empty.addView(emptyBody);
        }

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

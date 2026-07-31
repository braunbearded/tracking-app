package com.zaelio.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import com.zaelio.app.theme.ThemeStore;
import com.zaelio.app.ui.AppUi;

import java.util.function.LongConsumer;

import org.json.JSONObject;

public final class HomeUi {
    private static final int DELETE_SWIPE_LEFT_DP = 120;
    private static final int DELETE_SWIPE_RIGHT_DP = 60;
    private static final int DELETE_SWIPE_TRIGGER_DP = 80;

    private final Activity activity;
    private final TrackingDatabase db;
    private final ThemeStore theme;
    private final AppUi ui;
    private final LongConsumer openSession;
    private final LongConsumer editTracker;
    private final Runnable refresh;

    private interface DeleteAction {
        void request(Runnable restore, Runnable animateDelete);
    }

    public HomeUi(Activity activity, TrackingDatabase db, ThemeStore theme, AppUi ui,
                  LongConsumer openSession, LongConsumer editTracker, Runnable refresh) {
        this.activity = activity;
        this.db = db;
        this.theme = theme;
        this.ui = ui;
        this.openSession = openSession;
        this.editTracker = editTracker;
        this.refresh = refresh;
    }

    public void renderSessions(FrameLayout body) {
        ScrollView scrollView = createScrollView();
        LinearLayout box = createListBox(scrollView);

        java.util.List<Session> sessions = db.sessions();
        for (Session session : sessions) {
            Tracker tracker = db.readTracker(session.trackerId);
            if (tracker == null) {
                continue;
            }

            LinearLayout card = overviewCard(
                    tracker.name,
                    date(session.createdAt),
                    preview(session.id, tracker),
                    () -> openSession.accept(session.id),
                    null,
                    (restore, animateDelete) -> confirmDeleteSession(session, restore, animateDelete),
                    box,
                    () -> db.reorderSessions(childIds(box)));
            card.setTag(session.id);
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

        java.util.List<Tracker> trackers = db.trackers();
        for (Tracker tracker : trackers) {
            LinearLayout card = overviewCard(
                    tracker.name == null || tracker.name.trim().isEmpty() ? "Unbenannter Tracker" : tracker.name,
                    null,
                    fieldPreview(tracker),
                    () -> editTracker.accept(tracker.id),
                    () -> duplicateTracker(tracker),
                    (restore, animateDelete) -> confirmDeleteTracker(tracker, restore, animateDelete),
                    box,
                    () -> db.reorderTrackers(childIds(box)));
            card.setTag(tracker.id);
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
        box.setPadding(ui.spaceL(), ui.spaceM(), ui.spaceL(), ui.bottomSafePadding());
        scrollView.addView(box);
        return box;
    }

    private LinearLayout overviewCard(String title, String meta, String previewText, Runnable open,
                                      Runnable duplicateAction, DeleteAction deleteAction,
                                      LinearLayout reorderContainer, Runnable onReorder) {
        LinearLayout card = createCard();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        final boolean[] skipClick = new boolean[1];
        card.setOnClickListener(v -> {
            if (skipClick[0]) {
                skipClick[0] = false;
                return;
            }
            open.run();
        });
        attachDeleteGestures(card, deleteAction, skipClick);

        TextView handle = ui.listIcon("⠿");

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(activity);
        titleView.setText(title);
        titleView.setTextSize(ui.sp(16));
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setTextColor(theme.primaryTextColor());
        content.addView(titleView);

        if (meta != null && !meta.isEmpty()) {
            TextView metaView = new TextView(activity);
            metaView.setText(meta);
            metaView.setTextSize(ui.sp(13));
            metaView.setTextColor(theme.mutedTextColor());
            content.addView(metaView);
        }

        TextView preview = new TextView(activity);
        preview.setText(previewText);
        preview.setTextSize(ui.sp(14));
        preview.setTextColor(theme.primaryTextColor());
        preview.setLineSpacing(0f, 1.15f);
        preview.setMaxLines(2);
        preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        content.addView(preview);

        TextView menu = ui.listIcon("...");
        menu.setOnClickListener(v -> showCardMenu(menu, duplicateAction, () -> deleteAction.request(() -> {}, () -> animateDelete(card))));

        TextView arrow = ui.listIcon("›");
        arrow.setOnClickListener(v -> open.run());

        card.addView(ui.listRow(handle, content, menu, arrow), new LinearLayout.LayoutParams(-1, -2));
        attachOverviewReorder(handle, reorderContainer, card, onReorder);
        return card;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(activity);
        card.setPadding(ui.spaceM(), ui.spaceS(), ui.spaceS(), ui.spaceS());
        card.setBackground(ui.makeRoundedCard(theme.surfaceColor(), theme.accentSoftColor()));
        card.setElevation(ui.strokeWidth());
        return card;
    }

    private void showCardMenu(View anchor, Runnable duplicate, Runnable delete) {
        PopupMenu menu = new PopupMenu(activity, anchor, Gravity.END);
        if (duplicate != null) {
            menu.getMenu().add(0, 1, 0, "Duplizieren");
        }
        menu.getMenu().add(0, 2, 1, "Löschen");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1 && duplicate != null) {
                duplicate.run();
            } else {
                delete.run();
            }
            return true;
        });
        menu.show();
    }

    private LinearLayout.LayoutParams cardLayoutParams() {
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.bottomMargin = ui.spaceMl();
        return cardLp;
    }

    private LinearLayout emptyState(String titleText, String bodyText) {
        LinearLayout empty = new LinearLayout(activity);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setPadding(ui.spaceL(), ui.spaceL(), ui.spaceL(), ui.spaceL());
        empty.setBackground(ui.makeRoundedCard(theme.surfaceAltColor(), theme.borderColor()));

        TextView emptyTitle = ui.tv(titleText, 18);
        emptyTitle.setPadding(0, 0, 0, ui.spaceXs());
        empty.addView(emptyTitle);

        if (bodyText != null && !bodyText.isEmpty()) {
            TextView emptyBody = new TextView(activity);
            emptyBody.setText(bodyText);
            emptyBody.setTextSize(ui.sp(14));
            emptyBody.setTextColor(theme.secondaryTextColor());
            empty.addView(emptyBody);
        }

        LinearLayout.LayoutParams emptyLp = new LinearLayout.LayoutParams(-1, -2);
        emptyLp.topMargin = ui.spaceXs();
        empty.setLayoutParams(emptyLp);
        return empty;
    }

    private void attachOverviewReorder(View handle, LinearLayout container, View movedView, Runnable onChange) {
        ReorderHelper.attach(ui, handle, container, movedView, onChange);
    }

    private java.util.List<Long> childIds(LinearLayout container) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            Object tag = container.getChildAt(i).getTag();
            if (tag instanceof Long) {
                ids.add((Long) tag);
            }
        }
        return ids;
    }

    private void attachDeleteGestures(View card, DeleteAction deleteAction, boolean[] skipClick) {
        final float[] downX = new float[1];
        final boolean[] dragging = new boolean[1];
        final boolean[] deleteStarted = new boolean[1];
        card.setOnLongClickListener(v -> {
            deleteStarted[0] = true;
            deleteAction.request(markDeleteCandidate(card), () -> animateDelete(card));
            return true;
        });
        card.setOnTouchListener((v, event) -> {
            float dx = event.getRawX() - downX[0];
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX[0] = event.getRawX();
                dragging[0] = false;
                deleteStarted[0] = false;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(dx) > ui.spaceS()) {
                    dragging[0] = true;
                    card.getParent().requestDisallowInterceptTouchEvent(true);
                    card.setTranslationX(Math.max(-ui.px(DELETE_SWIPE_LEFT_DP), Math.min(ui.px(DELETE_SWIPE_RIGHT_DP), dx)));
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                card.animate().translationX(0).setDuration(120).start();
                if (dragging[0]) {
                    skipClick[0] = true;
                    if (!deleteStarted[0] && dx < -ui.px(DELETE_SWIPE_TRIGGER_DP)) {
                        deleteStarted[0] = true;
                        deleteAction.request(markDeleteCandidate(card), () -> animateDelete(card));
                    }
                    return true;
                }
            }
            return false;
        });
    }

    private Runnable markDeleteCandidate(View card) {
        Drawable background = card.getBackground();
        vibrate(card);
        setStrikeThrough(card, true);
        card.setBackground(ui.makeRoundedCard(theme.cautionFillColor(), theme.cautionStrokeColor()));
        card.animate().scaleX(0.98f).scaleY(0.98f).alpha(0.9f).setDuration(80).start();
        return () -> {
            setStrikeThrough(card, false);
            card.setBackground(background);
            card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(80).start();
        };
    }

    private void animateDelete(View card) {
        card.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).translationX(-ui.spaceXl()).setDuration(160).start();
    }

    private void vibrate(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager == null ? null : manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(60, 180));
        } else {
            vibrator.vibrate(60);
        }
    }

    private void setStrikeThrough(View view, boolean enabled) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int flag = Paint.STRIKE_THRU_TEXT_FLAG;
            textView.setPaintFlags(enabled ? textView.getPaintFlags() | flag : textView.getPaintFlags() & ~flag);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setStrikeThrough(group.getChildAt(i), enabled);
            }
        }
    }

    private void confirmDeleteSession(Session session, Runnable restore, Runnable animateDelete) {
        ui.confirmDelete("Session löschen", "Diese Session wirklich löschen?", () -> {
            animateDelete.run();
            activity.getWindow().getDecorView().postDelayed(() -> {
                db.deleteSession(session.id);
                refresh.run();
            }, 170);
        }, restore);
    }

    private void confirmDeleteTracker(Tracker tracker, Runnable restore, Runnable animateDelete) {
        String name = tracker.name == null || tracker.name.trim().isEmpty() ? "Diesen Tracker" : tracker.name;
        ui.confirmDelete("Tracker löschen", name + " wirklich löschen?", () -> {
            animateDelete.run();
            activity.getWindow().getDecorView().postDelayed(() -> {
                db.deleteTracker(tracker.id);
                refresh.run();
            }, 170);
        }, restore);
    }

    private void duplicateTracker(Tracker tracker) {
        try {
            JSONObject json = new JSONObject(JsonUtil.trackerToJson(tracker));
            json.put("name", (tracker.name == null || tracker.name.trim().isEmpty() ? "Unbenannter Tracker" : tracker.name) + " Kopie");
            TrackerJsonRepository.saveTracker(db, -1, json.toString(), true);
            refresh.run();
        } catch (Exception e) {
            android.widget.Toast.makeText(activity, e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private String preview(long sessionId, Tracker tracker) {
        java.util.Map<Long, FieldRecord> records = db.records(sessionId);
        StringBuilder builder = new StringBuilder();
        for (FieldDefinition field : tracker.fields) {
            FieldRecord record = records.get(field.id);
            if (record == null) {
                continue;
            }

            java.util.Map<String, Object> values = JsonUtil.toMap(record.valuesJson);
            Object value = values.get(field.key);
            if (value == null || String.valueOf(value).trim().isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(field.label == null || field.label.trim().isEmpty() ? field.key : field.label)
                    .append(": ")
                    .append(formatValue(field, value));
            if (builder.length() > 110) {
                break;
            }
        }
        return builder.length() == 0 ? "Noch keine Werte eingetragen." : builder.toString();
    }

    private String formatValue(FieldDefinition field, Object value) {
        if ("duration".equals(field.type)) {
            long millis = value instanceof Number ? ((Number) value).longValue() : parseLong(value);
            return FormatUtil.formatMs(millis);
        }
        if ("float".equals(field.type) && value instanceof Number) {
            return String.format(java.util.Locale.US, "%." + field.decimals + "f", ((Number) value).doubleValue())
;
        }
        return String.valueOf(value);
    }

    private long parseLong(Object value) {
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private String fieldPreview(Tracker tracker) {
        if (tracker.fields.isEmpty()) {
            return "Noch keine Felder angelegt.";
        }

        StringBuilder builder = new StringBuilder();
        for (FieldDefinition field : tracker.fields) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(field.label == null || field.label.trim().isEmpty() ? "Ohne Label" : field.label);
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

}

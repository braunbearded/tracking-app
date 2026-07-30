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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import com.zaelio.app.theme.ThemeStore;
import com.zaelio.app.ui.AppUi;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

public final class HomeUi {
    private final Activity activity;
    private final TrackingDatabase db;
    private final ThemeStore theme;
    private final AppUi ui;
    private final LongConsumer openSession;
    private final LongConsumer editTracker;
    private final Runnable refresh;

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
                    restore -> confirmDeleteSession(session, restore));
            card.setTag(session.id);
            attachOverviewReorder(card.getChildAt(0), box, card, () -> db.reorderSessions(childIds(box)));
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
                    restore -> confirmDeleteTracker(tracker, restore));
            card.setTag(tracker.id);
            attachOverviewReorder(card.getChildAt(0), box, card, () -> db.reorderTrackers(childIds(box)));
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

    private LinearLayout overviewCard(String title, String meta, String previewText, Runnable open, Consumer<Runnable> deleteAction) {
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

        card.addView(listIcon("⠿"), new LinearLayout.LayoutParams(ui.px(28), ui.px(56)));

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

        card.addView(content, new LinearLayout.LayoutParams(0, -2, 1));

        TextView menu = listIcon("...");
        menu.setOnClickListener(v -> showDeleteMenu(menu, () -> deleteAction.accept(() -> {})));
        card.addView(menu, new LinearLayout.LayoutParams(ui.px(36), ui.px(56)));

        TextView arrow = listIcon("›");
        arrow.setTextSize(ui.sp(24));
        arrow.setOnClickListener(v -> open.run());
        card.addView(arrow, new LinearLayout.LayoutParams(ui.px(28), ui.px(56)));
        return card;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(activity);
        card.setPadding(ui.px(12), ui.px(8), ui.px(8), ui.px(8));
        card.setBackground(ui.makeRoundedCard(theme.surfaceColor(), theme.accentSoftColor()));
        card.setElevation(ui.px(1));
        return card;
    }

    private TextView listIcon(String text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(ui.sp(24));
        view.setTextColor(theme.mutedTextColor());
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private void showDeleteMenu(View anchor, Runnable delete) {
        PopupMenu menu = new PopupMenu(activity, anchor, Gravity.END);
        menu.getMenu().add("Löschen");
        menu.setOnMenuItemClickListener(item -> {
            delete.run();
            return true;
        });
        menu.show();
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

    private void attachOverviewReorder(View handle, LinearLayout container, View movedView, Runnable onChange) {
        final float[] startY = new float[1];
        final float[] consumedY = new float[1];
        final float[] oldElevation = new float[1];
        handle.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                container.requestDisallowInterceptTouchEvent(true);
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
                    int distance = overviewReorderDistance(container, movedView, direction);
                    if (distance <= 0 || Math.abs(delta) <= distance / 2f) {
                        break;
                    }
                    if (!moveOverviewCard(container, movedView, direction)) {
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
                container.requestDisallowInterceptTouchEvent(false);
                return true;
            }
            return false;
        });
    }

    private boolean moveOverviewCard(LinearLayout container, View movedView, int direction) {
        int fromIndex = container.indexOfChild(movedView);
        int toIndex = fromIndex + direction;
        if (fromIndex < 0 || toIndex < 0 || toIndex >= container.getChildCount()) {
            return false;
        }
        View sibling = container.getChildAt(toIndex);
        int distance = viewHeightWithMargins(sibling);
        container.removeView(sibling);
        container.addView(sibling, direction > 0 ? fromIndex : toIndex + 1);
        sibling.animate().cancel();
        sibling.setTranslationY(direction > 0 ? distance : -distance);
        sibling.animate().translationY(0).setDuration(140).start();
        return true;
    }

    private int overviewReorderDistance(LinearLayout container, View movedView, int direction) {
        int fromIndex = container.indexOfChild(movedView);
        int toIndex = fromIndex + direction;
        return fromIndex < 0 || toIndex < 0 || toIndex >= container.getChildCount()
                ? 0
                : viewHeightWithMargins(container.getChildAt(toIndex));
    }

    private int viewHeightWithMargins(View view) {
        int height = view.getHeight();
        if (view.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
            height += lp.topMargin + lp.bottomMargin;
        }
        return height;
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

    private void attachDeleteGestures(View card, Consumer<Runnable> deleteAction, boolean[] skipClick) {
        final float[] downX = new float[1];
        final boolean[] dragging = new boolean[1];
        final boolean[] deleteStarted = new boolean[1];
        card.setOnLongClickListener(v -> {
            deleteStarted[0] = true;
            deleteAction.accept(markDeleteCandidate(card));
            return true;
        });
        card.setOnTouchListener((v, event) -> {
            float dx = event.getRawX() - downX[0];
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX[0] = event.getRawX();
                dragging[0] = false;
                deleteStarted[0] = false;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(dx) > ui.px(8)) {
                    dragging[0] = true;
                    card.getParent().requestDisallowInterceptTouchEvent(true);
                    card.setTranslationX(Math.max(-ui.px(120), Math.min(ui.px(60), dx)));
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                card.animate().translationX(0).setDuration(120).start();
                if (dragging[0]) {
                    skipClick[0] = true;
                    if (!deleteStarted[0] && dx < -ui.px(80)) {
                        deleteStarted[0] = true;
                        deleteAction.accept(markDeleteCandidate(card));
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

    private void confirmDeleteSession(Session session, Runnable restore) {
        showDeleteDialog("Session löschen", "Diese Session wirklich löschen?", () -> {
            db.deleteSession(session.id);
            refresh.run();
        }, restore);
    }

    private void confirmDeleteTracker(Tracker tracker, Runnable restore) {
        String name = tracker.name == null || tracker.name.trim().isEmpty() ? "Diesen Tracker" : tracker.name;
        showDeleteDialog("Tracker löschen", name + " wirklich löschen?", () -> {
            db.deleteTracker(tracker.id);
            refresh.run();
        }, restore);
    }

    private void showDeleteDialog(String title, String message, Runnable onDelete, Runnable onDismiss) {
        LinearLayout card = ui.contentCard();
        card.setPadding(ui.px(20), ui.px(18), ui.px(20), ui.px(16));

        ui.addDialogTitle(card, title);
        ui.addDialogMessage(card, message);

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel = ui.secondaryButton("Abbrechen");
        Button delete = ui.dangerButton("Löschen");
        buttons.addView(cancel, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams deleteLp = new LinearLayout.LayoutParams(0, -2, 1);
        deleteLp.leftMargin = ui.px(8);
        buttons.addView(delete, deleteLp);
        card.addView(buttons);

        final androidx.appcompat.app.AlertDialog[] dialog = new androidx.appcompat.app.AlertDialog[1];
        dialog[0] = ui.showCardDialog(card);
        dialog[0].setOnDismissListener(d -> onDismiss.run());
        cancel.setOnClickListener(v -> dialog[0].dismiss());
        delete.setOnClickListener(v -> {
            dialog[0].dismiss();
            onDelete.run();
        });
    }

    private String preview(long sessionId, Tracker tracker) {
        java.util.Map<Long, ItemRecord> records = db.records(sessionId);
        StringBuilder builder = new StringBuilder();
        for (FieldDefinition field : tracker.fields) {
            ItemRecord record = records.get(field.id);
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
            long seconds = millis / 1000;
            return String.format(java.util.Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
        }
        if ("float".equals(field.type) && value instanceof Number) {
            return String.format(java.util.Locale.US, "%." + field.decimals + "f", ((Number) value).doubleValue())
                    + unitSuffix(field);
        }
        return String.valueOf(value) + unitSuffix(field);
    }

    private long parseLong(Object value) {
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private String unitSuffix(FieldDefinition field) {
        return field.unit == null || field.unit.trim().isEmpty() || "string".equals(field.type) || "duration".equals(field.type)
                ? ""
                : " " + field.unit;
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

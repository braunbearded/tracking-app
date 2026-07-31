package com.zaelio.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zaelio.app.theme.ThemeStore;
import com.zaelio.app.ui.AppUi;

final class DeleteGestureHelper {
    private static final int DELETE_SWIPE_LEFT_DP = 180;
    private static final int DELETE_SWIPE_RIGHT_DP = 60;
    private static final int DELETE_SWIPE_TRIGGER_DP = 140;

    interface DeleteAction {
        void request(Runnable restore, Runnable animateDelete);
    }

    private DeleteGestureHelper() {
    }

    static void attachToTree(Activity activity, ThemeStore theme, AppUi ui, View rootView, View targetView,
                             DeleteAction deleteAction, boolean[] skipClick, View... excluded) {
        if (isExcluded(rootView, excluded)) {
            return;
        }
        attach(activity, theme, ui, rootView, targetView, deleteAction, skipClick);
        if (rootView instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) rootView;
            for (int i = 0; i < group.getChildCount(); i++) {
                attachToTree(activity, theme, ui, group.getChildAt(i), targetView, deleteAction, skipClick, excluded);
            }
        }
    }

    static void attach(Activity activity, ThemeStore theme, AppUi ui, View touchView, View targetView,
                       DeleteAction deleteAction, boolean[] skipClick) {
        final float[] downX = new float[1];
        final float[] downY = new float[1];
        final boolean[] dragging = new boolean[1];
        final boolean[] deleteStarted = new boolean[1];
        touchView.setOnLongClickListener(v -> {
            if (dragging[0]) {
                return true;
            }
            deleteStarted[0] = true;
            deleteAction.request(markDeleteCandidate(activity, theme, ui, targetView), () -> animateDelete(ui, targetView));
            return true;
        });
        touchView.setOnTouchListener((v, event) -> {
            float dx = event.getRawX() - downX[0];
            float dy = event.getRawY() - downY[0];
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX[0] = event.getRawX();
                downY[0] = event.getRawY();
                dragging[0] = false;
                deleteStarted[0] = false;
                touchView.setLongClickable(true);
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(dx) > ui.spaceS()) {
                    dragging[0] = true;
                    touchView.setLongClickable(false);
                    if (touchView.getParent() != null) {
                        touchView.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    targetView.setTranslationX(Math.max(-ui.px(DELETE_SWIPE_LEFT_DP), Math.min(ui.px(DELETE_SWIPE_RIGHT_DP), dx)));
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                touchView.setLongClickable(true);
                targetView.animate().translationX(0).setDuration(120).start();
                if (dragging[0]) {
                    if (skipClick != null) {
                        skipClick[0] = true;
                    }
                    if (!deleteStarted[0] && dx < -ui.px(DELETE_SWIPE_TRIGGER_DP) && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                        deleteStarted[0] = true;
                        deleteAction.request(markDeleteCandidate(activity, theme, ui, targetView), () -> animateDelete(ui, targetView));
                    }
                    return true;
                }
            }
            return false;
        });
    }

    private static boolean isExcluded(View view, View... excluded) {
        if (excluded == null) {
            return false;
        }
        for (View excludedView : excluded) {
            if (view == excludedView) {
                return true;
            }
        }
        return false;
    }

    static void animateDelete(AppUi ui, View targetView) {
        targetView.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).translationX(-ui.spaceXl()).setDuration(160).start();
    }

    private static Runnable markDeleteCandidate(Activity activity, ThemeStore theme, AppUi ui, View targetView) {
        Drawable background = targetView.getBackground();
        vibrate(activity, targetView);
        setStrikeThrough(targetView, true);
        targetView.setBackground(ui.makeRoundedCard(theme.cautionFillColor(), theme.cautionStrokeColor()));
        targetView.animate().scaleX(0.98f).scaleY(0.98f).alpha(0.9f).setDuration(80).start();
        return () -> {
            setStrikeThrough(targetView, false);
            targetView.setBackground(background);
            targetView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(80).start();
        };
    }

    private static void vibrate(Activity activity, View view) {
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

    private static void setStrikeThrough(View view, boolean enabled) {
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
}

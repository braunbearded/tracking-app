package com.zaelio.app;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.LinearLayout;

import com.zaelio.app.ui.AppUi;

import java.util.function.IntConsumer;

final class ReorderHelper {
    private static final long ANIMATION_MS = 140;

    private ReorderHelper() {
    }

    static void attach(AppUi ui, View handle, LinearLayout container, View movedView, Runnable onChange) {
        attach(ui, handle, container, movedView, onChange, null);
    }

    static void attach(AppUi ui, View handle, LinearLayout container, View movedView, Runnable onChange, IntConsumer afterSwap) {
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
                movedView.setElevation(oldElevation[0] + ui.spaceS());
                movedView.setAlpha(0.82f);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float delta = event.getRawY() - startY[0] - consumedY[0];
                int direction = delta > 0 ? 1 : -1;
                while (delta != 0) {
                    int distance = distance(container, movedView, direction);
                    if (distance <= 0 || Math.abs(delta) <= distance / 2f) {
                        break;
                    }
                    distance = swapSibling(container, movedView, direction);
                    if (distance <= 0) {
                        break;
                    }
                    if (afterSwap != null) {
                        afterSwap.accept(direction);
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

    private static int distance(LinearLayout container, View movedView, int direction) {
        int fromIndex = container.indexOfChild(movedView);
        int toIndex = fromIndex + direction;
        return fromIndex < 0 || toIndex < 0 || toIndex >= container.getChildCount()
                ? 0
                : heightWithMargins(container.getChildAt(toIndex));
    }

    private static int swapSibling(LinearLayout container, View movedView, int direction) {
        int fromIndex = container.indexOfChild(movedView);
        int toIndex = fromIndex + direction;
        if (fromIndex < 0 || toIndex < 0 || toIndex >= container.getChildCount()) {
            return 0;
        }
        View sibling = container.getChildAt(toIndex);
        int distance = heightWithMargins(sibling);
        container.removeView(sibling);
        container.addView(sibling, direction > 0 ? fromIndex : toIndex + 1);
        sibling.animate().cancel();
        sibling.setTranslationY(direction > 0 ? distance : -distance);
        sibling.animate().translationY(0).setDuration(ANIMATION_MS).start();
        return distance;
    }

    private static int heightWithMargins(View view) {
        int height = view.getHeight();
        if (view.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
            height += lp.topMargin + lp.bottomMargin;
        }
        return height;
    }

    private static void disallowParentIntercept(View view, boolean disallow) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
            parent = parent.getParent();
        }
    }
}

package com.zaelio.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zaelio.app.theme.ThemeStore;
import com.zaelio.app.ui.AppUi;

import java.util.Locale;
import java.util.Map;

final class FieldInputUi {
    private final Activity activity;
    private final ThemeStore theme;
    private final AppUi ui;
    private final Handler handler;
    private final Map<String, Long> timers;

    FieldInputUi(Activity activity, ThemeStore theme, AppUi ui, Handler handler, Map<String, Long> timers) {
        this.activity = activity;
        this.theme = theme;
        this.ui = ui;
        this.handler = handler;
        this.timers = timers;
    }

    void fieldControl(
            LinearLayout box,
            FieldDefinition field,
            Map<String, Object> values,
            Map<String, View> inputs,
            boolean readOnly,
            Runnable onChange) {
        LinearLayout fieldBox = new LinearLayout(activity);
        fieldBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams fieldBoxLp = new LinearLayout.LayoutParams(-1, -2);
        fieldBoxLp.bottomMargin = ui.px(18);
        box.addView(fieldBox, fieldBoxLp);

        TextView label = new TextView(activity);
        label.setText(field.label + (field.unit == null || field.unit.isEmpty() ? "" : " (" + field.unit + ")"));
        label.setTextSize(ui.sp(15));
        label.setTextColor(theme.primaryTextColor());
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(0, 0, 0, ui.px(8));
        fieldBox.addView(label);

        Object value = values.get(field.key);
        if ("string".equals(field.type)) {
            stringControl(fieldBox, field, value, inputs, readOnly, onChange);
            return;
        }
        if ("duration".equals(field.type)) {
            timerControl(fieldBox, field, value, inputs, readOnly, onChange);
            return;
        }
        numericControl(fieldBox, field, value, inputs, readOnly, onChange);
    }

    private void stringControl(LinearLayout fieldBox, FieldDefinition field, Object value, Map<String, View> inputs, boolean readOnly, Runnable onChange) {
        EditText editText = styledEditText(value);
        editText.setSingleLine(false);
        editText.setMinLines(stringMinLines());
        editText.setMaxLines(stringMaxLines());
        editText.setGravity(Gravity.TOP | Gravity.START);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setPadding(ui.px(12), ui.px(10), ui.px(12), ui.px(10));
        editText.setEnabled(!readOnly);
        if (!readOnly) {
            watchTextChange(editText, onChange);
        }
        fieldBox.addView(editText, new LinearLayout.LayoutParams(-1, -2));
        inputs.put(field.key, editText);
    }

    private void timerControl(LinearLayout fieldBox, FieldDefinition field, Object value, Map<String, View> inputs, boolean readOnly, Runnable onChange) {
        TextView display = new TextView(activity);
        display.setText(formatMs(toLong(value)));
        display.setTextSize(ui.sp(24));
        display.setTextColor(theme.primaryTextColor());
        display.setGravity(Gravity.CENTER);
        display.setTag(toLong(value));
        int timerHeight = numericHeight();
        fieldBox.addView(display, new LinearLayout.LayoutParams(-1, timerHeight));

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Button toggle = timers.containsKey(field.key) ? ui.dangerButton("Stop") : ui.primaryButton("Start");
        Button reset = ui.ghostButton("Reset");
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(0, timerHeight, 2f);
        toggleLp.rightMargin = ui.px(8);
        row.addView(toggle, toggleLp);
        row.addView(reset, new LinearLayout.LayoutParams(0, timerHeight, 1f));
        fieldBox.addView(row);

        toggle.setEnabled(!readOnly);
        reset.setEnabled(!readOnly);

        toggle.setOnClickListener(v -> {
            if (timers.containsKey(field.key)) {
                timers.remove(field.key);
                styleTimerToggle(toggle, false);
            } else {
                long current = (Long) display.getTag();
                timers.put(field.key, System.currentTimeMillis() - current);
                styleTimerToggle(toggle, true);
                tick(display, field.key, onChange);
            }
            onChange.run();
        });
        reset.setOnClickListener(v -> {
            timers.remove(field.key);
            styleTimerToggle(toggle, false);
            display.setTag(0L);
            display.setText(formatMs(0));
            onChange.run();
        });

        inputs.put(field.key, display);
    }

    private void numericControl(LinearLayout fieldBox, FieldDefinition field, Object value, Map<String, View> inputs, boolean readOnly, Runnable onChange) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Button minus = ui.secondaryButton("−");
        Button plus = ui.primaryButton("+");
        EditText editText = styledEditText(value);
        editText.setInputType("int".equals(field.type)
                ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED
                : InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        editText.setGravity(Gravity.CENTER);
        editText.setTextSize(ui.sp(14));
        editText.setPadding(ui.px(12), 0, ui.px(12), 0);
        editText.setEnabled(!readOnly);
        minus.setEnabled(!readOnly);
        plus.setEnabled(!readOnly);

        View.OnClickListener adjust = v -> {
            hideKeyboard(editText);
            double current = parseDoubleSafe(editText.getText().toString(), 0);
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

        int inputHeight = numericHeight();
        LinearLayout.LayoutParams minusLp = new LinearLayout.LayoutParams(0, inputHeight, 1f);
        minusLp.rightMargin = ui.px(8);
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(numericValueWidth(), inputHeight);
        editLp.rightMargin = ui.px(8);
        row.addView(minus, minusLp);
        row.addView(editText, editLp);
        row.addView(plus, new LinearLayout.LayoutParams(0, inputHeight, 1f));
        fieldBox.addView(row);
        inputs.put(field.key, editText);
    }

    private EditText styledEditText(Object value) {
        EditText editText = new EditText(activity);
        editText.setText(value == null ? "" : String.valueOf(value));
        editText.setTextColor(theme.primaryTextColor());
        editText.setHintTextColor(theme.mutedTextColor());
        editText.setMinHeight(ui.px(48));
        editText.setBackground(ui.makeRoundedCard(theme.surfaceColor(), theme.borderColor()));
        return editText;
    }

    private void styleTimerToggle(Button button, boolean running) {
        button.setText(running ? "Stop" : "Start");
        button.setTextColor(running ? 0xffb42318 : Color.WHITE);
        button.setBackgroundTintList(ColorStateList.valueOf(running ? theme.cautionFillColor() : theme.accentColor()));
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

    private void watchTextChange(EditText editText, Runnable onChange) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChange.run(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private int stringMinLines() {
        String size = theme.fieldSize();
        return "large".equals(size) ? 3 : "compact".equals(size) ? 1 : 2;
    }

    private int stringMaxLines() {
        String size = theme.fieldSize();
        return "large".equals(size) ? 10 : "compact".equals(size) ? 3 : 6;
    }

    private int numericHeight() {
        String size = theme.fieldSize();
        return ui.px("large".equals(size) ? 64 : "compact".equals(size) ? 48 : 56);
    }

    private int numericValueWidth() {
        String size = theme.fieldSize();
        return ui.px("large".equals(size) ? 112 : "compact".equals(size) ? 72 : 88);
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

    private double parseDoubleSafe(String text, double fallback) {
        try {
            return Double.parseDouble(text);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String formatMs(long millis) {
        long seconds = millis / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }
}

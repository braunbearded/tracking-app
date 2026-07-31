package com.zaelio.app.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.zaelio.app.R;
import com.zaelio.app.theme.ThemeStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

public final class AppUi {
    private static final int CORNER_RADIUS_DP = 6;
    private static final int SPACE_XS_DP = 4;
    private static final int SPACE_S_DP = 8;
    private static final int SPACE_SM_DP = 10;
    private static final int SPACE_M_DP = 12;
    private static final int SPACE_ML_DP = 14;
    private static final int SPACE_L_DP = 16;
    private static final int SPACE_DIALOG_Y_DP = 18;
    private static final int SPACE_XL_DP = 20;
    private static final int CHECK_ROW_HEIGHT_DP = 40;
    private static final int BUTTON_HEIGHT_DP = 48;
    private static final int ICON_BUTTON_SIZE_DP = 48;
    private static final int ROW_HEIGHT_DP = 56;
    private static final int DRAG_HANDLE_WIDTH_DP = 28;
    private static final int LIST_ACTION_WIDTH_DP = 36;
    private static final int BOTTOM_SAFE_PADDING_DP = 104;

    private final Activity activity;
    private final ThemeStore theme;

    public AppUi(Activity activity, ThemeStore theme) {
        this.activity = activity;
        this.theme = theme;
    }

    public TextView tv(String text, float sizeSp) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(sp(sizeSp));
        view.setPadding(px(16), px(12), px(16), px(8));
        view.setTextColor(theme.primaryTextColor());
        return view;
    }

    public Button button(String text, int fillColor, int textColor, int strokeColor) {
        MaterialButton button = new MaterialButton(activity);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(sp(14));
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setCornerRadius(px(CORNER_RADIUS_DP));
        button.setMinHeight(px(BUTTON_HEIGHT_DP));
        button.setMinimumHeight(px(BUTTON_HEIGHT_DP));
        button.setMinWidth(px(64));
        button.setPadding(px(16), 0, px(16), 0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundTintList(ColorStateList.valueOf(fillColor));
        button.setStrokeColor(ColorStateList.valueOf(strokeColor));
        button.setStrokeWidth(strokeColor == fillColor ? 0 : px(1));
        button.setRippleColor(ColorStateList.valueOf(theme.withAlpha(textColor, 0x22)));
        return button;
    }

    public Button primaryButton(String text) {
        return button(text, theme.accentColor(), Color.WHITE, theme.accentColor());
    }

    public Button secondaryButton(String text) {
        return button(text, theme.surfaceColor(), theme.primaryTextColor(), theme.borderColor());
    }

    public Button ghostButton(String text) {
        return button(text, theme.surfaceAltColor(), theme.primaryTextColor(), theme.borderColor());
    }

    public Button dangerButton(String text) {
        return button(text, theme.cautionFillColor(), 0xffb42318, theme.cautionStrokeColor());
    }

    public View bottomNav(boolean sessionsSelected, View.OnClickListener onSessions, View.OnClickListener onTracker) {
        LinearLayout nav = new LinearLayout(activity);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                px(68)));
        nav.setBackgroundColor(theme.navigationBarColor());
        nav.setElevation(px(8));
        nav.addView(navItem("Sessions", android.R.drawable.ic_menu_agenda, sessionsSelected, onSessions),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        nav.addView(navItem("Tracker", android.R.drawable.ic_menu_sort_by_size, !sessionsSelected, onTracker),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        return nav;
    }

    public View navItem(String label, int iconRes, boolean selected, View.OnClickListener onClick) {
        FrameLayout item = new FrameLayout(activity);
        item.setClickable(true);
        item.setFocusable(true);
        item.setBackground(squareRipple(theme.navigationBarColor(), theme.withAlpha(theme.accentColor(), 0x18)));
        item.setOnClickListener(onClick);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        content.setPadding(0, px(8), 0, px(8));

        Drawable icon = activity.getDrawable(iconRes);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(selected ? theme.accentColor() : theme.mutedTextColor());
        }

        ImageView iconView = new ImageView(activity);
        iconView.setImageDrawable(icon);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(px(20), px(20));
        content.addView(iconView, iconLp);

        TextView labelView = new TextView(activity);
        labelView.setText(label);
        labelView.setTextSize(sp(11));
        labelView.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        labelView.setIncludeFontPadding(false);
        labelView.setSingleLine();
        labelView.setTextColor(selected ? theme.accentColor() : theme.mutedTextColor());
        labelView.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        labelLp.topMargin = px(4);
        content.addView(labelView, labelLp);

        item.addView(content);
        return item;
    }

    public Button floatingActionButton(View.OnClickListener onClick) {
        MaterialButton fab = new MaterialButton(activity);
        fab.setText("+");
        fab.setAllCaps(false);
        fab.setTextSize(sp(26));
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setTextColor(0xffffffff);
        fab.setCornerRadius(px(CORNER_RADIUS_DP));
        fab.setBackgroundTintList(ColorStateList.valueOf(theme.accentColor()));
        fab.setRippleColor(ColorStateList.valueOf(theme.withAlpha(Color.WHITE, 0x22)));
        fab.setElevation(px(10));
        fab.setMinWidth(px(ICON_BUTTON_SIZE_DP));
        fab.setMinHeight(px(ICON_BUTTON_SIZE_DP));
        fab.setInsetTop(0);
        fab.setInsetBottom(0);
        fab.setPadding(0, 0, 0, px(4));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(ICON_BUTTON_SIZE_DP), px(ICON_BUTTON_SIZE_DP), Gravity.END | Gravity.BOTTOM);
        lp.rightMargin = px(20);
        lp.bottomMargin = px(20);
        fab.setLayoutParams(lp);
        fab.setOnClickListener(onClick);
        return fab;
    }

    public View appBar(String titleText, boolean showBack, Runnable onBack, boolean showOverflow, View.OnClickListener overflowClick) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setMinimumHeight(px(56));
        bar.setPadding(px(14), px(12), px(12), px(12));
        bar.setBackgroundColor(theme.surfaceColor());
        bar.setElevation(px(1));

        if (showBack) {
            bar.addView(iconButton(R.drawable.ic_arrow_back_24, "Zurück", v -> {
                if (onBack != null) {
                    onBack.run();
                }
            }), new LinearLayout.LayoutParams(px(ICON_BUTTON_SIZE_DP), px(ICON_BUTTON_SIZE_DP)));
        }

        TextView title = new TextView(activity);
        title.setText(titleText);
        title.setTextSize(sp(20));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(theme.primaryTextColor());
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1f);
        titleLp.leftMargin = showBack ? px(12) : 0;
        bar.addView(title, titleLp);

        if (showOverflow) {
            bar.addView(iconButton(R.drawable.ic_more_vert_24, "Menü", overflowClick),
                    new LinearLayout.LayoutParams(px(ICON_BUTTON_SIZE_DP), px(ICON_BUTTON_SIZE_DP)));
        }

        return bar;
    }

    private MaterialButton iconButton(int iconRes, String contentDescription, View.OnClickListener onClick) {
        MaterialButton button = new MaterialButton(activity);
        button.setText("");
        button.setAllCaps(false);
        button.setIcon(activity.getDrawable(iconRes));
        button.setIconTint(ColorStateList.valueOf(theme.primaryTextColor()));
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setIconPadding(0);
        button.setBackgroundTintList(ColorStateList.valueOf(theme.surfaceAltColor()));
        button.setRippleColor(ColorStateList.valueOf(theme.withAlpha(theme.primaryTextColor(), 0x18)));
        button.setCornerRadius(px(CORNER_RADIUS_DP));
        button.setMinWidth(px(ICON_BUTTON_SIZE_DP));
        button.setMinHeight(px(ICON_BUTTON_SIZE_DP));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setPadding(px(12), 0, px(12), 0);
        button.setContentDescription(contentDescription);
        button.setOnClickListener(onClick);
        return button;
    }

    public LinearLayout settingsCard() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(px(16), px(16), px(16), px(16));
        card.setBackground(makeRoundedCard(theme.surfaceColor(), theme.borderColor()));
        card.setElevation(px(2));
        return card;
    }

    public LinearLayout contentCard() {
        return settingsCard();
    }

    public LinearLayout screenBody(LinearLayout root, String title, Runnable onBack) {
        root.addView(appBar(title, false, null, false, null));
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(px(16), px(16), px(16), px(16));
        scrollView.addView(body);
        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
        if (onBack != null) {
            LinearLayout footer = new LinearLayout(activity);
            footer.setPadding(px(16), px(8), px(16), px(16));
            Button button = secondaryButton("Zurück");
            button.setOnClickListener(v -> onBack.run());
            footer.addView(button, new LinearLayout.LayoutParams(-1, -2));
            root.addView(footer);
        }
        return body;
    }

    public LinearLayout listRow(View leading, View content, View action) {
        return listRow(leading, content, action, null);
    }

    public LinearLayout listRow(View leading, View content, View primaryAction, View secondaryAction) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(rowHeight());
        if (leading != null) {
            row.addView(leading, new LinearLayout.LayoutParams(dragHandleWidth(), rowHeight()));
        }
        row.addView(content, new LinearLayout.LayoutParams(0, -2, 1));
        addListAction(row, primaryAction);
        addListAction(row, secondaryAction);
        return row;
    }

    private void addListAction(LinearLayout row, View action) {
        if (action != null) {
            row.addView(action, new LinearLayout.LayoutParams(listActionWidth(), rowHeight()));
        }
    }

    public void onTextChanged(EditText input, Runnable onChange) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChange.run(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    public TextInputLayout outlinedInput(String hint, EditText input) {
        TextInputLayout layout = new TextInputLayout(activity);
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxBackgroundColor(theme.surfaceColor());
        layout.setBoxStrokeColorStateList(inputBorderStateList());
        layout.setBoxStrokeWidth(strokeWidth());
        layout.setBoxStrokeWidthFocused(focusedStrokeWidth());
        layout.setHintTextColor(inputHintStateList());
        layout.setBoxCornerRadii(cornerRadius(), cornerRadius(), cornerRadius(), cornerRadius());
        input.setHint(null);
        input.setHintTextColor(inputHintStateList());
        input.setBackground(null);
        layout.addView(input, new LinearLayout.LayoutParams(-1, input.getMinLines() > 1 ? -2 : rowHeight()));
        return layout;
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

    public TextView listIcon(String text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(sp(24));
        view.setTextColor(theme.mutedTextColor());
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    public androidx.appcompat.app.AlertDialog showCardDialog(View content) {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(content)
                .show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    public void showActionMenu(String title, String[] labels, Runnable[] actions) {
        LinearLayout card = contentCard();
        card.setPadding(spaceXl(), spaceL(), spaceXl(), spaceL());
        if (title != null && !title.isEmpty()) {
            addDialogTitle(card, title);
        }
        final androidx.appcompat.app.AlertDialog[] dialog = new androidx.appcompat.app.AlertDialog[1];
        for (int i = 0; i < labels.length; i++) {
            Button button = labels[i].toLowerCase().contains("löschen") ? dangerButton(labels[i]) : secondaryButton(labels[i]);
            final int index = i;
            button.setOnClickListener(v -> {
                dialog[0].dismiss();
                actions[index].run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = i == labels.length - 1 ? 0 : spaceS();
            card.addView(button, lp);
        }
        dialog[0] = showCardDialog(card);
    }

    public void addDialogTitle(LinearLayout card, String text) {
        TextView title = tv(text, 20);
        title.setPadding(0, 0, 0, px(12));
        card.addView(title);
    }

    public void addDialogMessage(LinearLayout card, String text) {
        TextView message = new TextView(activity);
        message.setText(text);
        message.setTextSize(sp(14));
        message.setTextColor(theme.secondaryTextColor());
        message.setPadding(0, 0, 0, px(18));
        card.addView(message);
    }

    public void addSectionHeader(LinearLayout container, String titleText, String subtitleText) {
        TextView title = tv(titleText, 18);
        title.setPadding(0, 0, 0, spaceXs());
        container.addView(title);

        if (subtitleText != null && !subtitleText.isEmpty()) {
            TextView subtitle = new TextView(activity);
            subtitle.setText(subtitleText);
            subtitle.setTextSize(sp(14));
            subtitle.setTextColor(theme.secondaryTextColor());
            subtitle.setPadding(0, 0, 0, spaceM());
            container.addView(subtitle);
        }
    }

    public void confirmDelete(String title, String message, Runnable onDelete, Runnable onDismiss) {
        LinearLayout card = contentCard();
        card.setPadding(spaceXl(), px(SPACE_DIALOG_Y_DP), spaceXl(), spaceL());
        addDialogTitle(card, title);
        addDialogMessage(card, message);

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel = secondaryButton("Abbrechen");
        Button delete = dangerButton("Löschen");
        buttons.addView(cancel, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams deleteLp = new LinearLayout.LayoutParams(0, -2, 1);
        deleteLp.leftMargin = spaceS();
        buttons.addView(delete, deleteLp);
        card.addView(buttons);

        final androidx.appcompat.app.AlertDialog[] dialog = new androidx.appcompat.app.AlertDialog[1];
        dialog[0] = showCardDialog(card);
        dialog[0].setOnDismissListener(d -> {
            if (onDismiss != null) {
                onDismiss.run();
            }
        });
        cancel.setOnClickListener(v -> dialog[0].dismiss());
        delete.setOnClickListener(v -> {
            dialog[0].dismiss();
            onDelete.run();
        });
    }

    public GradientDrawable makeRoundedCard(int fillColor, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(px(CORNER_RADIUS_DP));
        drawable.setStroke(px(1), strokeColor);
        return drawable;
    }

    public GradientDrawable dragSelectedCard() {
        GradientDrawable drawable = makeRoundedCard(theme.accentSoftColor(), theme.accentColor());
        drawable.setStroke(px(2), theme.accentColor());
        return drawable;
    }

    public Drawable squareRipple(int fillColor, int rippleColor) {
        return new RippleDrawable(
                ColorStateList.valueOf(rippleColor),
                new ColorDrawable(fillColor),
                new ColorDrawable(Color.WHITE));
    }

    public int spaceXs() {
        return px(SPACE_XS_DP);
    }

    public int spaceS() {
        return px(SPACE_S_DP);
    }

    public int spaceSm() {
        return px(SPACE_SM_DP);
    }

    public int spaceM() {
        return px(SPACE_M_DP);
    }

    public int spaceMl() {
        return px(SPACE_ML_DP);
    }

    public int spaceL() {
        return px(SPACE_L_DP);
    }

    public int spaceXl() {
        return px(SPACE_XL_DP);
    }

    public int checkRowHeight() {
        return px(CHECK_ROW_HEIGHT_DP);
    }

    public int buttonHeight() {
        return px(BUTTON_HEIGHT_DP);
    }

    public int cornerRadius() {
        return px(CORNER_RADIUS_DP);
    }

    public int strokeWidth() {
        return px(1);
    }

    public int focusedStrokeWidth() {
        return px(2);
    }

    public int rowHeight() {
        return px(ROW_HEIGHT_DP);
    }

    private int dragHandleWidth() {
        return px(DRAG_HANDLE_WIDTH_DP);
    }

    private int listActionWidth() {
        return px(LIST_ACTION_WIDTH_DP);
    }

    public int bottomSafePadding() {
        return px(BOTTOM_SAFE_PADDING_DP);
    }

    public int px(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    public float sp(float value) {
        return value * theme.fontScale();
    }
}

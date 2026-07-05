package com.example.trackingapp.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.trackingapp.theme.ThemeStore;
import com.google.android.material.button.MaterialButton;

public final class AppUi {
    private static final int CARD_RADIUS_DP = 4;
    private static final int CONTROL_RADIUS_DP = 6;
    private static final int CHIP_RADIUS_DP = 4;

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
        button.setCornerRadius(px(CONTROL_RADIUS_DP));
        button.setPadding(px(16), px(12), px(16), px(12));
        button.setBackgroundTintList(ColorStateList.valueOf(fillColor));
        button.setStrokeWidth(px(1));
        button.setStrokeColor(ColorStateList.valueOf(strokeColor));
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

    public Button tabButton(String text, boolean selected) {
        return button(
                text,
                selected ? theme.accentSoftColor() : theme.surfaceColor(),
                selected ? theme.accentColor() : theme.mutedTextColor(),
                selected ? theme.accentColor() : theme.borderColor());
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
        Button fab = new Button(activity);
        fab.setText("+");
        fab.setAllCaps(false);
        fab.setTextSize(sp(28));
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setTextColor(0xffffffff);
        fab.setBackground(makeRoundedCard(theme.accentColor(), theme.accentColor()));
        fab.setElevation(px(10));
        fab.setMinWidth(px(56));
        fab.setMinHeight(px(56));
        fab.setPadding(0, 0, 0, px(4));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(56), px(56), Gravity.END | Gravity.BOTTOM);
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
            Button back = button("←", theme.surfaceAltColor(), theme.primaryTextColor(), theme.borderColor());
            back.setTextSize(sp(20));
            back.setMinWidth(px(44));
            back.setMinHeight(px(44));
            back.setPadding(px(10), 0, px(10), 0);
            back.setOnClickListener(v -> {
                if (onBack != null) {
                    onBack.run();
                }
            });
            bar.addView(back);
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
            Button overflow = button("⋮", theme.surfaceAltColor(), theme.primaryTextColor(), theme.borderColor());
            overflow.setTextSize(sp(24));
            overflow.setMinWidth(px(44));
            overflow.setMinHeight(px(44));
            overflow.setPadding(px(10), 0, px(10), 0);
            overflow.setOnClickListener(overflowClick);
            bar.addView(overflow);
        }

        return bar;
    }

    public TextView settingsCardTitle(String text) {
        TextView title = tv(text, 12);
        title.setTextColor(theme.mutedTextColor());
        title.setPadding(0, 0, 0, px(6));
        return title;
    }

    public LinearLayout settingsCard() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(px(16), px(16), px(16), px(16));
        card.setBackground(makeRoundedCard(theme.surfaceColor(), theme.borderColor()));
        card.setElevation(px(2));
        return card;
    }

    public TextView chip(String text, int backgroundColor, int textColor) {
        TextView chip = new TextView(activity);
        chip.setText(text);
        chip.setTextSize(sp(12));
        chip.setTextColor(textColor);
        chip.setPadding(px(10), px(6), px(10), px(6));

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(backgroundColor);
        drawable.setCornerRadius(px(CHIP_RADIUS_DP));
        chip.setBackground(drawable);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = px(8);
        chip.setLayoutParams(lp);
        return chip;
    }

    public GradientDrawable makeRoundedCard(int fillColor, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(px(CARD_RADIUS_DP));
        drawable.setStroke(px(1), strokeColor);
        return drawable;
    }

    public Drawable squareRipple(int fillColor, int rippleColor) {
        return new RippleDrawable(
                ColorStateList.valueOf(rippleColor),
                new ColorDrawable(fillColor),
                new ColorDrawable(Color.WHITE));
    }

    public int px(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    public float sp(float value) {
        return value * theme.fontScale();
    }
}

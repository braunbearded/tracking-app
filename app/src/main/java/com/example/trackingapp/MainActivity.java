package com.example.trackingapp;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.PopupMenu;
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
import com.example.trackingapp.theme.ThemeStore;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends Activity {
    private TrackingDatabase db;
    private ThemeStore theme;
    private LinearLayout root;
    private int currentTab = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Long> timers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = new ThemeStore(this);
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
        view.setTextColor(primaryTextColor());
        return view;
    }

    private Button btn(String text) {
        return secondaryButton(text);
    }

    private Button button(String text, int fillColor, int textColor, int strokeColor) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(14);
        button.setCornerRadius(px(24));
        button.setPadding(px(16), px(12), px(16), px(12));
        button.setBackgroundTintList(ColorStateList.valueOf(fillColor));
        button.setStrokeWidth(px(1));
        button.setStrokeColor(ColorStateList.valueOf(strokeColor));
        button.setRippleColor(ColorStateList.valueOf(withAlpha(textColor, 0x22)));
        return button;
    }

    private Button primaryButton(String text) {
        return button(text, accentColor(), Color.WHITE, accentColor());
    }

    private Button secondaryButton(String text) {
        return button(text, surfaceColor(), primaryTextColor(), borderColor());
    }

    private Button ghostButton(String text) {
        return button(text, surfaceAltColor(), primaryTextColor(), borderColor());
    }

    private Button dangerButton(String text) {
        return button(text, theme.cautionFillColor(), 0xffb42318, theme.cautionStrokeColor());
    }

    private Button tabButton(String text, boolean selected) {
        return button(
                text,
                selected ? accentSoftColor() : surfaceColor(),
                selected ? accentColor() : mutedTextColor(),
                selected ? accentColor() : borderColor());
    }

    private boolean darkMode() {
        return theme.darkMode();
    }

    private void setDarkMode(boolean enabled) {
        theme.setDarkMode(enabled);
    }

    private int accentIndex() {
        return theme.accentIndex();
    }

    private void setAccentIndex(int index) {
        theme.setAccentIndex(index);
    }

    private int accentColor() {
        return theme.accentColor();
    }

    private int accentSoftColor() {
        return theme.accentSoftColor();
    }

    private int bgColor() {
        return theme.backgroundColor();
    }

    private int surfaceColor() {
        return theme.surfaceColor();
    }

    private int surfaceAltColor() {
        return theme.surfaceAltColor();
    }

    private int primaryTextColor() {
        return theme.primaryTextColor();
    }

    private int secondaryTextColor() {
        return theme.secondaryTextColor();
    }

    private int mutedTextColor() {
        return theme.mutedTextColor();
    }

    private int borderColor() {
        return theme.borderColor();
    }

    private int withAlpha(int color, int alpha) {
        return theme.withAlpha(color, alpha);
    }

    private void refreshHome() {
        showHome(currentTab);
    }

    private void refreshSettings() {
        showSettingsScreen();
    }

    private View bottomNav(int selectedTab) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                px(76)));
        nav.setBackgroundColor(theme.navigationBarColor());
        nav.setElevation(px(8));
        nav.addView(navItem("Sessions", android.R.drawable.ic_menu_agenda, selectedTab == 0, v -> showHome(0)),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        nav.addView(navItem("Tracker", android.R.drawable.ic_menu_sort_by_size, selectedTab == 1, v -> showHome(1)),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        return nav;
    }

    private View navItem(String label, int iconRes, boolean selected, View.OnClickListener onClick) {
        FrameLayout item = new FrameLayout(this);
        item.setClickable(true);
        item.setFocusable(true);
        item.setBackground(squareRipple(theme.navigationBarColor(), withAlpha(accentColor(), 0x18)));
        item.setOnClickListener(onClick);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        content.setPadding(0, px(10), 0, px(10));

        Drawable icon = getDrawable(iconRes);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(selected ? accentColor() : mutedTextColor());
        }

        ImageView iconView = new ImageView(this);
        iconView.setImageDrawable(icon);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(px(22), px(22));
        iconView.setLayoutParams(iconLp);
        content.addView(iconView);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        labelView.setIncludeFontPadding(false);
        labelView.setSingleLine();
        labelView.setTextColor(selected ? accentColor() : mutedTextColor());
        labelView.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        labelLp.topMargin = px(4);
        labelView.setLayoutParams(labelLp);
        content.addView(labelView);

        item.addView(content);
        return item;
    }

    private Button floatingActionButton(int tab) {
        Button fab = new Button(this);
        fab.setText("+");
        fab.setAllCaps(false);
        fab.setTextSize(28);
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setTextColor(0xffffffff);
        fab.setBackground(makeRoundedCard(accentColor(), accentColor()));
        fab.setElevation(px(10));
        fab.setMinWidth(px(56));
        fab.setMinHeight(px(56));
        fab.setPadding(0, 0, 0, px(4));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(56), px(56), Gravity.END | Gravity.BOTTOM);
        lp.rightMargin = px(20);
        lp.bottomMargin = px(20);
        fab.setLayoutParams(lp);
        fab.setOnClickListener(v -> {
            if (tab == 0) {
                chooseTracker();
            } else {
                createTracker();
            }
        });
        return fab;
    }

    private void base() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bgColor());
        setContentView(root);
    }

    private void showHome(int tab) {
        currentTab = tab;
        base();

        root.addView(topAppBar());

        FrameLayout content = new FrameLayout(this);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(-1, 0, 1);
        root.addView(content, contentLp);

        FrameLayout body = new FrameLayout(this);
        content.addView(body, new FrameLayout.LayoutParams(-1, -1));

        if (tab == 0) {
            sessions(body);
        } else {
            trackers(body);
        }

        content.addView(floatingActionButton(tab));

        root.addView(bottomNav(tab));
    }

    private View topAppBar() {
        return appBar("Tracking App", false, null, true);
    }

    private View appBar(String titleText, boolean showBack, Runnable onBack, boolean showOverflow) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setMinimumHeight(px(64));
        bar.setPadding(px(16), px(18), px(12), px(14));
        bar.setBackgroundColor(accentColor());
        bar.setElevation(px(4));

        if (showBack) {
            Button back = button("←", withAlpha(0xffffffff, 0x22), Color.WHITE, withAlpha(0xffffffff, 0x22));
            back.setTextSize(20);
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

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xffffffff);

        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1f);
        titleLp.leftMargin = showBack ? px(12) : 0;
        bar.addView(title, titleLp);

        if (showOverflow) {
            Button overflow = overflowButton();
            overflow.setOnClickListener(v -> showOverflowMenu(v));
            bar.addView(overflow);
        }

        return bar;
    }

    private Button overflowButton() {
        Button button = new Button(this);
        button.setText("⋮");
        button.setAllCaps(false);
        button.setTextSize(24);
        button.setTextColor(0xffffffff);
        button.setMinWidth(px(44));
        button.setMinHeight(px(44));
        button.setPadding(px(10), 0, px(10), 0);
        button.setBackground(makeRoundedCard(withAlpha(accentColor(), 0x33), withAlpha(0xffffffff, 0x66)));
        return button;
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor, Gravity.END);
        menu.getMenu().add(0, 1, 0, "Einstellungen");
        menu.getMenu().add(0, 2, 1, "Über die App");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showSettingsScreen();
                return true;
            }
            if (item.getItemId() == 2) {
                showAboutDialog();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showSettingsScreen() {
        base();

        root.addView(appBar("Einstellungen", true, this::refreshHome, false));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(16), px(16), px(16), px(104));
        scrollView.addView(box);

        TextView intro = tv("Passe das Erscheinungsbild der App an.", 16);
        intro.setTextColor(secondaryTextColor());
        intro.setPadding(0, 0, 0, px(16));
        box.addView(intro);

        box.addView(settingsCardTitle("Darstellung"));
        box.addView(themeCard());

        box.addView(settingsCardTitle("Akzentfarbe"));
        box.addView(accentCard());

        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void showAboutDialog() {
        String versionName = "unknown";
        long versionCode = 0;
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = info.versionName == null ? "unknown" : info.versionName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = info.getLongVersionCode();
            } else {
                versionCode = info.versionCode;
            }
        } catch (Exception ignored) {
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(px(4), px(4), px(4), px(0));
        scrollView.addView(body);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(px(16), px(16), px(16), px(16));
        header.setBackground(makeRoundedCard(accentColor(), accentColor()));
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(-1, -2);
        headerLp.bottomMargin = px(12);
        body.addView(header, headerLp);

        TextView intro = new TextView(this);
        intro.setText("Über Tracking App");
        intro.setTextSize(22);
        intro.setTypeface(Typeface.DEFAULT_BOLD);
        intro.setTextColor(Color.WHITE);
        intro.setPadding(0, 0, 0, px(4));
        header.addView(intro);

        TextView subtitle = new TextView(this);
        subtitle.setText("Lokale Android-App auf SQLite-Basis. Keine Google Play Services, kein Firebase.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(withAlpha(Color.WHITE, 0xcc));
        header.addView(subtitle);

        body.addView(aboutInfoCard("Repository", "braunbearded/tracking-app", true));
        body.addView(aboutInfoCard("Version", versionName, false));
        body.addView(aboutInfoCard("Build", String.valueOf(versionCode), false));

        new MaterialAlertDialogBuilder(this)
                .setView(scrollView)
                .setPositiveButton("OK", null)
                .show();
    }

    private View aboutInfoCard(String label, String value, boolean clickable) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(px(16), px(12), px(16), px(12));
        row.setBackground(makeRoundedCard(surfaceColor(), borderColor()));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(mutedTextColor());
        labelView.setPadding(0, 0, 0, px(4));
        row.addView(labelView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(15);
        valueView.setTypeface(Typeface.DEFAULT_BOLD);
        valueView.setTextColor(primaryTextColor());
        row.addView(valueView);

        if (clickable && "Repository".equals(label)) {
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> openUrl("https://github.com/braunbearded/tracking-app"));
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = px(10);
        row.setLayoutParams(lp);
        if (clickable) {
            row.setBackground(makeRoundedCard(surfaceAltColor(), borderColor()));
        }
        return row;
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Link konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView settingsCardTitle(String text) {
        TextView title = tv(text, 12);
        title.setTextColor(mutedTextColor());
        title.setPadding(0, 0, 0, px(6));
        return title;
    }

    private View settingsCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(px(16), px(16), px(16), px(16));
        card.setBackground(makeRoundedCard(surfaceColor(), borderColor()));
        card.setElevation(px(2));
        return card;
    }

    private View themeCard() {
        LinearLayout card = (LinearLayout) settingsCard();

        TextView title = tv("Farbschema", 18);
        title.setPadding(0, 0, 0, px(4));
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Wähle System, Hell oder Dunkel für die gesamte App.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(secondaryTextColor());
        subtitle.setPadding(0, 0, 0, px(10));
        card.addView(subtitle);

        ChipGroup group = new ChipGroup(this);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        group.setChipSpacingHorizontal(px(8));
        group.setChipSpacingVertical(px(8));

        group.addView(themeModeChip("System", ThemeStore.THEME_SYSTEM));
        group.addView(themeModeChip("Hell", ThemeStore.THEME_LIGHT));
        group.addView(themeModeChip("Dunkel", ThemeStore.THEME_DARK));
        group.check(themeModeChipId(theme.themeMode()));
        group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            if (checkedId == themeModeChipId(ThemeStore.THEME_SYSTEM)) {
                theme.setThemeMode(ThemeStore.THEME_SYSTEM);
            } else if (checkedId == themeModeChipId(ThemeStore.THEME_LIGHT)) {
                theme.setThemeMode(ThemeStore.THEME_LIGHT);
            } else if (checkedId == themeModeChipId(ThemeStore.THEME_DARK)) {
                theme.setThemeMode(ThemeStore.THEME_DARK);
            }
            refreshSettings();
        });
        card.addView(group);

        return card;
    }

    private Chip themeModeChip(String label, int mode) {
        Chip chip = new Chip(this);
        chip.setId(themeModeChipId(mode));
        chip.setText(label);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setTextColor(primaryTextColor());
        chip.setChipBackgroundColor(ColorStateList.valueOf(surfaceAltColor()));
        chip.setChipStrokeColor(ColorStateList.valueOf(borderColor()));
        chip.setChipStrokeWidth(px(1));
        chip.setCheckedIconVisible(false);
        chip.setCheckedIcon(null);
        chip.setElevation(px(0));
        if (theme.themeMode() == mode) {
            chip.setChipBackgroundColor(ColorStateList.valueOf(accentSoftColor()));
            chip.setChipStrokeColor(ColorStateList.valueOf(accentColor()));
            chip.setTextColor(accentColor());
        }
        return chip;
    }

    private int themeModeChipId(int mode) {
        return 9000 + mode;
    }

    private View accentCard() {
        LinearLayout card = (LinearLayout) settingsCard();

        TextView subtitle = new TextView(this);
        subtitle.setText("Wähle eine Akzentfarbe für Header, Auswahl und Primäraktionen.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(secondaryTextColor());
        subtitle.setPadding(0, 0, 0, px(12));
        card.addView(subtitle);

        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(4);
            row.setPadding(0, 0, 0, px(10));

            for (int col = 0; col < 4; col++) {
                int index = rowIndex * 4 + col;
                if (index >= theme.accentCount()) {
                    View spacer = new View(this);
                    row.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));
                    continue;
                }
                row.addView(accentOption(index), new LinearLayout.LayoutParams(0, -2, 1f));
            }
            card.addView(row);
        }

        return card;
    }

    private Button accentOption(int index) {
        boolean selected = accentIndex() == index;
        int accent = theme.accentColor(index);
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(theme.accentName(index));
        button.setTextSize(13);
        button.setTextColor(selected ? Color.WHITE : accent);
        button.setPadding(px(8), px(18), px(8), px(18));
        int fillColor = selected ? accent : theme.accentSoftColor(index);
        int strokeColor = accent;
        button.setBackground(makeRoundedCard(fillColor, strokeColor));
        button.setElevation(selected ? px(4) : 0);
        button.setOnClickListener(v -> {
            setAccentIndex(index);
            refreshSettings();
        });
        return button;
    }

    private void sessions(FrameLayout body) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(16), px(12), px(16), px(104));
        scrollView.addView(box);

        TextView title = tv("Deine Läufe", 28);
        title.setPadding(0, 0, 0, px(4));
        box.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Alle Sessions über alle Tracker hinweg. Tippe eine Session an, um sie zu öffnen.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(secondaryTextColor());
        subtitle.setPadding(0, 0, 0, px(16));
        box.addView(subtitle);

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
            card.setBackground(makeRoundedCard(surfaceColor(), borderColor()));
            card.setElevation(px(2));

            View statusStripe = new View(this);
            statusStripe.setBackgroundColor(open ? accentColor() : mutedTextColor());
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
            leftMeta.setTextColor(mutedTextColor());
            metaRow.addView(leftMeta, new LinearLayout.LayoutParams(0, -2, 1));

            TextView rightMeta = new TextView(this);
            rightMeta.setText(recordCount + "/" + tracker.items.size() + " Items");
            rightMeta.setTextSize(13);
            rightMeta.setTextColor(mutedTextColor());
            rightMeta.setGravity(Gravity.END);
            metaRow.addView(rightMeta, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(metaRow);

            LinearLayout chipRow = new LinearLayout(this);
            chipRow.setOrientation(LinearLayout.HORIZONTAL);
            chipRow.setPadding(0, 0, 0, px(12));

            chipRow.addView(chip(session.status.equals("open") ? "Offen" : "Abgeschlossen", open ? accentSoftColor() : surfaceAltColor(), open ? accentColor() : primaryTextColor()));
            chipRow.addView(chip(recordCount + "/" + tracker.items.size(), accentSoftColor(), accentColor()));
            card.addView(chipRow);

            TextView preview = new TextView(this);
            preview.setText(preview(session.id));
            preview.setTextSize(14);
            preview.setTextColor(primaryTextColor());
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
            empty.setBackground(makeRoundedCard(surfaceAltColor(), borderColor()));

            TextView emptyTitle = tv("Noch keine Sessions vorhanden", 18);
            emptyTitle.setPadding(0, 0, 0, px(4));
            empty.addView(emptyTitle);

            TextView emptyBody = new TextView(this);
            emptyBody.setText("Starte eine neue Session, um erste Werte zu erfassen und die Historie aufzubauen.");
            emptyBody.setTextSize(14);
            emptyBody.setTextColor(secondaryTextColor());
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
        drawable.setCornerRadius(px(28));
        drawable.setStroke(px(1), strokeColor);
        return drawable;
    }

    private Drawable squareRipple(int fillColor, int rippleColor) {
        return new RippleDrawable(
                ColorStateList.valueOf(rippleColor),
                new ColorDrawable(fillColor),
                new ColorDrawable(Color.WHITE));
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
        card.setBackgroundColor(surfaceAltColor());

        TextView title = tv(readOnly ? "Abgeschlossener Stand" : "Aktueller Session-Stand", 16);
        title.setPadding(0, 0, 0, 6);
        card.addView(title);

        TextView body = new TextView(this);
        body.setText(summaryText(values, item));
        body.setTextSize(14);
        body.setTextColor(primaryTextColor());
        card.addView(body);

        TextView hint = new TextView(this);
        hint.setText(readOnly
                ? "Read-only. Werte sind unveränderlich."
                : "Änderungen werden beim Zurück- oder Weitergehen gespeichert.");
        hint.setTextSize(12);
        hint.setTextColor(mutedTextColor());
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
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(px(16), px(12), px(16), px(20));
        sheet.setBackgroundColor(surfaceColor());

        View handle = new View(this);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(px(42), px(5));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin = px(14);
        handle.setLayoutParams(handleLp);
        handle.setBackground(makeRoundedCard(surfaceAltColor(), borderColor()));
        sheet.addView(handle);

        TextView title = tv("Tracker auswählen", 22);
        title.setPadding(0, 0, 0, px(4));
        sheet.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(trackers.isEmpty()
                ? "Lege zuerst einen Tracker an."
                : "Wähle einen Tracker für die neue Session.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(secondaryTextColor());
        subtitle.setPadding(0, 0, 0, px(16));
        sheet.addView(subtitle);

        if (trackers.isEmpty()) {
            Button create = primaryButton("Neuen Tracker anlegen");
            create.setOnClickListener(v -> {
                dialog.dismiss();
                createTracker();
            });
            sheet.addView(create, new LinearLayout.LayoutParams(-1, -2));
        } else {
            for (Tracker tracker : trackers) {
                View item = selectionRow(
                        tracker.name,
                        tracker.description == null || tracker.description.trim().isEmpty()
                                ? "Ohne Beschreibung"
                                : tracker.description);
                item.setOnClickListener(v -> {
                    dialog.dismiss();
                    openSession(db.createSession(tracker.id));
                });
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                rowLp.bottomMargin = px(10);
                sheet.addView(item, rowLp);
            }
        }

        Button cancel = secondaryButton("Abbrechen");
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(-1, -2);
        cancelLp.topMargin = px(4);
        sheet.addView(cancel, cancelLp);

        dialog.setContentView(sheet);
        dialog.show();
    }

    private View selectionRow(String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(px(16), px(14), px(16), px(14));
        row.setBackground(makeRoundedCard(surfaceAltColor(), borderColor()));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);

        TextView rowTitle = new TextView(this);
        rowTitle.setText(title);
        rowTitle.setTextSize(16);
        rowTitle.setTypeface(Typeface.DEFAULT_BOLD);
        rowTitle.setTextColor(primaryTextColor());
        text.addView(rowTitle);

        TextView rowSubtitle = new TextView(this);
        rowSubtitle.setText(subtitle);
        rowSubtitle.setTextSize(13);
        rowSubtitle.setTextColor(mutedTextColor());
        rowSubtitle.setPadding(0, px(2), 0, 0);
        text.addView(rowSubtitle);

        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(text, textLp);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(22);
        arrow.setTextColor(mutedTextColor());
        arrow.setPadding(px(10), 0, 0, 0);
        row.addView(arrow);

        return row;
    }

    private void trackers(FrameLayout body) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(16), px(16), px(16), px(104));
        scrollView.addView(box);

        TextView eyebrow = tv("TRACKER", 12);
        eyebrow.setTextColor(mutedTextColor());
        eyebrow.setPadding(0, 0, 0, px(4));
        box.addView(eyebrow);

        TextView title = tv("Deine Tracker", 28);
        title.setPadding(0, 0, 0, px(4));
        box.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Verwalte Vorlagen, Items und Felder für deine Sessions.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(secondaryTextColor());
        subtitle.setPadding(0, 0, 0, px(16));
        box.addView(subtitle);

        List<Tracker> trackers = db.trackers();
        if (trackers.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setPadding(px(16), px(16), px(16), px(16));
            empty.setBackground(makeRoundedCard(surfaceAltColor(), borderColor()));

            TextView emptyTitle = tv("Noch keine Tracker vorhanden", 18);
            emptyTitle.setPadding(0, 0, 0, px(4));
            empty.addView(emptyTitle);

            TextView emptyBody = new TextView(this);
            emptyBody.setText("Lege einen Tracker an, um Items und Fields für deine Sessions zu definieren.");
            emptyBody.setTextSize(14);
            emptyBody.setTextColor(secondaryTextColor());
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
                card.setBackground(makeRoundedCard(surfaceColor(), borderColor()));
                card.setElevation(px(2));

                View stripe = new View(this);
                stripe.setBackgroundColor(accentColor());
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
                meta.setTextColor(mutedTextColor());
                metaRow.addView(meta);
                card.addView(metaRow);

                LinearLayout chipRow = new LinearLayout(this);
                chipRow.setOrientation(LinearLayout.HORIZONTAL);
                chipRow.setPadding(0, 0, 0, px(12));
                chipRow.addView(chip(itemCount == 0 ? "Leer" : itemCount + " Items", itemCount == 0 ? withAlpha(accentColor(), 0x18) : accentSoftColor(), itemCount == 0 ? primaryTextColor() : accentColor()));
                chipRow.addView(chip(fieldCount + " Fields", accentSoftColor(), accentColor()));
                card.addView(chipRow);

                TextView preview = new TextView(this);
                preview.setText(tracker.description == null || tracker.description.trim().isEmpty()
                        ? "Keine Beschreibung vorhanden."
                        : tracker.description);
                preview.setTextSize(14);
                preview.setTextColor(primaryTextColor());
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
        header.setBackgroundColor(bgColor());

        TextView eyebrow = tv(isNew ? "NEUER TRACKER" : "TRACKER BEARBEITEN", 12);
        eyebrow.setTextColor(mutedTextColor());
        eyebrow.setPadding(0, 0, 0, px(4));
        header.addView(eyebrow);

        TextView title = tv(isNew ? "Tracker anlegen" : "Tracker bearbeiten", 24);
        title.setPadding(0, 0, 0, px(4));
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Name, Beschreibung, Items und Fields direkt im Formular pflegen.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(secondaryTextColor());
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
            empty.setBackground(makeRoundedCard(surfaceAltColor(), borderColor()));

            TextView emptyTitle = tv("Noch keine Items angelegt", 18);
            emptyTitle.setPadding(0, 0, 0, px(4));
            empty.addView(emptyTitle);

            TextView emptyBody = new TextView(this);
            emptyBody.setText("Tippe auf \"Item hinzufügen\" und lege dann darunter die Fields an.");
            emptyBody.setTextSize(14);
            emptyBody.setTextColor(secondaryTextColor());
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
        card.setBackground(makeRoundedCard(surfaceAltColor(), borderColor()));

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
        row.setBackground(makeRoundedCard(surfaceColor(), borderColor()));
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
        required.setTextColor(primaryTextColor());
        views.requiredCheck = required;
        optionsRow.addView(required);
        row.addView(optionsRow);

        CheckBox prefill = new CheckBox(this);
        prefill.setText("Prefill from previous");
        prefill.setChecked(field != null && field.prefillFromPrevious);
        prefill.setTextColor(primaryTextColor());
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
        title.setTextColor(mutedTextColor());
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
        input.setBackground(makeRoundedCard(surfaceColor(), borderColor()));
        input.setTextColor(primaryTextColor());
        input.setHintTextColor(mutedTextColor());
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
        card.setBackgroundColor(surfaceAltColor());
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
            editText.setTextColor(primaryTextColor());
            editText.setHintTextColor(mutedTextColor());
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
        editText.setTextColor(primaryTextColor());
        editText.setHintTextColor(mutedTextColor());
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
        button.setTextColor(primaryTextColor());
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
        handle.setBackground(makeRoundedCard(surfaceAltColor(), borderColor()));
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
            bar.setBackgroundColor(mutedTextColor());
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

package com.example.trackingapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.trackingapp.theme.ThemeStore;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.ShapeAppearanceModel;

public final class SettingsUi {
    private final Activity activity;
    private final ThemeStore theme;
    private final AppUi ui;
    private final Runnable refreshSettings;
    private final Runnable backHome;

    public SettingsUi(Activity activity, ThemeStore theme, AppUi ui, Runnable refreshSettings, Runnable backHome) {
        this.activity = activity;
        this.theme = theme;
        this.ui = ui;
        this.refreshSettings = refreshSettings;
        this.backHome = backHome;
    }

    public void render(LinearLayout root) {
        root.addView(ui.appBar("Einstellungen", true, backHome, false, null));

        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(ui.px(16), ui.px(16), ui.px(16), ui.px(104));
        scrollView.addView(box);

        TextView intro = ui.tv("Passe das Erscheinungsbild der App an.", 16);
        intro.setTextColor(theme.secondaryTextColor());
        intro.setPadding(0, 0, 0, ui.px(16));
        box.addView(intro);

        box.addView(ui.settingsCardTitle("Darstellung"));
        box.addView(themeCard());

        box.addView(ui.settingsCardTitle("Schrift"));
        box.addView(fontCard());

        box.addView(ui.settingsCardTitle("Akzentfarbe"));
        box.addView(accentCard());

        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    public void showAboutDialog() {
        String versionName = "unknown";
        long versionCode = 0;
        try {
            android.content.pm.PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            versionName = info.versionName == null ? "unknown" : info.versionName;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                versionCode = info.getLongVersionCode();
            } else {
                versionCode = info.versionCode;
            }
        } catch (Exception ignored) {
        }

        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);

        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(ui.px(4), ui.px(4), ui.px(4), ui.px(0));
        scrollView.addView(body);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(ui.px(16), ui.px(16), ui.px(16), ui.px(16));
        header.setBackground(ui.makeRoundedCard(theme.surfaceColor(), theme.borderColor()));
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(-1, -2);
        headerLp.bottomMargin = ui.px(12);
        body.addView(header, headerLp);

        View accentBar = new View(activity);
        accentBar.setBackgroundColor(theme.accentColor());
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(-1, ui.px(4));
        accentLp.bottomMargin = ui.px(12);
        header.addView(accentBar, accentLp);

        TextView intro = new TextView(activity);
        intro.setText("Über Tracking App");
        intro.setTextSize(ui.sp(22));
        intro.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        intro.setTextColor(theme.primaryTextColor());
        intro.setPadding(0, 0, 0, ui.px(4));
        header.addView(intro);

        TextView subtitle = new TextView(activity);
        subtitle.setText("Lokale Android-App auf SQLite-Basis. Keine Google Play Services, kein Firebase.");
        subtitle.setTextSize(ui.sp(14));
        subtitle.setTextColor(theme.secondaryTextColor());
        header.addView(subtitle);

        body.addView(aboutInfoCard("Repository", "braunbearded/tracking-app", true));
        body.addView(aboutInfoCard("Version", versionName, false));
        body.addView(aboutInfoCard("Build", String.valueOf(versionCode), false));

        new MaterialAlertDialogBuilder(activity)
                .setView(scrollView)
                .setPositiveButton("OK", null)
                .show();
    }

    private View aboutInfoCard(String label, String value, boolean clickable) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(ui.px(16), ui.px(12), ui.px(16), ui.px(12));
        row.setBackground(ui.makeRoundedCard(theme.surfaceColor(), theme.borderColor()));

        TextView labelView = new TextView(activity);
        labelView.setText(label);
        labelView.setTextSize(ui.sp(12));
        labelView.setTextColor(theme.mutedTextColor());
        labelView.setPadding(0, 0, 0, ui.px(4));
        row.addView(labelView);

        TextView valueView = new TextView(activity);
        valueView.setText(value);
        valueView.setTextSize(ui.sp(15));
        valueView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        valueView.setTextColor(theme.primaryTextColor());
        row.addView(valueView);

        if (clickable && "Repository".equals(label)) {
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> openUrl("https://github.com/braunbearded/tracking-app"));
            row.setBackground(ui.makeRoundedCard(theme.surfaceAltColor(), theme.borderColor()));
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = ui.px(10);
        row.setLayoutParams(lp);
        return row;
    }

    private View themeCard() {
        LinearLayout card = ui.settingsCard();

        TextView title = ui.tv("Farbschema", 18);
        title.setPadding(0, 0, 0, ui.px(4));
        card.addView(title);

        TextView subtitle = new TextView(activity);
        subtitle.setText("Wähle System, Hell oder Dunkel für die gesamte App.");
        subtitle.setTextSize(ui.sp(14));
        subtitle.setTextColor(theme.secondaryTextColor());
        subtitle.setPadding(0, 0, 0, ui.px(10));
        card.addView(subtitle);

        ChipGroup group = new ChipGroup(activity);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        group.setChipSpacingHorizontal(ui.px(8));
        group.setChipSpacingVertical(ui.px(8));

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
            refreshSettings.run();
        });
        card.addView(group);
        return card;
    }

    private View fontCard() {
        LinearLayout card = ui.settingsCard();

        TextView title = ui.tv("Schriftgröße", 18);
        title.setPadding(0, 0, 0, ui.px(4));
        card.addView(title);

        TextView subtitle = new TextView(activity);
        subtitle.setText("Skaliert die wichtigsten Texte und Überschriften in der gesamten App.");
        subtitle.setTextSize(ui.sp(14));
        subtitle.setTextColor(theme.secondaryTextColor());
        subtitle.setPadding(0, 0, 0, ui.px(10));
        card.addView(subtitle);

        ChipGroup group = new ChipGroup(activity);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        group.setChipSpacingHorizontal(ui.px(8));
        group.setChipSpacingVertical(ui.px(8));

        for (int i = 0; i < theme.fontScaleCount(); i++) {
            group.addView(fontScaleChip(theme.fontScaleName(i), i));
        }
        group.check(fontScaleChipId(theme.fontScaleIndex()));
        group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            for (int i = 0; i < theme.fontScaleCount(); i++) {
                if (checkedId == fontScaleChipId(i)) {
                    theme.setFontScaleIndex(i);
                    refreshSettings.run();
                    break;
                }
            }
        });
        card.addView(group);
        return card;
    }

    private Chip themeModeChip(String label, int mode) {
        Chip chip = new Chip(activity);
        chip.setId(themeModeChipId(mode));
        chip.setText(label);
        styleChoiceChip(chip, theme.themeMode() == mode);
        return chip;
    }

    private Chip fontScaleChip(String label, int index) {
        Chip chip = new Chip(activity);
        chip.setId(fontScaleChipId(index));
        chip.setText(label);
        styleChoiceChip(chip, theme.fontScaleIndex() == index);
        return chip;
    }

    private void styleChoiceChip(Chip chip, boolean selected) {
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setTextColor(selected ? theme.accentColor() : theme.primaryTextColor());
        chip.setChipBackgroundColor(ColorStateList.valueOf(selected ? theme.accentSoftColor() : theme.surfaceAltColor()));
        chip.setChipStrokeColor(ColorStateList.valueOf(selected ? theme.accentColor() : theme.borderColor()));
        chip.setChipStrokeWidth(ui.px(1));
        chip.setShapeAppearanceModel(ShapeAppearanceModel.builder()
                .setAllCornerSizes(ui.px(8))
                .build());
        chip.setCheckedIconVisible(false);
        chip.setCheckedIcon(null);
        chip.setElevation(ui.px(0));
    }

    private View accentCard() {
        LinearLayout card = ui.settingsCard();

        TextView subtitle = new TextView(activity);
        subtitle.setText("Wähle eine Akzentfarbe für Header, Auswahl und Primäraktionen.");
        subtitle.setTextSize(ui.sp(14));
        subtitle.setTextColor(theme.secondaryTextColor());
        subtitle.setPadding(0, 0, 0, ui.px(12));
        card.addView(subtitle);

        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(4);
            row.setPadding(0, 0, 0, ui.px(10));

            for (int col = 0; col < 4; col++) {
                int index = rowIndex * 4 + col;
                if (index >= theme.accentCount()) {
                    View spacer = new View(activity);
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
        boolean selected = theme.accentIndex() == index;
        int accent = theme.accentColor(index);
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(theme.accentName(index));
        button.setTextSize(ui.sp(13));
        button.setTextColor(selected ? android.graphics.Color.WHITE : accent);
        button.setPadding(ui.px(8), ui.px(18), ui.px(8), ui.px(18));
        int fillColor = selected ? accent : theme.accentSoftColor(index);
        button.setBackground(ui.makeRoundedCard(fillColor, accent));
        button.setElevation(selected ? ui.px(4) : 0);
        button.setOnClickListener(v -> {
            theme.setAccentIndex(index);
            refreshSettings.run();
        });
        return button;
    }

    private void openUrl(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            android.widget.Toast.makeText(activity, "Link konnte nicht geöffnet werden", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private int themeModeChipId(int mode) {
        return 9000 + mode;
    }

    private int fontScaleChipId(int index) {
        return 9100 + index;
    }
}

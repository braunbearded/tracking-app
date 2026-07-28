package com.zaelio.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zaelio.app.theme.ThemeStore;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
        LinearLayout box = ui.screenBody(root, "Einstellungen", backHome);
        box.addView(themeCard());
        box.addView(fontCard());
        box.addView(fieldSizeCard());
        box.addView(accentCard());
    }

    public void renderAbout(LinearLayout root) {
        LinearLayout box = ui.screenBody(root, "Über die App", backHome);
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

        LinearLayout body = ui.contentCard();
        body.setPadding(ui.px(20), ui.px(18), ui.px(20), ui.px(16));
        box.addView(body);

        ui.addDialogTitle(body, "Über die App");
        ui.addDialogMessage(body, "Zaelio");
        body.addView(aboutInfoCard("Repository", "Zaelio", true));
        body.addView(aboutInfoCard("Version", versionName, false));
        body.addView(aboutInfoCard("Build", String.valueOf(versionCode), false));
    }

    private View aboutInfoCard(String label, String value, boolean clickable) {
        LinearLayout row = ui.contentCard();
        row.setPadding(ui.px(16), ui.px(12), ui.px(16), ui.px(12));

        TextView labelView = new TextView(activity);
        labelView.setText(label.toUpperCase(java.util.Locale.ROOT));
        labelView.setTextSize(ui.sp(12));
        labelView.setTextColor(theme.mutedTextColor());
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
            row.setOnClickListener(v -> openUrl("https://github.com/zaelio/zaelio"));
            row.setBackground(ui.makeRoundedCard(theme.surfaceAltColor(), theme.borderColor()));
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = ui.px(10);
        row.setLayoutParams(lp);
        return row;
    }

    private View themeCard() {
        LinearLayout card = ui.contentCard();
        ui.addSectionHeader(card, null, "Darstellung", null);

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
        LinearLayout card = ui.contentCard();
        ui.addSectionHeader(card, null, "Schriftgröße", null);

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

    private View fieldSizeCard() {
        LinearLayout card = ui.contentCard();
        ui.addSectionHeader(card, null, "Feldgröße", null);

        ChipGroup group = new ChipGroup(activity);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        group.setChipSpacingHorizontal(ui.px(8));
        group.setChipSpacingVertical(ui.px(8));

        for (int i = 0; i < theme.fieldSizeCount(); i++) {
            group.addView(fieldSizeChip(theme.fieldSizeName(i), i));
        }
        group.check(fieldSizeChipId(theme.fieldSizeIndex()));
        group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            for (int i = 0; i < theme.fieldSizeCount(); i++) {
                if (checkedId == fieldSizeChipId(i)) {
                    theme.setFieldSizeIndex(i);
                    refreshSettings.run();
                    break;
                }
            }
        });
        card.addView(group);
        return card;
    }

    private Chip fieldSizeChip(String label, int index) {
        Chip chip = new Chip(activity);
        chip.setId(fieldSizeChipId(index));
        chip.setText(label);
        styleChoiceChip(chip, theme.fieldSizeIndex() == index);
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
        LinearLayout card = ui.contentCard();
        ui.addSectionHeader(card, null, "Akzentfarbe", null);

        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(4);
            row.setPadding(0, 0, 0, ui.px(10));

            for (int col = 0; col < 4; col++) {
                int index = rowIndex * 4 + col;
                LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, -2, 1f);
                cellLp.leftMargin = col == 0 ? 0 : ui.px(4);
                cellLp.rightMargin = col == 3 ? 0 : ui.px(4);
                if (index >= theme.accentCount()) {
                    View spacer = new View(activity);
                    row.addView(spacer, cellLp);
                    continue;
                }
                row.addView(accentOption(index), cellLp);
            }
            card.addView(row);
        }
        return card;
    }

    private Button accentOption(int index) {
        boolean selected = theme.accentIndex() == index;
        int accent = theme.accentColor(index);
        int fillColor = selected ? accent : theme.accentSoftColor(index);
        Button button = ui.button(theme.accentName(index), fillColor, selected ? android.graphics.Color.WHITE : accent, accent);
        button.setTextSize(ui.sp(12));
        button.setSingleLine(true);
        button.setMinHeight(ui.px(44));
        button.setMinimumHeight(ui.px(44));
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

    private int fieldSizeChipId(int index) {
        return 9200 + index;
    }
}

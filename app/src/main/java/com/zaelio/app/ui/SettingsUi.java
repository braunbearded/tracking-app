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

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

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
        box.addView(themeCard(), cardLp());
        box.addView(fontCard(), cardLp());
        box.addView(fieldSizeCard(), cardLp());
        box.addView(accentCard(), cardLp());
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

        LinearLayout appCard = ui.contentCard();
        ui.addSectionHeader(appCard, "Zaelio", "Offline Tracker ohne Google-Dienste");
        box.addView(appCard, cardLp());

        box.addView(aboutInfoCard("Quellcode", "github.com/zaelio/zaelio", true));
        box.addView(aboutInfoCard("Version", versionName, false));
        box.addView(aboutInfoCard("Build", String.valueOf(versionCode), false));
    }

    private View aboutInfoCard(String label, String value, boolean clickable) {
        LinearLayout row = ui.contentCard();
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(ui.spaceL(), ui.spaceM(), ui.spaceM(), ui.spaceM());

        TextView labelView = new TextView(activity);
        labelView.setText(label);
        labelView.setTextSize(ui.sp(15));
        labelView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        labelView.setTextColor(theme.primaryTextColor());
        row.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1));

        TextView valueView = new TextView(activity);
        valueView.setText(value);
        valueView.setTextSize(ui.sp(13));
        valueView.setTextColor(theme.mutedTextColor());
        valueView.setGravity(android.view.Gravity.END);
        row.addView(valueView);

        if (clickable) {
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> openUrl("https://github.com/zaelio/zaelio"));
            row.setBackground(ui.makeRoundedCard(theme.surfaceAltColor(), theme.borderColor()));
        }

        row.setLayoutParams(cardLp());
        return row;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = ui.spaceSm();
        return lp;
    }

    private View themeCard() {
        int[] modes = {ThemeStore.THEME_SYSTEM, ThemeStore.THEME_LIGHT, ThemeStore.THEME_DARK};
        String[] labels = {"System", "Hell", "Dunkel"};
        return choiceCard("Darstellung", modes.length, selectedIndex(modes, theme.themeMode()), 9000,
                i -> labels[i], i -> theme.setThemeMode(modes[i]));
    }

    private View fontCard() {
        return choiceCard("Schriftgröße", theme.fontScaleCount(), theme.fontScaleIndex(), 9100,
                theme::fontScaleName, theme::setFontScaleIndex);
    }

    private View fieldSizeCard() {
        return choiceCard("Feldgröße", theme.fieldSizeCount(), theme.fieldSizeIndex(), 9200,
                theme::fieldSizeName, theme::setFieldSizeIndex);
    }

    private View choiceCard(String title, int count, int selected, int idBase, IntFunction<String> labelAt, IntConsumer selectAt) {
        LinearLayout card = ui.contentCard();
        ui.addSectionHeader(card, title, null);

        ChipGroup group = new ChipGroup(activity);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        group.setChipSpacingHorizontal(ui.spaceS());
        group.setChipSpacingVertical(ui.spaceS());
        for (int i = 0; i < count; i++) {
            group.addView(choiceChip(idBase + i, labelAt.apply(i), i == selected));
        }
        group.check(idBase + selected);
        group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                selectAt.accept(checkedIds.get(0) - idBase);
                refreshSettings.run();
            }
        });
        card.addView(group);
        return card;
    }

    private Chip choiceChip(int id, String label, boolean selected) {
        Chip chip = new Chip(activity);
        chip.setId(id);
        chip.setText(label);
        styleChoiceChip(chip, selected);
        return chip;
    }

    private int selectedIndex(int[] values, int selected) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == selected) {
                return i;
            }
        }
        return 0;
    }

    private void styleChoiceChip(Chip chip, boolean selected) {
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setTextColor(selected ? theme.accentColor() : theme.primaryTextColor());
        chip.setChipBackgroundColor(ColorStateList.valueOf(selected ? theme.accentSoftColor() : theme.surfaceAltColor()));
        chip.setChipStrokeColor(ColorStateList.valueOf(selected ? theme.accentColor() : theme.borderColor()));
        chip.setChipStrokeWidth(ui.strokeWidth());
        chip.setShapeAppearanceModel(ShapeAppearanceModel.builder()
                .setAllCornerSizes(ui.spaceS())
                .build());
        chip.setCheckedIconVisible(false);
        chip.setCheckedIcon(null);
        chip.setElevation(0);
    }

    private View accentCard() {
        LinearLayout card = ui.contentCard();
        ui.addSectionHeader(card, "Akzentfarbe", null);

        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(4);
            row.setPadding(0, 0, 0, ui.spaceSm());

            for (int col = 0; col < 4; col++) {
                int index = rowIndex * 4 + col;
                LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, -2, 1f);
                cellLp.leftMargin = col == 0 ? 0 : ui.spaceXs();
                cellLp.rightMargin = col == 3 ? 0 : ui.spaceXs();
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
        button.setMinHeight(ui.buttonHeight());
        button.setMinimumHeight(ui.buttonHeight());
        button.setElevation(selected ? ui.spaceXs() : 0);
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

}

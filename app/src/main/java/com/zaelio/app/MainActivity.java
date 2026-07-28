package com.zaelio.app;

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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import com.zaelio.app.theme.ThemeStore;
import com.zaelio.app.HomeUi;
import com.zaelio.app.ui.AppUi;
import com.zaelio.app.ui.SettingsUi;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends Activity {
    private static final int REQUEST_IMPORT_JSON = 10;
    private static final int REQUEST_EXPORT_JSON = 11;

    private TrackingDatabase db;
    private ThemeStore theme;
    private AppUi ui;
    private HomeUi homeUi;
    private SettingsUi settingsUi;
    private TrackerFlowUi trackerFlowUi;
    private LinearLayout root;
    private int currentTab = 0;
    private int transferMode = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = new ThemeStore(this);
        ui = new AppUi(this, theme);
        db = new TrackingDatabase(this);
        settingsUi = new SettingsUi(this, theme, ui, this::refreshSettings, this::refreshHome);
        trackerFlowUi = new TrackerFlowUi(this, db, theme, ui, handler, () -> showHome(0), () -> showHome(1));
        homeUi = new HomeUi(this, db, theme, ui, trackerFlowUi::openSession, trackerFlowUi::editTracker, this::refreshHome);
        showHome(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        if (requestCode == REQUEST_IMPORT_JSON) {
            importJson(data.getData());
        } else if (requestCode == REQUEST_EXPORT_JSON) {
            exportJson(data.getData());
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (trackerFlowUi != null) {
            trackerFlowUi.clearTimers();
        }
        super.onDestroy();
    }

    private void refreshHome() {
        showHome(currentTab);
    }

    private void refreshSettings() {
        showSettingsScreen();
    }

    private View bottomNav(int selectedTab) {
        return ui.bottomNav(selectedTab == 0, v -> showHome(0), v -> showHome(1));
    }

    private Button floatingActionButton(int tab) {
        return ui.floatingActionButton(v -> {
            if (tab == 0) {
                trackerFlowUi.chooseTracker();
            } else {
                trackerFlowUi.createTracker();
            }
        });
    }

    private void base() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(theme.backgroundColor());
        setContentView(root);
    }

    private void showHome(int tab) {
        currentTab = tab;
        base();

        root.addView(ui.appBar("Zaelio", false, null, true, this::showOverflowMenu));

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

    private void showOverflowMenu(View anchor) {
        LinearLayout card = ui.contentCard();
        card.setPadding(ui.px(20), ui.px(18), ui.px(20), ui.px(16));

        TextView title = ui.tv("Menü", 20);
        title.setPadding(0, 0, 0, ui.px(12));
        card.addView(title);

        final androidx.appcompat.app.AlertDialog[] dialog = new androidx.appcompat.app.AlertDialog[1];
        card.addView(menuButton("Einstellungen", () -> {
            dialog[0].dismiss();
            showSettingsScreen();
        }));
        card.addView(menuButton("Daten ex/importieren", () -> {
            dialog[0].dismiss();
            showDataTransferScreen();
        }));
        card.addView(menuButton("Über die App", () -> {
            dialog[0].dismiss();
            showAboutScreen();
        }));

        dialog[0] = new MaterialAlertDialogBuilder(this)
                .setView(card)
                .show();
        if (dialog[0].getWindow() != null) {
            dialog[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private View menuButton(String text, Runnable onClick) {
        Button button = ui.secondaryButton(text);
        button.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = ui.px(8);
        button.setLayoutParams(lp);
        return button;
    }

    private void showDataTransferScreen() {
        base();
        root.addView(ui.appBar("Daten ex/importieren", true, this::refreshHome, false, null));

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(ui.px(16), ui.px(16), ui.px(16), ui.px(104));
        scroll.addView(box);

        box.addView(transferCard("Alles", 0));
        box.addView(transferCard("Tracker", 1));
        box.addView(transferCard("Sessions", 2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private View transferCard(String title, int mode) {
        LinearLayout card = ui.contentCard();
        ui.addSectionHeader(card, null, title, null);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button importButton = ui.secondaryButton("Importieren");
        Button exportButton = ui.primaryButton("Exportieren");
        importButton.setOnClickListener(v -> chooseImportJson(mode));
        exportButton.setOnClickListener(v -> chooseExportJson(mode));
        row.addView(importButton, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams exportLp = new LinearLayout.LayoutParams(0, -2, 1);
        exportLp.leftMargin = ui.px(8);
        row.addView(exportButton, exportLp);
        card.addView(row);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = ui.px(12);
        card.setLayoutParams(lp);
        return card;
    }

    private void chooseImportJson(int mode) {
        transferMode = mode;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT_JSON);
    }

    private void chooseExportJson(int mode) {
        transferMode = mode;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "zaelio-" + transferName(mode) + "-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date()) + ".json");
        startActivityForResult(intent, REQUEST_EXPORT_JSON);
    }

    private void importJson(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            String json = readAll(in);
            int count = transferMode == 1
                    ? BackupJsonRepository.importTrackers(db, json)
                    : transferMode == 2 ? BackupJsonRepository.importSessions(db, json) : BackupJsonRepository.importAll(db, json);
            Toast.makeText(this, "Import abgeschlossen (" + count + " Tracker)", Toast.LENGTH_LONG).show();
            refreshHome();
        } catch (Exception e) {
            Toast.makeText(this, "Import fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportJson(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            String json = transferMode == 1
                    ? BackupJsonRepository.exportTrackers(db)
                    : transferMode == 2 ? BackupJsonRepository.exportSessions(db) : BackupJsonRepository.exportAll(db);
            out.write(json.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Export gespeichert", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String transferName(int mode) {
        return mode == 1 ? "trackers" : mode == 2 ? "sessions" : "backup";
    }

    private String readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toString("UTF-8");
    }

    private void showSettingsScreen() {
        base();
        settingsUi.render(root);
    }

    private void showAboutScreen() {
        base();
        settingsUi.renderAbout(root);
    }

    private void sessions(FrameLayout body) {
        homeUi.renderSessions(body);
    }

    private void trackers(FrameLayout body) {
        homeUi.renderTrackers(body);
    }
}

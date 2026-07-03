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
import com.example.trackingapp.HomeUi;
import com.example.trackingapp.ui.AppUi;
import com.example.trackingapp.ui.SettingsUi;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends Activity {
    private TrackingDatabase db;
    private ThemeStore theme;
    private AppUi ui;
    private HomeUi homeUi;
    private SettingsUi settingsUi;
    private TrackerFlowUi trackerFlowUi;
    private LinearLayout root;
    private int currentTab = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = new ThemeStore(this);
        ui = new AppUi(this, theme);
        db = new TrackingDatabase(this);
        settingsUi = new SettingsUi(this, theme, ui, this::refreshSettings, this::refreshHome);
        trackerFlowUi = new TrackerFlowUi(this, db, theme, ui, handler, () -> showHome(0), () -> showHome(1));
        homeUi = new HomeUi(this, db, theme, ui, trackerFlowUi::openSession, trackerFlowUi::editTracker);
        showHome(0);
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

        root.addView(ui.appBar("Tracking App", false, null, true, this::showOverflowMenu));

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
        settingsUi.render(root);
    }

    private void showAboutDialog() {
        settingsUi.showAboutDialog();
    }

    private void sessions(FrameLayout body) {
        homeUi.renderSessions(body);
    }

    private void trackers(FrameLayout body) {
        homeUi.renderTrackers(body);
    }
}

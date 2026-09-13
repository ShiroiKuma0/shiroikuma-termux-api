package com.termux.api.shiroikuma.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;

import com.termux.api.R;

/**
 * Host of the 白い熊 Termux API UI page (Phase 4) — a black toolbar carrying the yellow title, and
 * {@link ShiroikumaUiFragment} below it. Themed by {@code Theme.Shiroikuma.Ui} (manifest), so the
 * kxkb-style row layouts, the switches and every dialog come out black and yellow.
 *
 * <p>Reached from three places: the static launcher shortcut ({@code res/xml/shortcuts.xml}), a
 * long-press on the main activity's toolbar settings icon ({@link #installSettingsLongPress}), and
 * the first row of the Settings root screen ({@code res/xml/sets__termux.xml}).
 */
public class ShiroikumaUiActivity extends AppCompatActivity {

    public static void start(@NonNull Context context) {
        context.startActivity(new Intent(context, ShiroikumaUiActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shiroikuma_ui);

        Toolbar toolbar = findViewById(R.id.shiroikuma_toolbar);
        toolbar.setTitle(R.string.shiroikuma_ui_title);
        toolbar.setTitleTextColor(ShiroikumaViews.YELLOW);
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().mutate().setTint(ShiroikumaViews.YELLOW);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.shiroikuma_ui_fragment, new ShiroikumaUiFragment())
                    .commit();
        }
    }

    /**
     * Long-press on the toolbar's settings icon (or the overflow button, should the item ever fold
     * into it) opens this page. The action views only exist after the menu is laid out, hence the
     * {@code post()}: the settings item renders as an {@code ActionMenuItemView} carrying the menu
     * item's id, the overflow button as the {@code ActionMenuView}'s only plain {@code ImageView}.
     * Setting the listener after layout also replaces the item's tooltip long-press, which is the
     * point. Called from {@code TermuxAPIMainActivity.onCreateOptionsMenu} (one line upstream-side).
     */
    public static void installSettingsLongPress(@NonNull Activity activity, @Nullable View toolbarView, int menuItemId) {
        if (!(toolbarView instanceof Toolbar)) return;
        final Toolbar toolbar = (Toolbar) toolbarView;
        toolbar.post(() -> {
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                View child = toolbar.getChildAt(i);
                if (!(child instanceof ActionMenuView)) continue;
                ActionMenuView menuView = (ActionMenuView) child;
                for (int j = 0; j < menuView.getChildCount(); j++) {
                    View button = menuView.getChildAt(j);
                    if (button.getId() == menuItemId || button instanceof ImageView) {
                        button.setOnLongClickListener(v -> {
                            start(activity);
                            return true;
                        });
                    }
                }
            }
        });
    }
}

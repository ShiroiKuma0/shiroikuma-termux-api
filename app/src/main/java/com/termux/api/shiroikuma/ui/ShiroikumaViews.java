package com.termux.api.shiroikuma.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

/**
 * The house look, as view builders: black surfaces, yellow ink, bordered rounded boxes, pill
 * buttons, and the black-yellow info / confirm dialogs the UI page and the Export / Import panel
 * share. Fixed colours — this app has no appearance section, so nothing here is configurable.
 */
public final class ShiroikumaViews {

    public static final int BLACK = 0xFF000000;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int YELLOW_DIM = 0xFFC8C800;
    public static final int WARN = 0xFFFF5252;

    private ShiroikumaViews() {
    }

    public static int dp(@NonNull Context context, float v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }

    /** The bordered rounded panel every dialog surface is drawn on: black fill, 2dp yellow stroke, 16dp radius. */
    @NonNull
    public static GradientDrawable panelBackground(@NonNull Context context) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLACK);
        bg.setStroke(Math.max(1, dp(context, 2)), YELLOW);
        bg.setCornerRadius(dp(context, 16));
        return bg;
    }

    /** A smaller bordered box (the directory box): 2dp stroke, 10dp radius. */
    @NonNull
    public static GradientDrawable boxBackground(@NonNull Context context) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLACK);
        bg.setStroke(Math.max(1, dp(context, 2)), YELLOW);
        bg.setCornerRadius(dp(context, 10));
        return bg;
    }

    @NonNull
    public static TextView text(@NonNull Context context, @Nullable CharSequence s, int sizeSp, int color, boolean bold) {
        TextView tv = new TextView(context);
        tv.setText(s);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        if (bold) tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        return tv;
    }

    @NonNull
    public static CheckBox checkbox(@NonNull Context context, @NonNull String label, boolean bold, int indentPx) {
        CheckBox cb = new CheckBox(context);
        cb.setText(label);
        cb.setTextColor(YELLOW);
        cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        if (bold) cb.setTypeface(cb.getTypeface(), Typeface.BOLD);
        cb.setButtonTintList(ColorStateList.valueOf(YELLOW));
        cb.setPadding(dp(context, 8) + indentPx, dp(context, 7), 0, dp(context, 7));
        return cb;
    }

    @NonNull
    public static View divider(@NonNull Context context, int topGapDp) {
        View v = new View(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(context, 1)));
        lp.topMargin = dp(context, topGapDp);
        v.setLayoutParams(lp);
        v.setBackgroundColor(YELLOW);
        v.setAlpha(0.4f);
        return v;
    }

    /** The house pill: black fill, 1.5dp yellow stroke, 50dp corners, yellow ripple, no all-caps, minWidth 0. */
    @NonNull
    public static Button pill(@NonNull Context context, @NonNull String label, @Nullable View.OnClickListener onClick) {
        Button b = new Button(context);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(YELLOW);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLACK);
        bg.setStroke(Math.max(1, Math.round(1.5f * context.getResources().getDisplayMetrics().density)), YELLOW);
        bg.setCornerRadius(dp(context, 50));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf((YELLOW & 0x00FFFFFF) | 0x33000000), bg, null));
        b.setStateListAnimator(null);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(context, 20), dp(context, 8), dp(context, 20), dp(context, 8));
        b.setOnClickListener(onClick);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return b;
    }

    /** A title + body inside the bordered panel; the caller appends its button row. */
    @NonNull
    public static LinearLayout infoBox(@NonNull Context context, @NonNull String title, @NonNull CharSequence body) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(context, 22), dp(context, 20), dp(context, 22), dp(context, 16));
        box.setBackground(panelBackground(context));
        box.addView(text(context, title, 19, YELLOW, true));
        TextView bodyView = text(context, body, 14, YELLOW, false);
        bodyView.setPadding(0, dp(context, 10), 0, 0);
        box.addView(bodyView);
        return box;
    }

    /** A dialog whose only surface is {@code content} — the window itself is transparent. */
    @NonNull
    public static AlertDialog boxDialog(@NonNull Activity activity, @NonNull View content, boolean cancelable) {
        ScrollView scroll = new ScrollView(activity);
        int m = dp(activity, 10);
        scroll.setPadding(m, m, m, m);
        scroll.setClipToPadding(false);
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(activity).setView(scroll).create();
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(cancelable);
        return dialog;
    }

    public static void transparentWindow(@NonNull AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @NonNull
    public static LinearLayout buttonRow(@NonNull Context context) {
        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        buttons.setPadding(0, dp(context, 16), 0, 0);
        return buttons;
    }

    /** A bordered info dialog with one OK pill, right-aligned; {@code onOk} runs after it closes. */
    public static void showInfo(@NonNull Activity activity, @NonNull String title, @NonNull CharSequence body,
                                boolean cancelable, @Nullable Runnable onOk) {
        LinearLayout box = infoBox(activity, title, body);
        final AlertDialog dialog = boxDialog(activity, box, cancelable);
        LinearLayout buttons = buttonRow(activity);
        buttons.addView(pill(activity, activity.getString(com.termux.api.R.string.shiroikuma_eim_ok), v -> {
            dialog.dismiss();
            if (onOk != null) onOk.run();
        }));
        box.addView(buttons);
        dialog.show();
        transparentWindow(dialog);
    }

    /** A bordered confirm dialog: Cancel on the left, the action pill on the right. */
    public static void showConfirm(@NonNull Activity activity, @NonNull String title, @NonNull CharSequence body,
                                   @NonNull String actionLabel, @NonNull Runnable onConfirm) {
        LinearLayout box = infoBox(activity, title, body);
        final AlertDialog dialog = boxDialog(activity, box, true);
        LinearLayout buttons = buttonRow(activity);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        buttons.addView(pill(activity, activity.getString(com.termux.api.R.string.shiroikuma_eim_cancel), v -> dialog.dismiss()));
        View spacer = new View(activity);
        buttons.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));
        buttons.addView(pill(activity, actionLabel, v -> {
            dialog.dismiss();
            onConfirm.run();
        }));
        box.addView(buttons);
        dialog.show();
        transparentWindow(dialog);
    }
}

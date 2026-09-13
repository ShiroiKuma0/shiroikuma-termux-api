package com.termux.api.shiroikuma.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.termux.api.R;
import com.termux.api.shiroikuma.automation.AutomationAuth;
import com.termux.api.shiroikuma.backup.ShiroikumaExport;

/**
 * The 白い熊 Termux API UI page (Phase 4): the Export / Import section — the panel entry, the export
 * directory, the last export, and the three 保存復元 automation rows — followed by Reset. This app
 * has no appearance sections: Termux:API draws almost nothing of its own.
 *
 * <p>Rows are declared in {@code res/xml/preferences_shiroikuma_ui.xml} with the kxkb-style row
 * layouts; every value lives in the fork's two device-local prefs files, never in the exported app
 * preferences, so each row is {@code persistent="false"} and wired by key here.
 */
public class ShiroikumaUiFragment extends PreferenceFragmentCompat {

    /** {@code OpenDocument} that opens at the export directory (EXTRA_INITIAL_URI) when one is set. */
    private static final class OpenBackup extends ActivityResultContracts.OpenDocument {
        @Nullable
        Uri initial;

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull String[] input) {
            Intent intent = super.createIntent(context, input);
            if (initial != null) intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial);
            return intent;
        }
    }

    private final OpenBackup mOpenBackup = new OpenBackup();

    private final ActivityResultLauncher<Uri> mPickExportDir =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), this::onExportDirPicked);
    private final ActivityResultLauncher<String[]> mPickImportFile =
            registerForActivityResult(mOpenBackup, this::onImportFilePicked);

    @Nullable
    private ExportImportPanel mPanel;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_shiroikuma_ui, rootKey);

        Preference eximport = findPreference("pref_eximport");
        if (eximport != null) {
            eximport.setOnPreferenceClickListener(p -> {
                openPanel();
                return true;
            });
        }
        Preference dir = findPreference("pref_export_dir");
        if (dir != null) {
            dir.setOnPreferenceClickListener(p -> {
                mPickExportDir.launch(ShiroikumaExport.exportDirUri(requireContext()));
                return true;
            });
        }
        initializeAutomationRows();
        Preference reset = findPreference("pref_reset_ui");
        if (reset != null) {
            reset.setOnPreferenceClickListener(p -> {
                confirmReset();
                return true;
            });
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // No dividers between rows; the section hairlines are part of the category layouts.
        setDivider(null);
        setDividerHeight(0);
        View list = view.findViewById(android.R.id.list);
        if (list != null) list.setPadding(0, 0, 0, ShiroikumaViews.dp(view.getContext(), 24));
        view.setBackgroundColor(ShiroikumaViews.BLACK);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDirRow();
        refreshLastExportRow();
    }

    // --- Export / Import rows -------------------------------------------------------------------

    private void openPanel() {
        if (mPanel != null && mPanel.isShowing()) return;
        mPanel = new ExportImportPanel(requireActivity(), new ExportImportPanel.Host() {
            @Override
            public void pickExportDir(@Nullable Uri initial) {
                mPickExportDir.launch(initial);
            }

            @Override
            public void pickImportFile(@Nullable Uri initial) {
                mOpenBackup.initial = initial;
                mPickImportFile.launch(new String[]{"application/zip", "application/octet-stream", "*/*"});
            }

            @Override
            public void onChainFinished() {
                // A finished export/import closes the whole chain: info dialog → panel → this page.
                if (getActivity() != null) getActivity().finish();
            }
        });
        mPanel.show();
    }

    private void onExportDirPicked(@Nullable Uri uri) {
        if (uri == null) return;
        if (mPanel != null && mPanel.isShowing()) {
            mPanel.onDirPicked(uri);
        } else {
            ShiroikumaExport.setExportDirUri(requireContext(), uri);
        }
        refreshDirRow();
        refreshLastExportRow();
    }

    private void onImportFilePicked(@Nullable Uri uri) {
        if (uri != null && mPanel != null && mPanel.isShowing()) mPanel.onImportFilePicked(uri);
    }

    /** The export directory — its path in yellow, or a red "not set". */
    private void refreshDirRow() {
        Preference dir = findPreference("pref_export_dir");
        Context ctx = getContext();
        if (dir == null || ctx == null) return;
        String label = ShiroikumaExport.dirLabel(ctx);
        dir.setSummary(label != null ? label : warn(getString(R.string.shiroikuma_eim_dir_unset)));
    }

    /** The newest backup — queried on a background thread: yellow date/time/size, red "none" / "no directory". */
    private void refreshLastExportRow() {
        Preference last = findPreference("pref_last_export");
        Context ctx = getContext();
        if (last == null || ctx == null) return;
        if (ShiroikumaExport.exportDir(ctx) == null) {
            last.setSummary(warn(getString(R.string.shiroikuma_eim_last_nodir)));
            return;
        }
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            DocumentFile newest = ShiroikumaExport.newestExport(app);
            final CharSequence summary;
            if (newest == null) {
                summary = warn(app.getString(R.string.shiroikuma_eim_last_none));
            } else {
                long ts = newest.lastModified();
                String when = DateFormat.getDateFormat(app).format(ts) + " " + DateFormat.getTimeFormat(app).format(ts);
                summary = app.getString(R.string.shiroikuma_eim_last_value, when, ShiroikumaExport.humanSize(newest.length()));
            }
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                Preference p = findPreference("pref_last_export");
                if (p != null) p.setSummary(summary);
            });
        }, "shiroikuma-last-export").start();
    }

    private static CharSequence warn(String s) {
        SpannableString span = new SpannableString(s);
        span.setSpan(new ForegroundColorSpan(ShiroikumaViews.WARN), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }

    // --- 保存復元 automation (contract v2 §2) — inside the Export / Import section ---------------

    /**
     * Three rows, in the order every sister app shows them: the master switch (default ON), 「Use
     * authorization token?」 (default OFF), and the token row — shown only while the token is being
     * asked for; tap copies, the pill regenerates after a confirm.
     */
    private void initializeAutomationRows() {
        final Context ctx = requireContext();
        SwitchPreferenceCompat enabled = findPreference("pref_automation_enabled");
        if (enabled != null) {
            enabled.setChecked(AutomationAuth.isEnabled(ctx));
            enabled.setOnPreferenceChangeListener((p, value) -> {
                AutomationAuth.setEnabled(ctx, Boolean.TRUE.equals(value));
                return true;
            });
        }

        final AutomationTokenPreference token = findPreference("pref_automation_token");
        SwitchPreferenceCompat requireToken = findPreference("pref_automation_require_token");
        if (requireToken != null) {
            boolean required = AutomationAuth.isTokenRequired(ctx);
            requireToken.setChecked(required);
            if (token != null) token.setVisible(required);
            requireToken.setOnPreferenceChangeListener((p, value) -> {
                boolean now = Boolean.TRUE.equals(value);
                AutomationAuth.setTokenRequired(ctx, now);
                if (token != null) {
                    token.setVisible(now);
                    if (now) updateTokenRow(token);
                }
                return true;
            });
        }

        if (token == null) return;
        updateTokenRow(token);
        token.setOnPreferenceClickListener(p -> {
            ClipboardManager cb = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) {
                cb.setPrimaryClip(ClipData.newPlainText("automation_token", AutomationAuth.token(ctx)));
                Toast.makeText(ctx, R.string.shiroikuma_auto_token_copied, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        token.setOnRegenerateListener(() -> {
            if (getActivity() == null) return;
            ShiroikumaViews.showConfirm(getActivity(),
                    getString(R.string.shiroikuma_auto_token_regen_title),
                    getString(R.string.shiroikuma_auto_token_regen_msg),
                    getString(R.string.shiroikuma_auto_regenerate),
                    () -> {
                        AutomationAuth.regenerateToken(ctx);
                        updateTokenRow(token);
                        Toast.makeText(ctx, R.string.shiroikuma_auto_token_regenerated, Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void updateTokenRow(@NonNull AutomationTokenPreference token) {
        Context ctx = getContext();
        if (ctx == null) return;
        token.setSummary(AutomationAuth.abbreviate(AutomationAuth.token(ctx)) + "\n"
                + getString(R.string.shiroikuma_auto_token_desc));
    }

    // --- Reset ----------------------------------------------------------------------------------

    /** Clears the fork's own prefs — the export directory and the automation rows — after a confirm. */
    private void confirmReset() {
        if (getActivity() == null) return;
        ShiroikumaViews.showConfirm(getActivity(),
                getString(R.string.shiroikuma_reset_confirm_title),
                getString(R.string.shiroikuma_reset_confirm_msg),
                getString(R.string.shiroikuma_eim_ok),
                () -> {
                    Context ctx = requireContext();
                    ShiroikumaExport.setExportDirUri(ctx, null);
                    AutomationAuth.reset(ctx);
                    requireActivity().recreate();
                });
    }
}

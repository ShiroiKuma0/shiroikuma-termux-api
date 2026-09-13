package com.termux.api.shiroikuma.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.termux.api.R;

/**
 * The automation-token row of the Export / Import section: tapping the row copies the token, and a
 * "Regenerate" pill sits on the right (inside the row's widget frame, from
 * {@code @layout/preference_widget_shiroikuma_regenerate}). Ported from ArcaneChat.
 */
public class AutomationTokenPreference extends Preference {

    @Nullable
    private Runnable onRegenerate;

    public AutomationTokenPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AutomationTokenPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AutomationTokenPreference(@NonNull Context context) {
        super(context);
    }

    public void setOnRegenerateListener(@Nullable Runnable onRegenerate) {
        this.onRegenerate = onRegenerate;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View button = holder.findViewById(R.id.shiroikuma_automation_regenerate);
        if (button == null) return;
        // The pill consumes the touch, so tapping it does not also fire the row's copy action.
        button.setOnClickListener(v -> {
            if (onRegenerate != null) onRegenerate.run();
        });
    }
}

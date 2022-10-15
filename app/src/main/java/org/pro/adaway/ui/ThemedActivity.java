package org.pro.adaway.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import org.pro.adaway.helper.PreferenceHelper;
import org.pro.adaway.ui.support.SupportActivity;
import org.pro.adaway.ui.welcome.WelcomeActivity;

public abstract class ThemedActivity extends AppCompatActivity {
    @Override
    protected final void attachBaseContext(final Context newBase) {
        super.attachBaseContext(newBase);
        applyTheme();
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyTheme();
    }

    /**
     * Applies theme from {@link android.content.SharedPreferences} settings
     */
    private void applyTheme() {
        if (canApplyTheme(this)) applyTheme(this);
    }

    /**
     * Checks if {@link Activity} is applicable for applying theme
     *
     * @param activity the activity for checking
     *
     * @return `true` if theming allowed, else `false`
     */
    private static boolean canApplyTheme(final Activity activity) {
        return !(activity instanceof SupportActivity
                || activity instanceof WelcomeActivity
        );
    }

    /**
     * Apply the user selected theme.
     *
     * @param context The context to apply theme.
     */
    private static void applyTheme(final Context context) {
        try {
            AppCompatDelegate.setDefaultNightMode(PreferenceHelper.getDarkThemeMode(context));
        } catch (Exception e) {
            // ignore
        }
    }
}
package org.pro.adaway.ui.prefs;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.pro.adaway.R;
import org.pro.adaway.ui.ThemedActivity;

/**
 * This activity is the preferences activity.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class PrefsActivity extends ThemedActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    static final String PREFERENCE_NOT_FOUND = "preference not found";

    static void setAppBarTitle(@NonNull final PreferenceFragmentCompat fragment, @StringRes final int title) {
        final FragmentActivity activity = fragment.getActivity();
        if (activity instanceof PrefsActivity) {
            final ActionBar supportActionBar = ((PrefsActivity) activity).getSupportActionBar();
            if (supportActionBar != null) supportActionBar.setTitle(title);
        }
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set view content.
        setContentView(R.layout.prefs_activity);

        // Configure actionbar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null)
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new PrefsMainFragment())
                    .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) fragmentManager.popBackStack();
        else super.onSupportNavigateUp();
        return true;
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull final PreferenceFragmentCompat caller, @NonNull final Preference pref) {
        // Instantiate the new fragment
        final String fragmentClassName = pref.getFragment();
        if (fragmentClassName == null) return false;

        final FragmentManager fragmentManager = getSupportFragmentManager();

        final Fragment fragment = fragmentManager.getFragmentFactory().instantiate(getClassLoader(), fragmentClassName);
        final Bundle args = pref.getExtras();
        fragment.setArguments(args);
        // See https://developer.android.com/guide/topics/ui/settings/organize-your-settings#java
        // noinspection deprecation
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        fragmentManager.beginTransaction()
                .setCustomAnimations(
                        R.animator.fragment_open_enter, R.animator.fragment_open_exit,
                        R.animator.fragment_close_enter, R.animator.fragment_close_exit
                )
                .replace(R.id.settings_container, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }
}

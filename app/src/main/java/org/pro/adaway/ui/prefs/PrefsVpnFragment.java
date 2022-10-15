package org.pro.adaway.ui.prefs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.pro.adaway.R;
import org.pro.adaway.ui.prefs.exclusion.PrefsVpnExcludedAppsActivity;
import org.pro.adaway.vpn.VpnServiceControls;

import static org.pro.adaway.ui.prefs.PrefsActivity.PREFERENCE_NOT_FOUND;
import static org.pro.adaway.util.Constants.PREFS_NAME;

/**
 * This fragment is the preferences fragment for VPN ad blocker.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class PrefsVpnFragment extends PreferenceFragmentCompat {
    private ActivityResultLauncher<Intent> startActivityLauncher;

    @Override
    public void onCreatePreferences(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey) {
        // Configure preferences
        getPreferenceManager().setSharedPreferencesName(PREFS_NAME);
        addPreferencesFromResource(R.xml.preferences_vpn);
        // Register for activity
        registerForStartActivity();
        // Bind pref actions
        bindExcludedSystemApps();
        bindExcludedUserApps();
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        PrefsActivity.setAppBarTitle(this, R.string.pref_vpn_title);
    }

    private void registerForStartActivity() {
        this.startActivityLauncher = registerForActivityResult(new StartActivityForResult(), result -> restartVpn());
    }

    private void bindExcludedSystemApps() {
        final ListPreference excludeUserAppsPreferences = findPreference(getString(R.string.pref_vpn_excluded_system_apps_key));
        assert excludeUserAppsPreferences != null : PREFERENCE_NOT_FOUND;
        excludeUserAppsPreferences.setOnPreferenceChangeListener((preference, newValue) -> {
            restartVpn();
            return true;
        });
    }

    private void bindExcludedUserApps() {
        final Context context = requireContext();
        final Preference excludeUserAppsPreferences = findPreference(getString(R.string.pref_vpn_excluded_user_apps_key));
        assert excludeUserAppsPreferences != null : PREFERENCE_NOT_FOUND;
        excludeUserAppsPreferences.setOnPreferenceClickListener(preference -> {
            this.startActivityLauncher.launch(new Intent(context, PrefsVpnExcludedAppsActivity.class));
            return true;
        });
    }

    private void restartVpn() {
        final Context context = requireContext();
        if (VpnServiceControls.isRunning(context)) {
            VpnServiceControls.stop(context);
            VpnServiceControls.start(context);
        }
    }
}

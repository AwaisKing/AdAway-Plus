package org.pro.adaway.ui.prefs;

import static org.pro.adaway.model.adblocking.AdBlockMethod.ROOT;
import static org.pro.adaway.model.adblocking.AdBlockMethod.VPN;
import static org.pro.adaway.ui.prefs.PrefsActivity.PREFERENCE_NOT_FOUND;
import static org.pro.adaway.util.Constants.PREFS_NAME;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.pro.adaway.R;
import org.pro.adaway.helper.PreferenceHelper;
import org.pro.adaway.model.adblocking.AdBlockMethod;

/**
 * This fragment is the preferences main fragment.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class PrefsMainFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        // Configure preferences
        getPreferenceManager().setSharedPreferencesName(PREFS_NAME);
        addPreferencesFromResource(R.xml.preferences_main);
        // Bind pref actions
        bindThemePrefAction();
        bindAdBlockMethod();
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        PrefsActivity.setAppBarTitle(this, R.string.pref_main_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        PrefsActivity.setAppBarTitle(this, R.string.pref_main_title);
    }

    private void bindThemePrefAction() {
        final Preference darkThemePref = findPreference(getString(R.string.pref_dark_theme_mode_key));
        assert darkThemePref != null : PREFERENCE_NOT_FOUND;
        darkThemePref.setOnPreferenceChangeListener((preference, newValue) -> {
            requireActivity().recreate();
            // Allow preference change
            return true;
        });
    }

    private void bindAdBlockMethod() {
        final Preference rootPreference = findPreference(getString(R.string.pref_root_ad_block_method_key));
        assert rootPreference != null : PREFERENCE_NOT_FOUND;
        final Preference vpnPreference = findPreference(getString(R.string.pref_vpn_ad_block_method_key));
        assert vpnPreference != null : PREFERENCE_NOT_FOUND;
        final AdBlockMethod adBlockMethod = PreferenceHelper.getAdBlockMethod(requireContext());
        rootPreference.setEnabled(adBlockMethod == ROOT);
        vpnPreference.setEnabled(adBlockMethod == VPN);
    }
}
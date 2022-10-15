package org.pro.adaway.ui.prefs;

import static android.os.Build.VERSION.SDK_INT;
import static android.provider.Settings.ACTION_SECURITY_SETTINGS;
import static android.widget.Toast.LENGTH_SHORT;
import static org.pro.adaway.model.root.MountType.READ_ONLY;
import static org.pro.adaway.model.root.MountType.READ_WRITE;
import static org.pro.adaway.model.root.ShellUtils.isWritable;
import static org.pro.adaway.model.root.ShellUtils.remountPartition;
import static org.pro.adaway.ui.prefs.PrefsActivity.PREFERENCE_NOT_FOUND;
import static org.pro.adaway.util.Constants.ANDROID_SYSTEM_ETC_HOSTS;
import static org.pro.adaway.util.Constants.PREFS_NAME;
import static org.pro.adaway.util.WebServerUtils.TEST_URL;
import static org.pro.adaway.util.WebServerUtils.copyCertificate;
import static org.pro.adaway.util.WebServerUtils.getWebServerState;
import static org.pro.adaway.util.WebServerUtils.installCertificate;
import static org.pro.adaway.util.WebServerUtils.isWebServerRunning;
import static org.pro.adaway.util.WebServerUtils.startWebServer;
import static org.pro.adaway.util.WebServerUtils.stopWebServer;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.net.InetAddresses;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.R;
import org.pro.adaway.helper.PreferenceHelper;
import org.pro.adaway.ui.dialog.MissingAppDialog;
import org.pro.adaway.util.AppExecutors;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import awaisome.compat.CreateDocument;

/**
 * This fragment is the preferences fragment for root ad blocker.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class PrefsRootFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    /**
     * The webserver certificate mime type.
     */
    private static final String CERTIFICATE_MIME_TYPE = "application/x-x509-ca-cert";
    /**
     * The launcher to start open hosts file activity.
     */
    private ActivityResultLauncher<Intent> openHostsFileLauncher;
    /**
     * The launcher to prepare web service certificate activity.
     */
    private ActivityResultLauncher<String> prepareCertificateLauncher;

    @Override
    public void onCreatePreferences(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey) {
        final PreferenceManager preferenceManager = getPreferenceManager();
        // Configure preferences
        preferenceManager.setSharedPreferencesName(PREFS_NAME);
        addPreferencesFromResource(R.xml.preferences_root);
        // Register for activities
        registerForOpenHostActivity();
        registerForPrepareCertificateActivity();
        // Bind pref actions
        bindOpenHostsFile();
        bindRedirection();
        bindWebServerPrefAction();
        bindWebServerTest();
        bindWebServerCertificate();
        // Update current state
        updateWebServerState();
        // Register as listener
        final SharedPreferences sharedPreferences = preferenceManager.getSharedPreferences();
        assert sharedPreferences != null;
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        PrefsActivity.setAppBarTitle(this, R.string.pref_root_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update current state
        updateWebServerState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister as listener
        final SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null) sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        final Context context = requireContext();
        // Restart web server on icon change
        if (context.getString(R.string.pref_webserver_icon_key).equals(key) && isWebServerRunning()) {
            stopWebServer();
            startWebServer(context);
            updateWebServerState();
        }
    }

    private void registerForOpenHostActivity() {
        this.openHostsFileLauncher = registerForActivityResult(new StartActivityForResult(), result -> {
            try {
                final File hostFile = new File(ANDROID_SYSTEM_ETC_HOSTS).getCanonicalFile();
                remountPartition(hostFile, READ_ONLY);
            } catch (final IOException e) {
                if (BuildConfig.DEBUG)
                    Log.e("AWAISKING_APP", "Failed to get hosts canonical file.", e);
            }
        });
    }

    private void registerForPrepareCertificateActivity() {
        this.prepareCertificateLauncher = registerForActivityResult(new CreateDocument(CERTIFICATE_MIME_TYPE),
                this::prepareWebServerCertificate);
    }

    private void bindOpenHostsFile() {
        final Preference openHostsFilePreference = findPreference(getString(R.string.pref_open_hosts_key));
        assert openHostsFilePreference != null : PREFERENCE_NOT_FOUND;
        openHostsFilePreference.setOnPreferenceClickListener(this::openHostsFile);
    }

    private boolean openHostsFile(final Preference preference) {
        try {
            final File hostFile = new File(ANDROID_SYSTEM_ETC_HOSTS).getCanonicalFile();
            final boolean remount = !isWritable(hostFile) && remountPartition(hostFile, READ_WRITE);
            final Intent intent = new Intent().setAction(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.parse("file://" + hostFile.getAbsolutePath()), "text/plain");
            if (remount) this.openHostsFileLauncher.launch(intent);
            else startActivity(intent);
            return true;
        } catch (final IOException e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "Failed to get hosts canonical file.", e);
        } catch (final ActivityNotFoundException e) {
            MissingAppDialog.showTextEditorMissingDialog(getContext());
            return false;
        }
        return false;
    }

    private void bindRedirection() {
        final Context context = requireContext();
        final boolean ipv6Enabled = PreferenceHelper.getEnableIpv6(context);

        final Preference ipv4RedirectionPreference = findPreference(getString(R.string.pref_redirection_ipv4_key));
        assert ipv4RedirectionPreference != null : PREFERENCE_NOT_FOUND;
        ipv4RedirectionPreference.setOnPreferenceChangeListener((preference, newValue) ->
                validateRedirection(Inet4Address.class, (String) newValue));

        final Preference ipv6RedirectionPreference = findPreference(getString(R.string.pref_redirection_ipv6_key));
        assert ipv6RedirectionPreference != null : PREFERENCE_NOT_FOUND;
        ipv6RedirectionPreference.setEnabled(ipv6Enabled);
        ipv6RedirectionPreference.setOnPreferenceChangeListener((preference, newValue) ->
                validateRedirection(Inet6Address.class, (String) newValue)
        );
    }

    private boolean validateRedirection(@NonNull final Class<? extends InetAddress> addressType, final String redirection) {
        boolean valid;
        try {
            final InetAddress inetAddress = InetAddresses.forString(redirection);
            valid = addressType.isAssignableFrom(inetAddress.getClass());
        } catch (final IllegalArgumentException exception) {
            valid = false;
        }
        if (!valid) Toast.makeText(requireContext(), R.string.pref_redirection_invalid, LENGTH_SHORT).show();
        return valid;
    }

    private void bindWebServerPrefAction() {
        final Context context = requireContext();
        // Start web server when preference is enabled
        final SwitchPreferenceCompat webServerEnabledPref = findPreference(getString(R.string.pref_webserver_enabled_key));
        assert webServerEnabledPref != null : PREFERENCE_NOT_FOUND;
        webServerEnabledPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue.equals(true)) {
                // Start web server
                startWebServer(context);
                updateWebServerState();
                return isWebServerRunning();
            } else {
                // Stop web server
                stopWebServer();
                updateWebServerState();
                return !isWebServerRunning();
            }
        });
    }

    private void bindWebServerTest() {
        final Preference webServerTest = findPreference(getString(R.string.pref_webserver_test_key));
        assert webServerTest != null : PREFERENCE_NOT_FOUND;
        webServerTest.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(TEST_URL)));
            return true;
        });
    }

    private void bindWebServerCertificate() {
        final Preference webServerTest = findPreference(getString(R.string.pref_webserver_certificate_key));
        assert webServerTest != null : PREFERENCE_NOT_FOUND;
        webServerTest.setOnPreferenceClickListener(preference -> {
            if (SDK_INT < VERSION_CODES.R) installCertificate(requireContext());
            else this.prepareCertificateLauncher.launch("adaway-webserver-certificate.crt");
            return true;
        });
    }

    private void prepareWebServerCertificate(final Uri uri) {
        // Check user selected document
        if (uri == null) return;
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Certificate URI: " + uri);
        copyCertificate(requireActivity(), uri);
        new MaterialAlertDialogBuilder(requireContext())
                .setCancelable(true)
                .setTitle(R.string.pref_webserver_certificate_dialog_title)
                .setMessage(R.string.pref_webserver_certificate_dialog_content)
                .setPositiveButton(R.string.pref_webserver_certificate_dialog_action, (dialog, which) -> {
                    dialog.dismiss();
                    startActivity(new Intent(ACTION_SECURITY_SETTINGS));
                })
                .create()
                .show();
    }

    private void updateWebServerState() {
        final Preference webServerTest = findPreference(getString(R.string.pref_webserver_test_key));
        assert webServerTest != null : PREFERENCE_NOT_FOUND;
        webServerTest.setSummary(R.string.pref_webserver_state_checking);
        final AppExecutors executors = AppExecutors.getInstance();
        executors.networkIO().execute(() -> {
            // Wait for server to start
            try {
                Thread.sleep(500);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final int summaryResId = getWebServerState();
            executors.mainThread().execute(() -> webServerTest.setSummary(summaryResId));
        });
    }
}

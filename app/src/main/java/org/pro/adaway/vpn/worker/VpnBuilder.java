package org.pro.adaway.vpn.worker;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;
import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;
import static java.util.Collections.emptySet;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.R;
import org.pro.adaway.helper.PreferenceHelper;
import org.pro.adaway.ui.home.HomeActivity;
import org.pro.adaway.vpn.dns.DnsServerMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This utility class is in charge of establishing a new VPN interface.
 *
 * @author Bruce BUJON
 */
public final class VpnBuilder {
    /**
     * Private constructor.
     */
    private VpnBuilder() {}

    /**
     * Establish the VPN interface.
     *
     * @param service         The VPN service to create interface.
     * @param dnsServerMapper The DNS server mapper used to configure VPN address and routes.
     *
     * @return The VPN interface.
     */
    public static ParcelFileDescriptor establish(final VpnService service, final DnsServerMapper dnsServerMapper) {
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Establishing VPN…");
        final VpnService.Builder builder = service.new Builder();
        // Configure VPN address and DNS servers
        dnsServerMapper.configureVpn(service, builder);
        // Exclude applications from VPN according user preferences (all applications goes through VPN by default)
        excludeApplicationsFromVpn(service, builder);
        // Allow applications to bypass the VPN by programmatically binding to a network for compatibility
        builder.allowBypass();
        // Set file descriptor in blocking mode as worker has a dedicated thread
        builder.setBlocking(true);
        // Set the VPN to unmetered
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            builder.setMetered(false);
        // Explicitly allow both families to prevent a family being blocked if no DNS server is found with it
        builder.allowFamily(AF_INET);
        builder.allowFamily(AF_INET6);

        // Create a new interface.
        final ParcelFileDescriptor pfd = builder
                .setSession(service.getString(R.string.app_name))
                .setConfigureIntent(PendingIntent.getActivity(service, 1, new Intent(service, HomeActivity.class),
                        FLAG_CANCEL_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? FLAG_IMMUTABLE : 0)))
                .establish();
        if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "VPN established.");
        return pfd;
    }

    private static void excludeApplicationsFromVpn(@NonNull final Context context, final VpnService.Builder builder) {
        final PackageManager packageManager = context.getPackageManager();

        final ApplicationInfo self = context.getApplicationInfo();
        final Set<String> excludedApps = PreferenceHelper.getVpnExcludedApps(context);
        final String vpnExcludedSystemApps = PreferenceHelper.getVpnExcludedSystemApps(context);
        final Set<String> webBrowserPackageName = vpnExcludedSystemApps.equals("allExceptBrowsers")
                ? getWebBrowserPackageName(packageManager)
                : emptySet();

        final List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(0);
        for (final ApplicationInfo applicationInfo : installedApplications) {
            boolean excluded = false;
            // Skip itself
            if (applicationInfo.packageName.equals(self.packageName)) continue;

            // Check system app
            if ((applicationInfo.flags & FLAG_SYSTEM) != 0) excluded = vpnExcludedSystemApps.equals("all") ||
                    (vpnExcludedSystemApps.equals("allExceptBrowsers") && !webBrowserPackageName.contains(applicationInfo.packageName));
                // Check user excluded applications
            else if (excludedApps.contains(applicationInfo.packageName)) excluded = true;

            if (excluded) {
                try {
                    builder.addDisallowedApplication(applicationInfo.packageName);
                } catch (final PackageManager.NameNotFoundException e) {
                    if (BuildConfig.DEBUG)
                        Log.w("AWAISKING_APP", "Failed to exclude application " + applicationInfo.packageName + " from VPN.", e);
                }
            }
        }
    }

    @NonNull
    private static Set<String> getWebBrowserPackageName(@NonNull final PackageManager packageManager) {
        final Set<String> packageNames = new HashSet<>(0, 0.95f);
        final List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("https://isabrowser.adaway.org/")), 0);
        for (final ResolveInfo resolveInfo : resolveInfoList) {
            if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "adding: " + resolveInfo.loadLabel(packageManager)
                    + "(" + resolveInfo.activityInfo.packageName + ") into <excluding WebBrowsers> packages");
            packageNames.add(resolveInfo.activityInfo.packageName);
        }

        packageNames.add("com.google.android.webview");
        packageNames.add("com.android.htmlviewer");
        packageNames.add("com.google.android.backuptransport");
        packageNames.add("com.google.android.gms");
        packageNames.add("com.google.android.gsf");

        return packageNames;
    }
}

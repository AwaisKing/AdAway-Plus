package org.pro.adaway.vpn;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.net.NetworkCapabilities.TRANSPORT_VPN;
import static org.pro.adaway.broadcast.Command.START;
import static org.pro.adaway.broadcast.Command.STOP;
import static org.pro.adaway.vpn.VpnStatus.STOPPED;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

import androidx.annotation.NonNull;

import org.pro.adaway.helper.PreferenceHelper;

/**
 * This utility class allows controlling (start and stop) the AdAway VPN service.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public final class VpnServiceControls {
    /**
     * Private constructor.
     */
    private VpnServiceControls() {}

    /**
     * Check if the VPN service is currently running.
     *
     * @param context The application context.
     *
     * @return {@code true} if the VPN service is currently running, {@code false} otherwise.
     */
    public static boolean isRunning(final Context context) {
        final boolean networkVpnCapability = checkAnyNetworkVpnCapability(context);
        VpnStatus status = PreferenceHelper.getVpnServiceStatus(context);
        if (status.isStarted() && !networkVpnCapability) {
            status = STOPPED;
            PreferenceHelper.setVpnServiceStatus(context, status);
        }
        return status.isStarted();
    }

    /**
     * Check if the VPN service is started.
     *
     * @param context The application context.
     *
     * @return {@code true} if the VPN service is started, {@code false} otherwise.
     */
    public static boolean isStarted(final Context context) {
        return PreferenceHelper.getVpnServiceStatus(context).isStarted();
    }

    /**
     * Start the VPN service.
     *
     * @param context The application context.
     *
     * @return {@code true} if the service is started, {@code false} otherwise.
     */
    public static boolean start(final Context context) {
        // Check if VPN is already running
        if (isRunning(context)) return true;
        // Start the VPN service
        final Intent intent = new Intent(context, VpnService.class);
        START.appendToIntent(intent);

        final boolean started = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? context.startForegroundService(intent) != null
                : context.startService(intent) != null;

        if (started) // Start the heartbeat
            VpnServiceHeartbeat.start(context);

        return started;
    }

    /**
     * Stop the VPN service.
     *
     * @param context The application context.
     */
    public static void stop(final Context context) {
        // Stop the heartbeat
        VpnServiceHeartbeat.stop(context);
        // Stop the service
        final Intent intent = new Intent(context, VpnService.class);
        STOP.appendToIntent(intent);
        context.startService(intent);
    }

    private static boolean checkAnyNetworkVpnCapability(@NonNull final Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        for (final Network network : connectivityManager.getAllNetworks()) {
            final NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            if (networkCapabilities != null && networkCapabilities.hasTransport(TRANSPORT_VPN))
                return true;
        }
        return false;
    }
}

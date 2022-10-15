package org.pro.adaway.vpn.worker;

import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.vpn.VpnServiceControls;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class monitors the VPN network interface is still up while the VPN is running.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class VpnConnectionMonitor {
    private static final int CONNECTION_CHECK_DELAY_MS = 10_000;
    private static final Pattern TUNNEL_PATTERN = Pattern.compile("tun([0-9]+)");
    /**
     * The application context.
     */
    private final Context context;
    /**
     * Whether the monitor is running (<code>true</code> if running, <code>false</code> if stopped).
     */
    private final AtomicBoolean running;
    /**
     * The network interface to monitor (<code>null</code> if not initialized).
     */
    private NetworkInterface networkInterface;

    /**
     * Constructor.
     *
     * @param context The application context.
     */
    VpnConnectionMonitor(final Context context) {
        this.context = context;
        this.running = new AtomicBoolean(true);
        this.networkInterface = null;
    }

    @NonNull
    private static NetworkInterface findVpnNetworkInterface() {
        try {
            NetworkInterface vpnNetworkInterface = null;
            final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                final NetworkInterface networkInterface = networkInterfaces.nextElement();
                vpnNetworkInterface = pickLastVpnNetworkInterface(vpnNetworkInterface, networkInterface);
            }
            if (vpnNetworkInterface == null) throw new IllegalStateException("Failed to find a network interface.");
            return vpnNetworkInterface;
        } catch (final SocketException e) {
            throw new IllegalStateException("Failed to find VPN network interface.", e);
        }
    }

    private static NetworkInterface pickLastVpnNetworkInterface(final NetworkInterface current,  @NonNull final NetworkInterface other) {
        // Ensure other is a tunnel interface, otherwise picks current
        final Matcher otherMatcher = TUNNEL_PATTERN.matcher(other.getName());
        if (!otherMatcher.matches()) return current;
        // If other is a tunnel interface and no current interface, picks other.
        if (current == null) return other;
        // Ensure current is still a tunnel interface (it happens to change), otherwise picks other
        final Matcher currentMatcher = TUNNEL_PATTERN.matcher(current.getName());
        if (!currentMatcher.matches()) {
            if (BuildConfig.DEBUG)
                Log.e("AWAISKING_APP", "Current interface " + current.getName() + " is no more a tunnel interface.");
            return other;
        }
        // Compare current and ot then pick the last one
        final int currentTunnelNumber = parseInt(requireNonNull(currentMatcher.group(1)));
        final int otherTunnelNumber = parseInt(requireNonNull(otherMatcher.group(1)));
        return otherTunnelNumber > currentTunnelNumber ? other : current;
    }

    /**
     * Initialize the monitor once the VPN connection is up.
     */
    void initialize() {
        if (BuildConfig.DEBUG)
            Log.d("AWAISKING_APP", "Initializing connection monitor…");
        this.networkInterface = findVpnNetworkInterface();
        if (BuildConfig.DEBUG)
            Log.i("AWAISKING_APP", "Connection monitor initialized to watch interface " + this.networkInterface.getName());
    }

    /**
     * Monitor the VPN network interface is still up while the VPN is running.
     */
    void monitor() {
        try {
            while (this.running.get()) {
                if (this.networkInterface != null && !this.networkInterface.isUp()) {
                    stop();
                    if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "VPN network interface "
                            + (this.networkInterface == null ? "unset" : this.networkInterface.getName())
                            + " is down. Starting VPN service…");
                    VpnServiceControls.start(this.context);
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(CONNECTION_CHECK_DELAY_MS);
                } catch (final InterruptedException e) {
                    if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Stop monitoring.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (final SocketException e) {
            if (BuildConfig.DEBUG) Log.w("AWAISKING_APP", "Failed to test VPN network interface "
                    + this.networkInterface.getName() + ". Starting VPN service…", e);
            VpnServiceControls.start(this.context);
        }
    }

    /**
     * Reset the connection monitor if the VPN connection changed.
     */
    void reset() {
        this.networkInterface = null;
    }

    /**
     * Stop the monitor.
     */
    void stop() {
        this.running.set(false);
    }
}

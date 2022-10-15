package org.pro.adaway.vpn;

import androidx.annotation.StringRes;

import org.pro.adaway.R;

public enum VpnStatus {
    STARTING(10, R.string.vpn_notification_starting),
    RUNNING(11, R.string.vpn_notification_running),
    STOPPING(20, R.string.vpn_notification_stopping),
    WAITING_FOR_NETWORK(21, R.string.vpn_notification_waiting_for_net),
    RECONNECTING(22, R.string.vpn_notification_reconnecting),
    RECONNECTING_NETWORK_ERROR(23, R.string.vpn_notification_reconnecting_error),
    STOPPED(0, R.string.vpn_notification_stopped);

    private final int code;

    @StringRes
    private final int textResource;

    VpnStatus(final int code, @StringRes final int textResource) {
        this.code = code;
        this.textResource = textResource;
    }

    public static VpnStatus fromCode(final int code) {
        for (final VpnStatus vpnStatus : VpnStatus.values())
            if (vpnStatus.code == code)
                return vpnStatus;
        return STOPPED;
    }

    @StringRes
    public int getTextResource() {
        return this.textResource;
    }

    public int toCode() {
        return this.code;
    }

    public boolean isStarted() {
        return this != STOPPED;
    }
}

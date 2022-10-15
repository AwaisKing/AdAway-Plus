/*
 * Derived from dns66:
 * Copyright (C) 2016-2019 Julian Andres Klode <jak@jak-linux.org>
 *
 * Derived from AdBuster:
 * Copyright (C) 2016 Daniel Brodie <dbrodie@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Contributions shall also be provided under any later versions of the
 * GPL.
 */
package org.pro.adaway.vpn;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static org.pro.adaway.broadcast.Command.START;
import static org.pro.adaway.broadcast.Command.STOP;
import static org.pro.adaway.broadcast.CommandReceiver.SEND_COMMAND_ACTION;
import static org.pro.adaway.helper.NotificationHelper.VPN_RESUME_SERVICE_NOTIFICATION_ID;
import static org.pro.adaway.helper.NotificationHelper.VPN_RUNNING_SERVICE_NOTIFICATION_ID;
import static org.pro.adaway.helper.NotificationHelper.VPN_SERVICE_NOTIFICATION_CHANNEL;
import static org.pro.adaway.vpn.VpnService.NetworkType.CELLULAR;
import static org.pro.adaway.vpn.VpnService.NetworkType.WIFI;
import static org.pro.adaway.vpn.VpnStatus.RECONNECTING;
import static org.pro.adaway.vpn.VpnStatus.RUNNING;
import static org.pro.adaway.vpn.VpnStatus.STARTING;
import static org.pro.adaway.vpn.VpnStatus.STOPPED;
import static org.pro.adaway.vpn.VpnStatus.WAITING_FOR_NETWORK;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.R;
import org.pro.adaway.broadcast.Command;
import org.pro.adaway.broadcast.CommandReceiver;
import org.pro.adaway.helper.PreferenceHelper;
import org.pro.adaway.ui.home.HomeActivity;
import org.pro.adaway.vpn.worker.VpnWorker;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This class is the VPN platform service implementation.
 * <p>
 * it is in charge of:
 * <ul>
 * <li>Accepting service commands,</li>
 * <li>Starting / stopping the {@link VpnWorker} thread,</li>
 * <li>Publishing notifications and intent about the VPN state,</li>
 * <li>Reacting to network connectivity changes.</li>
 * </ul>
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class VpnService extends android.net.VpnService implements Handler.Callback {
    public static final String VPN_UPDATE_STATUS_INTENT = "org.jak_linux.dns66.VPN_UPDATE_STATUS";
    public static final String VPN_UPDATE_STATUS_EXTRA = "VPN_STATUS";
    /**
     * Notification intent related.
     */
    private static final int REQUEST_CODE_START = 43;
    private static final int REQUEST_CODE_PAUSE = 42;
    /**
     * Handler related.
     */
    private static final int VPN_STATUS_UPDATE_MESSAGE_TYPE = 0;

    private final MyHandler handler;
    private final VpnWorker vpnWorker;
    private final NetworkTypeCallback wifiNetworkCallback;
    private final NetworkTypeCallback cellularNetworkCallback;
    private final Set<NetworkType> availableNetworkTypes;
    private ConnectivityManager connectivityManager;

    /**
     * Constructor.
     */
    public VpnService() {
        this.handler = new MyHandler(this);
        this.wifiNetworkCallback = new NetworkTypeCallback(WIFI);
        this.cellularNetworkCallback = new NetworkTypeCallback(CELLULAR);
        this.availableNetworkTypes = new HashSet<>(0, 0.95f);
        this.vpnWorker = new VpnWorker(this);
    }

    @Override
    public void onCreate() {
        getConnectivityManager();
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Creating VPN service…");
        registerNetworkCallback();
    }

    @Override
    public int onStartCommand(@Nullable final Intent intent, final int flags, final int startId) {
        getConnectivityManager();
        if (BuildConfig.DEBUG)
            Log.d("AWAISKING_APP", "onStartCommand: " + (intent == null ? "null intent" : intent));
        // Check null intent that happens when system restart the service
        // https://developer.android.com/reference/android/app/Service#START_STICKY
        final Command command = intent == null ? START : Command.readFromIntent(intent);
        switch (command) {
            case START:
                startVpn();
                return START_STICKY;
            case STOP:
                stopVpn();
                return START_NOT_STICKY;
            default:
                if (BuildConfig.DEBUG) Log.w("AWAISKING_APP", "Unknown command: " + command);
                return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Destroying VPN service…");
        unregisterNetworkCallback();
        if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "Destroyed VPN service.");
    }

    public ConnectivityManager getConnectivityManager() {
        if (connectivityManager == null) connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager;
    }

    @Override
    public boolean handleMessage(@NonNull final Message message) {
        if (message.what == VPN_STATUS_UPDATE_MESSAGE_TYPE)
            updateVpnStatus(VpnStatus.fromCode(message.arg1));
        return true;
    }

    /**
     * Notify a of the new VPN status.
     *
     * @param status The new VPN status.
     */
    public void notifyVpnStatus(@NonNull final VpnStatus status) {
        final Message statusMessage = this.handler.obtainMessage(VPN_STATUS_UPDATE_MESSAGE_TYPE, status.toCode(), 0);
        this.handler.sendMessage(statusMessage);
    }

    private void startVpn() {
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Starting VPN service…");
        PreferenceHelper.setVpnServiceStatus(this, RUNNING);
        updateVpnStatus(STARTING);
        this.vpnWorker.start();
        if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "VPN service started.");
    }

    private void stopVpn() {
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Stopping VPN service…");
        PreferenceHelper.setVpnServiceStatus(this, STOPPED);
        this.vpnWorker.stop();
        stopForeground(true);
        stopSelf();
        updateVpnStatus(STOPPED);
        if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "VPN service stopped.");
    }

    private void waitForNetVpn() {
        this.vpnWorker.stop();
        updateVpnStatus(WAITING_FOR_NETWORK);
    }

    private void reconnect() {
        updateVpnStatus(RECONNECTING);
        this.vpnWorker.start();
    }

    private void updateVpnStatus(final VpnStatus status) {
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        final Notification notification = getNotification(status);
        switch (status) {
            case STARTING:
            case RUNNING:
                notificationManager.cancel(VPN_RESUME_SERVICE_NOTIFICATION_ID);
                startForeground(VPN_RUNNING_SERVICE_NOTIFICATION_ID, notification);
                break;
            default:
                notificationManager.notify(VPN_RESUME_SERVICE_NOTIFICATION_ID, notification);
        }

        // TODO BUG - Nobody is listening to this intent
        // TODO BUG - VpnModel can lister to it to update the MainActivity according its current state
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(VPN_UPDATE_STATUS_INTENT).putExtra(VPN_UPDATE_STATUS_EXTRA, status));
    }

    @NonNull
    private Notification getNotification(@NonNull final VpnStatus status) {
        final String title = getString(R.string.vpn_notification_title, getString(status.getTextResource()));

        final Intent intent = new Intent(getApplicationContext(), HomeActivity.class)
                .setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
        final PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? FLAG_IMMUTABLE : FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, VPN_SERVICE_NOTIFICATION_CHANNEL)
                .setPriority(NotificationManagerCompat.IMPORTANCE_LOW)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.logo)
                .setColorized(true)
                .setColor(ResourcesCompat.getColor(getResources(), R.color.notification, getTheme()))
                .setContentTitle(title);
        switch (status) {
            case RUNNING:
                final Intent stopIntent = new Intent(this, CommandReceiver.class).setAction(SEND_COMMAND_ACTION);
                STOP.appendToIntent(stopIntent);
                final PendingIntent stopActionIntent = PendingIntent.getBroadcast(this, REQUEST_CODE_PAUSE, stopIntent,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? FLAG_IMMUTABLE : FLAG_UPDATE_CURRENT);
                builder.addAction(R.drawable.ic_pause_24dp, getString(R.string.vpn_notification_action_pause), stopActionIntent);
                break;
            case STOPPED:
                final Intent startIntent = new Intent(this, CommandReceiver.class).setAction(SEND_COMMAND_ACTION);
                START.appendToIntent(startIntent);
                final PendingIntent startActionIntent = PendingIntent.getBroadcast(this, REQUEST_CODE_START, startIntent,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? FLAG_IMMUTABLE : FLAG_UPDATE_CURRENT);
                builder.addAction(0, getString(R.string.vpn_notification_action_resume), startActionIntent);
                break;
        }
        return builder.build();
    }

    private void registerNetworkCallback() {
        final ConnectivityManager connectivityManager = getConnectivityManager();

        final NetworkRequest wifiNetworkRequest = new NetworkRequest.Builder().addTransportType(TRANSPORT_WIFI).build();
        final NetworkRequest cellularNetworkRequest = new NetworkRequest.Builder().addTransportType(TRANSPORT_CELLULAR).build();

        initializeNetworkTypes(connectivityManager);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connectivityManager.registerNetworkCallback(wifiNetworkRequest, this.wifiNetworkCallback, this.handler);
            connectivityManager.registerNetworkCallback(cellularNetworkRequest, this.cellularNetworkCallback, this.handler);
        } else {
            connectivityManager.registerNetworkCallback(wifiNetworkRequest, this.wifiNetworkCallback);
            connectivityManager.registerNetworkCallback(cellularNetworkRequest, this.cellularNetworkCallback);
        }
    }

    private void unregisterNetworkCallback() {
        final ConnectivityManager connectivityManager = getConnectivityManager();
        connectivityManager.unregisterNetworkCallback(this.wifiNetworkCallback);
        connectivityManager.unregisterNetworkCallback(this.cellularNetworkCallback);
    }

    private void initializeNetworkTypes(final ConnectivityManager connectivityManager) {
        this.availableNetworkTypes.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                final NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (networkCapabilities != null) {
                    if (networkCapabilities.hasTransport(TRANSPORT_WIFI)) this.availableNetworkTypes.add(WIFI);
                    if (networkCapabilities.hasTransport(TRANSPORT_CELLULAR)) this.availableNetworkTypes.add(CELLULAR);
                }
            }
        } else {
            final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting()) {
                final int networkInfoType = activeNetworkInfo.getType();
                if (networkInfoType == ConnectivityManager.TYPE_WIFI) this.availableNetworkTypes.add(WIFI);
                if (networkInfoType == ConnectivityManager.TYPE_MOBILE) this.availableNetworkTypes.add(CELLULAR);
            }
        }

        if (BuildConfig.DEBUG)
            Log.d("AWAISKING_APP", "Initial network types: " + this.availableNetworkTypes);
    }

    private void addNetworkType(final NetworkType type) {
        final boolean noNetwork = this.availableNetworkTypes.isEmpty();
        this.availableNetworkTypes.add(type);
        if (noNetwork) {
            if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Reconnecting VPN on network " + type);
            reconnect();
        }
    }

    private void removeNetworkType(final NetworkType type) {
        this.availableNetworkTypes.remove(type);
        if (!this.availableNetworkTypes.isEmpty()) reconnect();
        else {
            if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Waiting for network…");
            waitForNetVpn();
        }
    }

    /**
     * This class receives network change events to monitor network type available.
     *
     * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
     * @see <a href="https://developer.android.com/training/basics/network-ops/reading-network-state#listening-events">Android Developer Documentation</a>
     */
    private class NetworkTypeCallback extends NetworkCallback {
        private final NetworkType monitoredType;

        NetworkTypeCallback(final NetworkType monitoredType) {
            this.monitoredType = monitoredType;
        }

        @Override
        public void onAvailable(@NonNull final Network network) {
            if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "On available " + this.monitoredType);
            addNetworkType(this.monitoredType);
        }

        @Override
        public void onLost(@NonNull final Network network) {
            if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "On lost " + this.monitoredType);
            removeNetworkType(this.monitoredType);
        }
    }

    enum NetworkType {
        CELLULAR,
        WIFI,
    }

    /**
     * The handler may only keep a weak reference around, otherwise it leaks
     */
    private static class MyHandler extends Handler {
        private final WeakReference<Callback> callback;

        MyHandler(final Callback callback) {
            super(Objects.requireNonNull(Looper.myLooper()));
            this.callback = new WeakReference<>(callback);
        }

        @Override
        public void handleMessage(@NonNull final Message msg) {
            final Callback callback = this.callback.get();
            if (callback != null) callback.handleMessage(msg);
            super.handleMessage(msg);
        }
    }
}

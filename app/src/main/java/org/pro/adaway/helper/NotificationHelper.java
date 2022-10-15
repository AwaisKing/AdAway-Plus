package org.pro.adaway.helper;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.app.PendingIntent.getActivity;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static androidx.core.app.NotificationCompat.PRIORITY_LOW;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;

import org.pro.adaway.R;
import org.pro.adaway.ui.home.HomeActivity;
import org.pro.adaway.ui.update.UpdateActivity;

/**
 * This class is an helper class to deals with notifications.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public final class NotificationHelper {
    /**
     * The notification channel for updates.
     */
    public static final String UPDATE_NOTIFICATION_CHANNEL = "UpdateChannel";
    /**
     * The notification channel for VPN service.
     */
    public static final String VPN_SERVICE_NOTIFICATION_CHANNEL = "VpnServiceChannel";
    /**
     * The update hosts notification identifier.
     */
    private static final int UPDATE_HOSTS_NOTIFICATION_ID = 10;
    /**
     * The update application notification identifier.
     */
    private static final int UPDATE_APP_NOTIFICATION_ID = 11;
    /**
     * The VPN running service notification identifier.
     */
    public static final int VPN_RUNNING_SERVICE_NOTIFICATION_ID = 20;
    /**
     * The VPN resume service notification identifier.
     */
    public static final int VPN_RESUME_SERVICE_NOTIFICATION_ID = 21;

    /**
     * Private constructor.
     */
    private NotificationHelper() {}

    /**
     * Create the application notification channel.
     *
     * @param context The application context.
     */
    public static void createNotificationChannels(@NonNull Context context) {
        // Create update notification channel
        final NotificationChannelCompat updateChannel = new NotificationChannelCompat.Builder(UPDATE_NOTIFICATION_CHANNEL, IMPORTANCE_LOW)
                .setName(context.getString(R.string.notification_update_channel_name))
                .setDescription(context.getString(R.string.notification_update_channel_description))
                .build();

        // Create VPN service notification channel
        final NotificationChannelCompat vpnServiceChannel = new NotificationChannelCompat.Builder(VPN_SERVICE_NOTIFICATION_CHANNEL, IMPORTANCE_LOW)
                .setName(context.getString(R.string.notification_vpn_channel_name))
                .setDescription(context.getString(R.string.notification_vpn_channel_description))
                .build();

        // Register the channels with the system; you can't change the importance or other notification behaviors after this
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.createNotificationChannel(updateChannel);
        notificationManager.createNotificationChannel(vpnServiceChannel);
    }

    /**
     * Show the notification about new hosts update available.
     *
     * @param context The application context.
     */
    public static void showUpdateHostsNotification(@NonNull Context context) {
        // Get notification manager
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Build notification
        int color = ResourcesCompat.getColor(context.getResources(), R.color.notification, context.getTheme());
        String title = context.getString(R.string.notification_update_host_available_title);
        String text = context.getString(R.string.notification_update_host_available_text);
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = getActivity(context, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? FLAG_IMMUTABLE : FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, UPDATE_NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.logo)
                .setColorized(true)
                .setColor(color)
                .setShowWhen(false)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setPriority(PRIORITY_LOW)
                .setAutoCancel(true);
        // Notify the built notification
        notificationManager.notify(UPDATE_HOSTS_NOTIFICATION_ID, builder.build());
    }

    /**
     * Show the notification about new application update available.
     *
     * @param context The application context.
     */
    public static void showUpdateApplicationNotification(@NonNull Context context) {
        // Get notification manager
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Build notification
        int color = ResourcesCompat.getColor(context.getResources(), R.color.notification, context.getTheme());
        String title = context.getString(R.string.notification_update_app_available_title);
        String text = context.getString(R.string.notification_update_app_available_text);
        Intent intent = new Intent(context, UpdateActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = getActivity(context, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? FLAG_IMMUTABLE : FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, UPDATE_NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.logo)
                .setColorized(true)
                .setColor(color)
                .setShowWhen(false)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setPriority(PRIORITY_LOW)
                .setAutoCancel(true);
        // Notify the built notification
        notificationManager.notify(UPDATE_HOSTS_NOTIFICATION_ID, builder.build());
    }

    /**
     * Hide the notification about new hosts update available.
     *
     * @param context The application context.
     */
    public static void clearUpdateNotifications(@NonNull Context context) {
        // Get notification manager
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Cancel the notification
        notificationManager.cancel(UPDATE_HOSTS_NOTIFICATION_ID);
        notificationManager.cancel(UPDATE_APP_NOTIFICATION_ID);
    }
}

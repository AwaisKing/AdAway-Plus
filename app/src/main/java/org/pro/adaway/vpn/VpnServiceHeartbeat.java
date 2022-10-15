package org.pro.adaway.vpn;

import static android.content.Context.ACTIVITY_SERVICE;
import static androidx.work.ExistingPeriodicWorkPolicy.KEEP;
import static androidx.work.ListenableWorker.Result.success;
import static java.lang.Integer.MAX_VALUE;
import static java.util.concurrent.TimeUnit.MINUTES;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.pro.adaway.BuildConfig;

/**
 * This class is a worker to monitor the {@link VpnService} is still running.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class VpnServiceHeartbeat extends Worker {
    /**
     * The VPN service heartbeat unique worker name.
     */
    private static final String WORK_NAME = "vpnHeartbeat";

    /**
     * Constructor.
     *
     * @param context      The application context.
     * @param workerParams The worker parameters.
     */
    public VpnServiceHeartbeat(@NonNull final Context context, @NonNull final WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final Context context = getApplicationContext();
        if (VpnServiceControls.isStarted(context) && !isVpnServiceRunning()) {
            if (BuildConfig.DEBUG)
                Log.d("AWAISKING_APP", "VPN service is not running. Starting VPN service…");
            VpnServiceControls.start(context);
            if (BuildConfig.DEBUG)
                Log.i("AWAISKING_APP", "VPN service started.");
        }
        return success();
    }

    // TODO Use VpnServiceControls.isVpnServiceRunning instead?
    private boolean isVpnServiceRunning() {
        final String serviceName = VpnService.class.getName();
        final ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE);
        // Deprecated as it only return application service. It is fine for this use case.
        for (final ActivityManager.RunningServiceInfo service : manager.getRunningServices(MAX_VALUE))
            if (serviceName.equals(service.service.getClassName()))
                return true;
        return false;
    }

    /**
     * Start the VPN service monitor.
     *
     * @param context The application context.
     */
    public static void start(final Context context) {
        final PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(VpnServiceHeartbeat.class, 15, MINUTES)
                .addTag("VPN-heartbeat")
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, KEEP, workRequest);
    }

    /**
     * Stop the VPN service monitor.
     *
     * @param context The application context.
     */
    public static void stop(final Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }
}

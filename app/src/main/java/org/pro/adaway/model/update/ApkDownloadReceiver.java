package org.pro.adaway.model.update;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import static android.content.Intent.ACTION_INSTALL_PACKAGE;

import org.pro.adaway.BuildConfig;

/**
 * This class is a {@link BroadcastReceiver} to install downloaded application updates.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class ApkDownloadReceiver extends BroadcastReceiver {
    private final long downloadId;

    public ApkDownloadReceiver(long downloadId) {
        this.downloadId = downloadId;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Fetching the download id received with the broadcast
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        // Checking if the received broadcast is for our enqueued download by matching download id
        if (this.downloadId == id) {
            DownloadManager downloadManager;
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M)
                downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            else
                downloadManager = context.getSystemService(DownloadManager.class);
            Uri apkUri = downloadManager.getUriForDownloadedFile(id);
            if (apkUri == null) {
                if (BuildConfig.DEBUG)
                    Log.w("AWAISKING_APP", "Failed to download id: " + id);
            } else {
                installApk(context, apkUri);
            }
        }
    }

    private void installApk(Context context, Uri apkUri) {
        Intent install = new Intent(ACTION_INSTALL_PACKAGE);
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        install.setData(apkUri);
        context.startActivity(install);
    }
}

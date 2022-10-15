package org.pro.adaway.model.update;

import android.app.DownloadManager;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.R;
import org.pro.adaway.helper.PreferenceHelper;
import org.json.JSONException;

import java.io.IOException;
import java.util.Objects;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE;
import static android.os.Build.VERSION.SDK_INT;
import static org.pro.adaway.model.update.UpdateStore.getApkStore;

/**
 * This class is the model in charge of updating the application.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class UpdateModel {
    private static final String MANIFEST_URL = "https://awaisome.com/adaway/manifest.json";
    private final Context context;
    private final OkHttpClient client;
    private final MutableLiveData<Manifest> manifest;
    private ApkDownloadReceiver receiver;

    /**
     * Constructor.
     *
     * @param context The application context.
     */
    public UpdateModel(final Context context) {
        this.context = context;
        this.manifest = new MutableLiveData<>();
        this.client = buildHttpClient();
        ApkUpdateService.syncPreferences(context);
    }

    /**
     * Get the current version code.
     *
     * @return The current version code.
     */
    public int getVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    /**
     * Get the current version name.
     *
     * @return The current version name.
     */
    public String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Get the last version manifest.
     *
     * @return The last version manifest.
     */
    public LiveData<Manifest> getManifest() {
        return this.manifest;
    }

    /**
     * Get the application update store.
     *
     * @return The application update store.
     */
    public UpdateStore getStore() {
        return getApkStore(this.context);
    }

    /**
     * Get the application update channel.
     *
     * @return The application update channel.
     */
    public String getChannel() {
        return PreferenceHelper.getIncludeBetaReleases(this.context) ? "beta" : "stable";
    }

    /**
     * Check if there is an update available.
     */
    public void checkForUpdate() {
        final Manifest manifest = downloadManifest();
        // Notify update
        if (manifest != null) this.manifest.postValue(manifest);
    }

    @NonNull
    private OkHttpClient buildHttpClient() {
        return new OkHttpClient.Builder().build();
    }

    @Nullable
    private Manifest downloadManifest() {
        final HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse(MANIFEST_URL))
                .newBuilder()
                .addQueryParameter("versionCode", Integer.toString(BuildConfig.VERSION_CODE))
                .addQueryParameter("sdkCode", Integer.toString(SDK_INT))
                .addQueryParameter("channel", getChannel())
                .addQueryParameter("store", getStore().getName())
                .build();
        final Request request = new Request.Builder()
                .url(httpUrl)
                .build();
        try (final Response execute = this.client.newCall(request).execute(); final ResponseBody body = execute.body()) {
            if (execute.isSuccessful()) return new Manifest(Objects.requireNonNull(body).string());
        } catch (final IOException | JSONException exception) {
            if (BuildConfig.DEBUG)
                Log.e("AWAISKING_APP", "Unable to download manifest.", exception);
        }
        // Return failed
        return null;
    }

    /**
     * Update the application to the latest version.
     *
     * @return The download identifier ({@code -1} if download was not started).
     */
    public long update() {
        // Check manifest
        final Manifest manifest = this.manifest.getValue();
        if (manifest == null) return -1;
        // Check previous broadcast receiver
        if (this.receiver != null) this.context.unregisterReceiver(this.receiver);
        // Queue download
        final long downloadId = download(manifest);
        // Register new broadcast receiver
        this.receiver = new ApkDownloadReceiver(downloadId);
        this.context.registerReceiver(this.receiver, new IntentFilter(ACTION_DOWNLOAD_COMPLETE));
        // Return download identifier
        return downloadId;
    }

    private long download(final Manifest manifest) {
        if (BuildConfig.DEBUG)
            Log.i("AWAISKING_APP", "Downloading " + manifest.version + ".");
        final Uri uri = Uri.parse(manifest.apkUrl);
        final DownloadManager.Request request = new DownloadManager.Request(uri)
                .setTitle("AdAway " + manifest.version)
                .setDescription(this.context.getString(R.string.update_notification_description));
        final DownloadManager downloadManager;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            downloadManager = this.context.getSystemService(DownloadManager.class);
        else
            downloadManager = (DownloadManager) this.context.getSystemService(Context.DOWNLOAD_SERVICE);
        return downloadManager.enqueue(request);
    }
}

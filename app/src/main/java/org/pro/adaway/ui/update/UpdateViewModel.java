package org.pro.adaway.ui.update;

import static android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR;
import static android.app.DownloadManager.COLUMN_STATUS;
import static android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES;

import android.app.Application;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.pro.adaway.AdAwayApplication;
import org.pro.adaway.BuildConfig;
import org.pro.adaway.model.update.Manifest;
import org.pro.adaway.model.update.UpdateModel;
import org.pro.adaway.util.AppExecutors;

import java.util.concurrent.Executor;

/**
 * This class is an {@link AndroidViewModel} for the {@link UpdateActivity} cards.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class UpdateViewModel extends AndroidViewModel {
    private static final Executor NETWORK_IO = AppExecutors.getInstance().networkIO();
    private final MutableLiveData<DownloadStatus> downloadProgress;
    private final UpdateModel updateModel;

    public UpdateViewModel(@NonNull final Application application) {
        super(application);
        this.updateModel = ((AdAwayApplication) application).getUpdateModel();
        this.downloadProgress = new MutableLiveData<>();
    }

    public LiveData<Manifest> getAppManifest() {
        return this.updateModel.getManifest();
    }

    public void update() {
        final long downloadId = this.updateModel.update();
        NETWORK_IO.execute(() -> trackProgress(downloadId));
    }

    public MutableLiveData<DownloadStatus> getDownloadProgress() {
        return this.downloadProgress;
    }

    private void trackProgress(final long downloadId) {
        final Application application = getApplication();
        final DownloadManager downloadManager;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            downloadManager = application.getSystemService(DownloadManager.class);
        else
            downloadManager = (DownloadManager) application.getSystemService(Context.DOWNLOAD_SERVICE);

        final DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        boolean finishDownload = false;
        long total = 0;
        while (!finishDownload) {
            // Add wait before querying download manager
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                if (BuildConfig.DEBUG)
                    Log.w("AWAISKING_APP", "Failed to wait before querying download manager.", e);
                Thread.currentThread().interrupt();
            }
            // Query download manager
            try (final Cursor cursor = downloadManager.query(query)) {
                if (!cursor.moveToFirst()) {
                    if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Download item was not found");
                    continue;
                }
                // Check download status
                final int statusColumnIndex = cursor.getColumnIndex(COLUMN_STATUS);
                final int status = cursor.getInt(statusColumnIndex);
                switch (status) {
                    case DownloadManager.STATUS_FAILED:
                        finishDownload = true;
                        this.downloadProgress.postValue(null);
                        break;
                    case DownloadManager.STATUS_RUNNING:
                        final int totalSizeColumnIndex = cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES);
                        total = cursor.getLong(totalSizeColumnIndex);
                        if (total > 0) {
                            final int bytesDownloadedColumnIndex = cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR);
                            final long downloaded = cursor.getLong(bytesDownloadedColumnIndex);
                            this.downloadProgress.postValue(new DownloadStatus(downloaded, total));
                        }
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        this.downloadProgress.postValue(new DownloadStatus(total, total));
                        finishDownload = true;
                        break;
                }
            }
        }
    }
}
package org.pro.adaway.model.backup;

import static org.pro.adaway.db.entity.ListType.ALLOWED;
import static org.pro.adaway.db.entity.ListType.BLOCKED;
import static org.pro.adaway.db.entity.ListType.REDIRECTED;
import static org.pro.adaway.model.backup.BackupFormat.ALLOWED_KEY;
import static org.pro.adaway.model.backup.BackupFormat.BLOCKED_KEY;
import static org.pro.adaway.model.backup.BackupFormat.REDIRECTED_KEY;
import static org.pro.adaway.model.backup.BackupFormat.SOURCES_KEY;
import static org.pro.adaway.model.backup.BackupFormat.hostToJson;
import static org.pro.adaway.model.backup.BackupFormat.sourceToJson;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pro.adaway.BuildConfig;
import org.pro.adaway.R;
import org.pro.adaway.db.AppDatabase;
import org.pro.adaway.db.dao.HostListItemDao;
import org.pro.adaway.db.dao.HostsSourceDao;
import org.pro.adaway.db.entity.HostListItem;
import org.pro.adaway.db.entity.HostsSource;
import org.pro.adaway.db.entity.ListType;
import org.pro.adaway.util.AppExecutors;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import awaisome.compat.StringInputStream;

/**
 * This class is a helper class to export user lists and hosts sources to a backup file.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class BackupExporter {
    private static final Executor DISK_IO_EXECUTOR = AppExecutors.getInstance().diskIO();
    private static final Executor MAIN_THREAD_EXECUTOR = AppExecutors.getInstance().mainThread();

    private BackupExporter() {}

    /**
     * Export all user lists and hosts sources to a backup file on the external storage.
     *
     * @param context The application context.
     */
    public static void exportToBackup(final Context context, final Uri backupUri) {
        DISK_IO_EXECUTOR.execute(() -> {
            boolean imported = true;
            try {
                exportBackup(context, backupUri);
            } catch (final IOException e) {
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "Failed to import backup.", e);
                imported = false;
            }
            final boolean successful = imported;
            final String fileName = getFileNameFromUri(backupUri);
            MAIN_THREAD_EXECUTOR.execute(() -> notifyExportEnd(context, successful, fileName));
        });
    }

    @NonNull
    private static String getFileNameFromUri(@NonNull final Uri backupUri) {
        final String path = backupUri.getPath();
        return TextUtils.isEmpty(path) ? "" : new File(path).getName();
    }

    @UiThread
    private static void notifyExportEnd(@NonNull final Context context, final boolean successful, final String backupUri) {
        final String exportStr = context.getString(successful ? R.string.export_success : R.string.export_failed, backupUri);
        Toast.makeText(context, exportStr, Toast.LENGTH_LONG).show();
    }

    static void exportBackup(final Context context, final Uri backupUri) throws IOException {
        // Open writer on the export file
        try (final OutputStream outputStream = context.getContentResolver().openOutputStream(backupUri)) {
            final JSONObject backup = makeBackup(context);
            try (final StringInputStream inputStream = new StringInputStream(backup.toString(4))) {
                final byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, read);
            }
        } catch (final JSONException e) {
            throw new IOException("Failed to generate backup.", e);
        } catch (final IOException e) {
            throw new IOException("Could not write file.", e);
        }
    }

    @NonNull
    private static JSONObject makeBackup(final Context context) throws JSONException {
        final AppDatabase database = AppDatabase.getInstance(context);
        final HostsSourceDao hostsSourceDao = database.hostsSourceDao();
        final HostListItemDao hostListItemDao = database.hostsListItemDao();

        final List<HostListItem> userHosts = hostListItemDao.getUserList();

        final List<HostListItem> blockedHosts = new ArrayList<>(0);
        final List<HostListItem> allowedHosts = new ArrayList<>(0);
        final List<HostListItem> redirectedHosts = new ArrayList<>(0);

        for (final HostListItem userHost : userHosts) {
            final ListType hostType = userHost.getType();
            if (hostType == BLOCKED) blockedHosts.add(userHost);
            else if (hostType == ALLOWED) allowedHosts.add(userHost);
            else if (hostType == REDIRECTED) redirectedHosts.add(userHost);
        }

        return new JSONObject()
                .put(SOURCES_KEY, buildSourcesBackup(hostsSourceDao.getAll()))
                .put(BLOCKED_KEY, buildListBackup(blockedHosts))
                .put(ALLOWED_KEY, buildListBackup(allowedHosts))
                .put(REDIRECTED_KEY, buildListBackup(redirectedHosts));
    }

    @NonNull
    private static JSONArray buildSourcesBackup(@NonNull final List<HostsSource> sources) throws JSONException {
        final JSONArray sourceArray = new JSONArray();
        for (final HostsSource source : sources) sourceArray.put(sourceToJson(source));
        return sourceArray;
    }

    @NonNull
    private static JSONArray buildListBackup(@NonNull final List<HostListItem> hosts) throws JSONException {
        final JSONArray listArray = new JSONArray();
        for (final HostListItem host : hosts) listArray.put(hostToJson(host));
        return listArray;
    }
}

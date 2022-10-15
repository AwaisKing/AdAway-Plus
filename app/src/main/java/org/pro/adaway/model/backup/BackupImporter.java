package org.pro.adaway.model.backup;

import static org.pro.adaway.db.entity.ListType.ALLOWED;
import static org.pro.adaway.db.entity.ListType.BLOCKED;
import static org.pro.adaway.db.entity.ListType.REDIRECTED;
import static org.pro.adaway.model.backup.BackupFormat.ALLOWED_KEY;
import static org.pro.adaway.model.backup.BackupFormat.BLOCKED_KEY;
import static org.pro.adaway.model.backup.BackupFormat.REDIRECTED_KEY;
import static org.pro.adaway.model.backup.BackupFormat.SOURCES_KEY;
import static org.pro.adaway.model.backup.BackupFormat.hostFromJson;
import static org.pro.adaway.model.backup.BackupFormat.sourceFromJson;

import android.content.Context;
import android.net.Uri;
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
import org.pro.adaway.db.entity.ListType;
import org.pro.adaway.util.AppExecutors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

/**
 * This class is a helper class to import user lists and hosts sources to a backup file.<br>
 * Importing a file source will no restore read access from Storage Access Framework.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public final class BackupImporter {
    private BackupImporter() {}

    private static final Executor DISK_IO_EXECUTOR = AppExecutors.getInstance().diskIO();
    private static final Executor MAIN_THREAD_EXECUTOR = AppExecutors.getInstance().mainThread();

    /**
     * Import a backup file.
     *
     * @param context   The application context.
     * @param backupUri The URI of a backup file.
     */
    @UiThread
    public static void importFromBackup(final Context context, final Uri backupUri) {
        DISK_IO_EXECUTOR.execute(() -> {
            boolean imported = true;
            try {
                importBackup(context, backupUri);
            } catch (final IOException e) {
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "Failed to import backup.", e);
                imported = false;
            }
            final boolean successful = imported;
            MAIN_THREAD_EXECUTOR.execute(() -> notifyImportEnd(context, successful));
        });
    }

    @UiThread
    private static void notifyImportEnd(@NonNull final Context context, final boolean successful) {
        final String importStr = context.getString(successful ? R.string.import_success : R.string.import_failed);
        Toast.makeText(context, importStr, Toast.LENGTH_LONG).show();
    }

    static void importBackup(final Context context, final Uri backupUri) throws IOException {
        try (final InputStream inputStream = context.getContentResolver().openInputStream(backupUri)) {
            final StringBuilder contentBuilder = new StringBuilder();
            int read;
            while ((read = inputStream.read()) != -1)
                contentBuilder.append((byte) read);
            importBackup(context, new JSONObject(contentBuilder.toString()));
        } catch (final JSONException exception) {
            throw new IOException("Failed to parse backup file.", exception);
        } catch (final FileNotFoundException exception) {
            throw new IOException("Failed to find backup file.", exception);
        } catch (final IOException exception) {
            throw new IOException("Failed to read backup file.", exception);
        }
    }

    private static void importBackup(final Context context, @NonNull final JSONObject backupObject) throws JSONException {
        final AppDatabase database = AppDatabase.getInstance(context);
        final HostsSourceDao hostsSourceDao = database.hostsSourceDao();
        final HostListItemDao hostListItemDao = database.hostsListItemDao();

        importSourceBackup(hostsSourceDao, backupObject.getJSONArray(SOURCES_KEY));
        importListBackup(hostListItemDao, BLOCKED, backupObject.getJSONArray(BLOCKED_KEY));
        importListBackup(hostListItemDao, ALLOWED, backupObject.getJSONArray(ALLOWED_KEY));
        importListBackup(hostListItemDao, REDIRECTED, backupObject.getJSONArray(REDIRECTED_KEY));
    }

    private static void importSourceBackup(final HostsSourceDao hostsSourceDao, @NonNull final JSONArray sources) throws JSONException {
        for (int index = 0; index < sources.length(); index++)
            hostsSourceDao.insert(sourceFromJson(sources.getJSONObject(index)));
    }

    private static void importListBackup(final HostListItemDao hostListItemDao, final ListType type, @NonNull final JSONArray hosts) throws JSONException {
        for (int index = 0; index < hosts.length(); index++) {
            final JSONObject hostObject = hosts.getJSONObject(index);
            final HostListItem host = hostFromJson(hostObject);
            host.setType(type);
            final Integer id = hostListItemDao.getHostId(host.getHost());
            if (id == null) hostListItemDao.insert(host);
            else {
                host.setId(id);
                hostListItemDao.update(host);
            }
        }
    }
}

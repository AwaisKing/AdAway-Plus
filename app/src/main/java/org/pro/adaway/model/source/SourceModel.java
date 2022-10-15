package org.pro.adaway.model.source;

import static android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.R;
import org.pro.adaway.db.AppDatabase;
import org.pro.adaway.db.converter.ZonedDateTimeConverter;
import org.pro.adaway.db.dao.HostEntryDao;
import org.pro.adaway.db.dao.HostListItemDao;
import org.pro.adaway.db.dao.HostsSourceDao;
import org.pro.adaway.db.entity.HostEntry;
import org.pro.adaway.db.entity.HostListItem;
import org.pro.adaway.db.entity.HostsSource;
import org.pro.adaway.db.entity.SourceType;
import org.pro.adaway.model.error.HostError;
import org.pro.adaway.model.error.HostErrorException;
import org.pro.adaway.model.git.GitHostsSource;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.format.FormatStyle;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is the model to represent hosts source management.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class SourceModel {
    /**
     * The HTTP client cache size (100Mo).
     */
    private static final long CACHE_SIZE = 100L * 1024L * 1024L;
    private static final String LAST_MODIFIED_HEADER = "Last-Modified";
    private static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    private static final String IF_MODIFIED_SINCE_HEADER = "If-Modified-Since";
    private static final String ENTITY_TAG_HEADER = "ETag";
    private static final String WEAK_ENTITY_TAG_PREFIX = "W/";
    /**
     * The application context.
     */
    private final Context context;
    /**
     * The {@link HostsSource} DAO.
     */
    private final HostsSourceDao hostsSourceDao;
    /**
     * The {@link HostListItem} DAO.
     */
    private final HostListItemDao hostListItemDao;
    /**
     * The {@link HostEntry} DAO.
     */
    private final HostEntryDao hostEntryDao;
    /**
     * The update available status.
     */
    private final MutableLiveData<Boolean> updateAvailable;
    /**
     * The model state.
     */
    private final MutableLiveData<String> state;
    /**
     * The HTTP client to download hosts sources ({@code null} until initialized by {@link #getHttpClient()}).
     */
    private OkHttpClient cachedHttpClient;
    /**
     * Date Time formatter for parsing {@link ZonedDateTime} times
     */
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    /**
     * Constructor.
     *
     * @param context The application context.
     */
    public SourceModel(final Context context) {
        final AppDatabase database = AppDatabase.getInstance(context);
        this.context = context;
        this.hostsSourceDao = database.hostsSourceDao();
        this.hostListItemDao = database.hostsListItemDao();
        this.hostEntryDao = database.hostEntryDao();
        this.state = new MutableLiveData<>("");
        this.updateAvailable = new MutableLiveData<>(false);
        SourceUpdateService.syncPreferences(context);
    }

    /**
     * Get the model state.
     *
     * @return The model state.
     */
    public LiveData<String> getState() {
        return this.state;
    }

    /**
     * Get the update available status.
     *
     * @return {@code true} if source update is available, {@code false} otherwise.
     */
    public LiveData<Boolean> isUpdateAvailable() {
        return this.updateAvailable;
    }

    /**
     * Check if there is update available for hosts sources.
     *
     * @throws HostErrorException If the hosts sources could not be checked.
     */
    public boolean checkForUpdate() throws HostErrorException {
        // Check current connection
        if (isDeviceOffline()) throw new HostErrorException(HostError.NO_CONNECTION);

        // Initialize update status
        boolean updateAvailable = false;

        // Get enabled hosts sources
        final List<HostsSource> sources = this.hostsSourceDao.getEnabled();
        if (sources.isEmpty()) {
            // Return no update as no source
            this.updateAvailable.postValue(false);
            return false;
        }

        // Update state
        setState(R.string.status_check);

        // Check each source
        for (final HostsSource source : sources) {
            // Get URL and lastModified from db
            final ZonedDateTime lastModifiedLocal = source.getLocalModificationDate();
            // Update state
            setState(R.string.status_check_source, source.getLabel());
            // Get hosts source last update
            final ZonedDateTime lastModifiedOnline = getHostsSourceLastUpdate(source);
            // Some help with debug here
            if (BuildConfig.DEBUG) {
                Log.d("AWAISKING_APP", "lastModifiedLocal: " + dateToString(lastModifiedLocal));
                Log.d("AWAISKING_APP", "lastModifiedOnline: " + dateToString(lastModifiedOnline));
            }
            // Save last modified online
            this.hostsSourceDao.updateOnlineModificationDate(source.getId(), lastModifiedOnline);
            // Check if last modified online retrieved
            if (lastModifiedOnline == null) {
                // If not, consider update is available if install is older than a week
                final ZonedDateTime lastWeek = ZonedDateTime.now().minus(1, ChronoUnit.WEEKS);
                if (lastModifiedLocal != null && lastModifiedLocal.isBefore(lastWeek)) {
                    updateAvailable = true;
                }
            } else {
                // Check if source was never installed or installed before the last update
                if (lastModifiedLocal == null || lastModifiedOnline.isAfter(lastModifiedLocal)) {
                    updateAvailable = true;
                }
            }
        }

        // Update statuses
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Update check result: " + updateAvailable);
        setState(updateAvailable ? R.string.status_update_available : R.string.status_no_update_found);
        this.updateAvailable.postValue(updateAvailable);

        // Return update availability
        return updateAvailable;
    }

    /**
     * Format {@link ZonedDateTime} for printing.
     *
     * @param zonedDateTime The date to format.
     *
     * @return The formatted date string.
     */
    @NonNull
    private String dateToString(final ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) return "not defined";
        return zonedDateTime + " (" + zonedDateTime.format(dateTimeFormatter) + ")";
    }

    /**
     * Checks if device is offline.
     *
     * @return returns {@code true} if device is offline, {@code false} otherwise.
     */
    private boolean isDeviceOffline() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        final NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        return netInfo == null || !netInfo.isConnectedOrConnecting();
    }

    /**
     * Get the hosts source last online update.
     *
     * @param source The hosts source to get last online update.
     *
     * @return The last online date, {@code null} if the date could not be retrieved.
     */
    @Nullable
    private ZonedDateTime getHostsSourceLastUpdate(@NonNull final HostsSource source) {
        switch (source.getType()) {
            case URL:
                return getUrlLastUpdate(source);
            case FILE:
                final Uri fileUri = Uri.parse(source.getUrl());
                return getFileLastUpdate(fileUri);
            default:
                return null;
        }
    }

    /**
     * Get the url last online update.
     *
     * @param source The source to get last online update.
     *
     * @return The last online date, {@code null} if the date could not be retrieved.
     */
    @Nullable
    private ZonedDateTime getUrlLastUpdate(@NonNull final HostsSource source) {
        final String url = source.getUrl();
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Checking url last update for source: " + url);

        // Check Git hosting
        if (GitHostsSource.isHostedOnGit(url)) {
            try {
                return GitHostsSource.getSource(url).getLastUpdate();
            } catch (final MalformedURLException e) {
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "Failed to get Git last commit for url " + url, e);
                return null;
            }
        }

        // Default hosting
        final Request request = getRequestFor(source).head().build();
        try (final Response response = getHttpClient().newCall(request).execute()) {
            final String lastModified = response.header(LAST_MODIFIED_HEADER);
            if (lastModified == null) {
                return response.code() == HttpURLConnection.HTTP_NOT_MODIFIED ? source.getOnlineModificationDate() : null;
            }
            return ZonedDateTime.parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (final IOException | DateTimeParseException e) {
            if (BuildConfig.DEBUG) {
                Log.e("AWAISKING_APP", "Exception while fetching last modified date of source " + url, e);
            }
            return null;
        }
    }

    /**
     * Get the file last modified date.
     *
     * @param fileUri The file uri to get last modified date.
     *
     * @return The file last modified date, {@code null} if date could not be retrieved.
     */
    @Nullable
    private ZonedDateTime getFileLastUpdate(final Uri fileUri) {
        final ContentResolver contentResolver = this.context.getContentResolver();
        try (final Cursor cursor = contentResolver.query(fileUri, null, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                if (BuildConfig.DEBUG)
                    Log.w("AWAISKING_APP", "The content resolver could not find " + fileUri);
                return null;
            }

            final int columnIndex = cursor.getColumnIndex(COLUMN_LAST_MODIFIED);
            if (columnIndex == -1) {
                if (BuildConfig.DEBUG)
                    Log.w("AWAISKING_APP", "The content resolver does not support last modified column " + fileUri);
                return null;
            }

            return ZonedDateTimeConverter.fromTimestamp(cursor.getLong(columnIndex));
        } catch (final Exception e) {
            if (BuildConfig.DEBUG)
                Log.e("AWAISKING_APP", "The SAF permission was removed.", e);
            return null;
        }
    }

    /**
     * Retrieve all hosts sources files to copy into a private local file.
     *
     * @throws HostErrorException If the hosts sources could not be downloaded.
     */
    public void retrieveHostsSources() throws HostErrorException {
        // Check connection status
        if (isDeviceOffline()) throw new HostErrorException(HostError.NO_CONNECTION);

        // Update state to downloading
        setState(R.string.status_retrieve);

        // Initialize copy counters
        int numberOfCopies = 0;
        int numberOfFailedCopies = 0;

        // Compute current date in UTC timezone
        final ZonedDateTime now = ZonedDateTime.now();

        // Get each hosts source
        for (final HostsSource source : this.hostsSourceDao.getAll()) {
            final int sourceId = source.getId();

            // Clear disabled source
            if (!source.isEnabled()) {
                this.hostListItemDao.clearSourceHosts(sourceId);
                this.hostsSourceDao.clearProperties(sourceId);
                continue;
            }

            // Get hosts source last update
            ZonedDateTime onlineModificationDate = getHostsSourceLastUpdate(source);
            if (onlineModificationDate == null) onlineModificationDate = now;

            // Check if update available
            ZonedDateTime localModificationDate = source.getLocalModificationDate();
            if (localModificationDate != null && localModificationDate.isAfter(onlineModificationDate)) {
                if (BuildConfig.DEBUG)
                    Log.i("AWAISKING_APP", "Skip source " + source.getLabel() + ": no update.");
                continue;
            }

            // Increment number of copy
            numberOfCopies++;
            try {
                // Check hosts source type
                final SourceType type = source.getType();
                if (type == SourceType.URL)
                    downloadHostSource(source);
                else if (type == SourceType.FILE)
                    readSourceFile(source);
                else if (BuildConfig.DEBUG)
                    Log.w("AWAISKING_APP", "Hosts source type [" + source.getType() + "] is not supported.");

                // Update local and online modification dates to now
                localModificationDate = onlineModificationDate.isAfter(now) ? onlineModificationDate : now;
                this.hostsSourceDao.updateModificationDates(sourceId, localModificationDate, onlineModificationDate);

                // Update size
                this.hostsSourceDao.updateSize(sourceId);
            } catch (final IOException e) {
                if (BuildConfig.DEBUG)
                    Log.w("AWAISKING_APP", "Failed to retrieve host source " + source.getUrl(), e);
                // Increment number of failed copy
                numberOfFailedCopies++;
            }
        }

        // Check if all copies failed
        if (numberOfCopies == numberOfFailedCopies && numberOfCopies != 0)
            throw new HostErrorException(HostError.DOWNLOAD_FAILED);

        // Synchronize hosts entries
        syncHostEntries();

        // Mark no update available
        this.updateAvailable.postValue(false);
    }

    /**
     * Synchronize hosts entries from current source states.
     */
    public void syncHostEntries() {
        setState(R.string.status_sync_database);
        this.hostEntryDao.sync();
    }

    /**
     * Get the HTTP client to download hosts sources.
     *
     * @return The HTTP client to download hosts sources.
     */
    @NonNull
    private OkHttpClient getHttpClient() {
        if (this.cachedHttpClient == null) {
            this.cachedHttpClient = new OkHttpClient.Builder()
                    .cache(new Cache(this.context.getCacheDir(), CACHE_SIZE))
                    .build();
        }
        return this.cachedHttpClient;
    }

    /**
     * Get request builder for an hosts source.
     * All cache data available are filled into the headers.
     *
     * @param source The hosts source to get request builder.
     *
     * @return The hosts source request builder.
     */
    private Request.Builder getRequestFor(@NonNull final HostsSource source) {
        Request.Builder request = new Request.Builder().url(source.getUrl());
        if (source.getEntityTag() != null)
            request = request.header(IF_NONE_MATCH_HEADER, source.getEntityTag());

        if (source.getOnlineModificationDate() != null) {
            final String lastModified = source.getOnlineModificationDate().format(DateTimeFormatter.RFC_1123_DATE_TIME);
            request = request.header(IF_MODIFIED_SINCE_HEADER, lastModified);
        }

        return request;
    }

    /**
     * Download an hosts source file and append it to the database.
     *
     * @param source The hosts source to download.
     *
     * @throws IOException If the hosts source could not be downloaded.
     */
    private void downloadHostSource(@NonNull final HostsSource source) throws IOException {
        // Get hosts file URL
        final String hostsFileUrl = source.getUrl();
        if (BuildConfig.DEBUG)
            Log.i("AWAISKING_APP", "Downloading hosts file: " + hostsFileUrl);

        // Set state to downloading hosts source
        setState(R.string.status_download_source, hostsFileUrl);

        // Create request
        final Request request = getRequestFor(source).build();

        // Request hosts file and open byte stream
        try (final Response response = getHttpClient().newCall(request).execute();
             final InputStream inputStream = Objects.requireNonNull(response.body()).byteStream()) {
            // Skip source parsing if not modified
            if (response.code() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                if (BuildConfig.DEBUG)
                    Log.w("AWAISKING_APP", "Source " + source.getUrl() + " was not updated since last fetch.");
                return;
            }

            // Extract ETag if present
            String entityTag = response.header(ENTITY_TAG_HEADER);
            if (entityTag != null) {
                if (entityTag.startsWith(WEAK_ENTITY_TAG_PREFIX))
                    entityTag = entityTag.substring(WEAK_ENTITY_TAG_PREFIX.length());
                this.hostsSourceDao.updateEntityTag(source.getId(), entityTag);
            }

            // Parse source
            parseSourceInputStream(source, inputStream);
        } catch (final IOException e) {
            throw new IOException("Exception while downloading hosts file from " + hostsFileUrl + ".", e);
        }
    }

    /**
     * Read a hosts source file and append it to the database.
     *
     * @param hostsSource The hosts source to copy.
     *
     * @throws IOException If the hosts source could not be copied.
     */
    private void readSourceFile(@NonNull final HostsSource hostsSource) throws IOException {
        // Get hosts file URI
        final String hostsFileUrl = hostsSource.getUrl();
        if (BuildConfig.DEBUG)
            Log.i("AWAISKING_APP", "Reading hosts source file: " + hostsFileUrl);

        // Set state to reading hosts source
        setState(R.string.status_read_source, hostsFileUrl);

        // Start reading hosts source
        try (final InputStream inputStream = this.context.getContentResolver().openInputStream(Uri.parse(hostsFileUrl))) {
            parseSourceInputStream(hostsSource, inputStream);
        } catch (final IOException e) {
            throw new IOException("Error while reading hosts file from " + hostsFileUrl + ".", e);
        }
    }

    /**
     * Parse a source from its input stream to store it into database.
     *
     * @param hostsSource The host source to parse.
     * @param inputStream The host source reader.
     */
    private void parseSourceInputStream(@NonNull final HostsSource hostsSource, final InputStream inputStream) {
        setState(R.string.status_parse_source, hostsSource.getLabel());
        final long startTime = System.currentTimeMillis();
        new SourceLoader(hostsSource).parse(inputStream, this.hostListItemDao);
        final long endTime = System.currentTimeMillis();
        if (BuildConfig.DEBUG)
            Log.i("AWAISKING_APP", "Parsed " + hostsSource.getUrl() + " in " + (endTime - startTime) / 1000 + "s");
    }

    /**
     * Enable all hosts sources.
     *
     * @return {@code true} if at least one source was updated, {@code false} otherwise.
     */
    public boolean enableAllSources() {
        boolean updated = false;
        for (final HostsSource source : this.hostsSourceDao.getAll()) {
            if (!source.isEnabled()) {
                this.hostsSourceDao.toggleEnabled(source);
                updated = true;
            }
        }
        return updated;
    }

    private void setState(@StringRes final int stateResId, final Object... details) {
        final String state = this.context.getString(stateResId, details);
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Source model state: " + state);
        this.state.postValue(state);
    }
}

package org.pro.adaway.ui.log;

import static org.pro.adaway.db.entity.HostsSource.USER_SOURCE_ID;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.pro.adaway.AdAwayApplication;
import org.pro.adaway.db.AppDatabase;
import org.pro.adaway.db.dao.HostEntryDao;
import org.pro.adaway.db.dao.HostListItemDao;
import org.pro.adaway.db.entity.HostListItem;
import org.pro.adaway.db.entity.ListType;
import org.pro.adaway.model.adblocking.AdBlockMethod;
import org.pro.adaway.model.adblocking.AdBlockModel;
import org.pro.adaway.util.AppExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is an {@link AndroidViewModel} for the {@link LogActivity}.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class LogViewModel extends AndroidViewModel {
    private final AdBlockModel adBlockModel;
    private final HostListItemDao hostListItemDao;
    private final HostEntryDao hostEntryDao;
    private final MutableLiveData<List<LogEntry>> logEntries;
    private final MutableLiveData<Boolean> recording;
    private LogEntrySort sort;

    public LogViewModel(@NonNull final Application application) {
        super(application);
        this.adBlockModel = ((AdAwayApplication) application).getAdBlockModel();
        this.hostListItemDao = AppDatabase.getInstance(application).hostsListItemDao();
        this.hostEntryDao = AppDatabase.getInstance(application).hostEntryDao();
        this.logEntries = new MutableLiveData<>();
        this.recording = new MutableLiveData<>(this.adBlockModel.isRecordingLogs());
        this.sort = LogEntrySort.TOP_LEVEL_DOMAIN;
    }

    public boolean areBlockedRequestsIgnored() {
        return this.adBlockModel.getMethod() == AdBlockMethod.ROOT;
    }

    public LiveData<List<LogEntry>> getLogs() {
        return this.logEntries;
    }

    public void clearLogs() {
        this.adBlockModel.clearLogs();
        this.logEntries.postValue(Collections.emptyList());
    }

    public void updateLogs() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            final List<LogEntry> logItems = new ArrayList<>(0);

            // Get tcpdump logs
            final List<String> logs = this.adBlockModel.getLogs();
            for (final String log : logs) {
                final ListType type = this.hostEntryDao.getTypeOfHost(log);
                logItems.add(new LogEntry(log, type));
            }
            Collections.sort(logItems, this.sort.comparator());

            // Post result
            this.logEntries.postValue(logItems);
        });
    }

    public void toggleSort() {
        this.sort = this.sort == LogEntrySort.ALPHABETICAL ? LogEntrySort.TOP_LEVEL_DOMAIN : LogEntrySort.ALPHABETICAL;
        this.sortDnsRequests(this.sort);
    }

    public LiveData<Boolean> isRecording() {
        return this.recording;
    }

    public void toggleRecording() {
        final boolean recording = !this.adBlockModel.isRecordingLogs();
        this.adBlockModel.setRecordingLogs(recording);
        this.recording.postValue(recording);
    }

    public void addListItem(@NonNull final String host, @NonNull final ListType type, final String redirection) {
        // Create new host list item
        final HostListItem item = new HostListItem();
        item.setType(type);
        item.setHost(host);
        item.setRedirection(redirection);
        item.setEnabled(true);
        item.setSourceId(USER_SOURCE_ID);
        // Insert host list item
        AppExecutors.getInstance().diskIO().execute(() -> this.hostListItemDao.insert(item));
        // Update log entries
        updateLogEntryType(host, type);
    }

    public void removeListItem(@NonNull final String host) {
        // Delete host list item
        AppExecutors.getInstance().diskIO().execute(() -> this.hostListItemDao.deleteUserFromHost(host));
        // Update log entries
        updateLogEntryType(host, null);
    }

    private void updateLogEntryType(@NonNull final String host, final ListType type) {
        // Get current values
        final List<LogEntry> entries = this.logEntries.getValue();
        if (entries == null) return;
        // Update entry type
        final List<LogEntry> updatedEntries = new ArrayList<>();
        for (final LogEntry entry : entries) {
            final LogEntry logEntry = entry.getHost().equals(host) ? new LogEntry(host, type) : entry;
            updatedEntries.add(logEntry);
        }
        // Post new values
        this.logEntries.postValue(updatedEntries);
    }

    private void sortDnsRequests(final LogEntrySort sort) {
        // Save current sort
        this.sort = sort;
        // Apply sort to values
        final List<LogEntry> entries = this.logEntries.getValue();
        if (entries != null) {
            final List<LogEntry> sortedEntries = new ArrayList<>(entries);
            Collections.sort(sortedEntries, this.sort.comparator());
            this.logEntries.postValue(sortedEntries);
        }
        // Notify user
        Toast.makeText(getApplication(), this.sort.getName(), Toast.LENGTH_SHORT).show();
    }
}

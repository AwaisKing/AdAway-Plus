package org.pro.adaway.ui.home;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.pro.adaway.AdAwayApplication;
import org.pro.adaway.BuildConfig;
import org.pro.adaway.db.AppDatabase;
import org.pro.adaway.db.dao.HostListItemDao;
import org.pro.adaway.db.dao.HostsSourceDao;
import org.pro.adaway.model.adblocking.AdBlockModel;
import org.pro.adaway.model.error.HostError;
import org.pro.adaway.model.error.HostErrorException;
import org.pro.adaway.model.source.SourceModel;
import org.pro.adaway.model.update.Manifest;
import org.pro.adaway.model.update.UpdateModel;
import org.pro.adaway.util.AppExecutors;

/**
 * This class is an {@link AndroidViewModel} for the {@link HomeActivity} cards.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class HomeViewModel extends AndroidViewModel {
    private static final AppExecutors EXECUTORS = AppExecutors.getInstance();

    private final SourceModel sourceModel;
    private final AdBlockModel adBlockModel;
    private final UpdateModel updateModel;

    private final HostsSourceDao hostsSourceDao;
    private final HostListItemDao hostListItemDao;

    private final MutableLiveData<Boolean> pending;
    private final MediatorLiveData<String> state;
    private final MutableLiveData<HostError> error;

    public HomeViewModel(@NonNull final Application application) {
        super(application);
        final AdAwayApplication awayApplication = (AdAwayApplication) application;
        this.sourceModel = awayApplication.getSourceModel();
        this.adBlockModel = awayApplication.getAdBlockModel();
        this.updateModel = awayApplication.getUpdateModel();

        final AppDatabase database = AppDatabase.getInstance(application);
        this.hostsSourceDao = database.hostsSourceDao();
        this.hostListItemDao = database.hostsListItemDao();

        this.pending = new MutableLiveData<>(false);
        this.state = new MediatorLiveData<>();
        this.state.addSource(this.sourceModel.getState(), this.state::setValue);
        this.state.addSource(this.adBlockModel.getState(), this.state::setValue);
        this.error = new MutableLiveData<>();
    }

    private static boolean isTrue(@NonNull final LiveData<Boolean> liveData) {
        final Boolean value = liveData.getValue();
        return value != null && value;
    }

    public LiveData<Boolean> isAdBlocked() {
        return this.adBlockModel.isApplied();
    }

    public LiveData<Boolean> isUpdateAvailable() {
        return this.sourceModel.isUpdateAvailable();
    }

    public String getVersionName() {
        return this.updateModel.getVersionName();
    }

    public LiveData<Manifest> getAppManifest() {
        return this.updateModel.getManifest();
    }

    public LiveData<Integer> getBlockedHostCount() {
        return this.hostListItemDao.getBlockedHostCount();
    }

    public LiveData<Integer> getAllowedHostCount() {
        return this.hostListItemDao.getAllowedHostCount();
    }

    public LiveData<Integer> getRedirectHostCount() {
        return this.hostListItemDao.getRedirectHostCount();
    }

    public LiveData<Integer> getUpToDateSourceCount() {
        return this.hostsSourceDao.countUpToDate();
    }

    public LiveData<Integer> getOutdatedSourceCount() {
        return this.hostsSourceDao.countOutdated();
    }

    public LiveData<Boolean> getPending() {
        return this.pending;
    }

    public LiveData<String> getState() {
        return this.state;
    }

    public LiveData<HostError> getError() {
        return this.error;
    }

    public void checkForAppUpdate() {
        EXECUTORS.networkIO().execute(this.updateModel::checkForUpdate);
    }

    public void toggleAdBlocking() {
        if (isTrue(this.pending)) return;
        EXECUTORS.diskIO().execute(() -> {
            try {
                this.pending.postValue(true);
                if (isTrue(this.adBlockModel.isApplied())) this.adBlockModel.revert();
                else this.adBlockModel.apply();
            } catch (final HostErrorException exception) {
                if (BuildConfig.DEBUG) Log.w("AWAISKING_APP", "Failed to toggle ad blocking.", exception);
                this.error.postValue(exception.getError());
            } finally {
                this.pending.postValue(false);
            }
        });
    }

    public void update() {
        if (isTrue(this.pending)) return;
        EXECUTORS.networkIO().execute(() -> {
            try {
                this.pending.postValue(true);
                this.sourceModel.checkForUpdate();
            } catch (final HostErrorException exception) {
                if (BuildConfig.DEBUG) Log.w("AWAISKING_APP", "Failed to update.", exception);
                this.error.postValue(exception.getError());
            } finally {
                this.pending.postValue(false);
            }
        });
    }

    public void sync() {
        if (isTrue(this.pending)) return;
        EXECUTORS.networkIO().execute(() -> {
            try {
                this.pending.postValue(true);
                this.sourceModel.retrieveHostsSources();
                this.adBlockModel.apply();
            } catch (final HostErrorException exception) {
                if (BuildConfig.DEBUG) Log.w("AWAISKING_APP", "Failed to sync.", exception);
                this.error.postValue(exception.getError());
            } finally {
                this.pending.postValue(false);
            }
        });
    }

    public void enableAllSources() {
        EXECUTORS.diskIO().execute(() -> {
            if (this.sourceModel.enableAllSources())
                sync();
        });
    }
}

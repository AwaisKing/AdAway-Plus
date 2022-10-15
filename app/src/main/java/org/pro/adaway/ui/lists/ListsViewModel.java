package org.pro.adaway.ui.lists;

import static org.pro.adaway.db.entity.HostsSource.USER_SOURCE_ID;
import static org.pro.adaway.db.entity.ListType.ALLOWED;
import static org.pro.adaway.db.entity.ListType.BLOCKED;
import static org.pro.adaway.db.entity.ListType.REDIRECTED;
import static org.pro.adaway.ui.lists.ListsFilter.ALL;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import org.pro.adaway.db.AppDatabase;
import org.pro.adaway.db.dao.HostListItemDao;
import org.pro.adaway.db.entity.HostListItem;
import org.pro.adaway.db.entity.ListType;
import org.pro.adaway.ui.lists.type.AbstractListFragment;
import org.pro.adaway.util.AppExecutors;

import java.util.concurrent.Executor;

/**
 * This class is an {@link AndroidViewModel} for the {@link AbstractListFragment} implementations.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class ListsViewModel extends AndroidViewModel {
    private static final Executor EXECUTOR = AppExecutors.getInstance().diskIO();
    private final HostListItemDao hostListItemDao;
    private final MutableLiveData<ListsFilter> filter;
    private final LiveData<PagingData<HostListItem>> blockedListItems;
    private final LiveData<PagingData<HostListItem>> allowedListItems;
    private final LiveData<PagingData<HostListItem>> redirectedListItems;
    private final MutableLiveData<Boolean> modelChanged;

    public ListsViewModel(@NonNull final Application application) {
        super(application);
        this.hostListItemDao = AppDatabase.getInstance(application).hostsListItemDao();
        this.filter = new MutableLiveData<>(ALL);
        final PagingConfig pagingConfig = new PagingConfig(50, 150, true);
        this.blockedListItems = Transformations.switchMap(this.filter, filter -> PagingLiveData.getLiveData(new Pager<>(pagingConfig, () ->
                this.hostListItemDao.loadList(BLOCKED.getValue(), filter.sourcesIncluded, filter.sqlQuery)
        )));
        this.allowedListItems = Transformations.switchMap(this.filter, filter -> PagingLiveData.getLiveData(new Pager<>(pagingConfig, () ->
                this.hostListItemDao.loadList(ALLOWED.getValue(), filter.sourcesIncluded, filter.sqlQuery)
        )));
        this.redirectedListItems = Transformations.switchMap(this.filter, filter -> PagingLiveData.getLiveData(new Pager<>(pagingConfig, () ->
                this.hostListItemDao.loadList(REDIRECTED.getValue(), filter.sourcesIncluded, filter.sqlQuery)
        )));
        this.modelChanged = new MutableLiveData<>(false);
    }

    public LiveData<PagingData<HostListItem>> getBlockedListItems() {
        return this.blockedListItems;
    }

    public LiveData<PagingData<HostListItem>> getAllowedListItems() {
        return this.allowedListItems;
    }

    public LiveData<PagingData<HostListItem>> getRedirectedListItems() {
        return this.redirectedListItems;
    }

    public LiveData<Boolean> getModelChanged() {
        return this.modelChanged;
    }

    public void toggleItemEnabled(@NonNull final HostListItem item) {
        item.setEnabled(!item.isEnabled());
        EXECUTOR.execute(() -> {
            this.hostListItemDao.update(item);
            this.modelChanged.postValue(true);
        });
    }

    public void addListItem(@NonNull final ListType type, @NonNull final String host, final String redirection) {
        final HostListItem item = new HostListItem();
        item.setType(type);
        item.setHost(host);
        item.setRedirection(redirection);
        item.setEnabled(true);
        item.setSourceId(USER_SOURCE_ID);
        EXECUTOR.execute(() -> {
            final Integer id = this.hostListItemDao.getHostId(host);
            if (id == null) this.hostListItemDao.insert(item);
            else {
                item.setId(id);
                this.hostListItemDao.update(item);
            }
            this.modelChanged.postValue(true);
        });
    }

    public void updateListItem(@NonNull final HostListItem item, @NonNull final String host, final String redirection) {
        item.setHost(host);
        item.setRedirection(redirection);
        EXECUTOR.execute(() -> {
            this.hostListItemDao.update(item);
            this.modelChanged.postValue(true);
        });
    }

    public void removeListItem(final HostListItem list) {
        EXECUTOR.execute(() -> {
            this.hostListItemDao.delete(list);
            this.modelChanged.postValue(true);
        });
    }

    public void search(final String query) {
        final ListsFilter currentFilter = getFilter();
        final ListsFilter newFilter = new ListsFilter(currentFilter.sourcesIncluded, query);
        this.filter.setValue(newFilter);
    }

    public boolean isSearching() {
        return !TextUtils.isEmpty(getFilter().query);
    }

    public void clearSearch() {
        final ListsFilter currentFilter = getFilter();
        final ListsFilter newFilter = new ListsFilter(currentFilter.sourcesIncluded, "");
        this.filter.setValue(newFilter);
    }

    public void toggleSources() {
        final ListsFilter currentFilter = getFilter();
        final ListsFilter newFilter = new ListsFilter(!currentFilter.sourcesIncluded, currentFilter.query);
        this.filter.setValue(newFilter);
    }

    private ListsFilter getFilter() {
        final ListsFilter filter = this.filter.getValue();
        return filter == null ? ALL : filter;
    }
}

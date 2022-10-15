package org.pro.adaway.ui.lists;

import static android.content.Intent.ACTION_SEARCH;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import org.pro.adaway.R;
import org.pro.adaway.databinding.ListsFragmentBinding;
import org.pro.adaway.ui.ThemedActivity;
import org.pro.adaway.ui.adblocking.ApplyConfigurationSnackbar;

/**
 * This activity display hosts list items.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class ListsActivity extends ThemedActivity {
    /**
     * The tab to display argument.
     */
    public static final String TAB = "org.pro.adaway.lists.tab";
    /**
     * The blocked hosts tab index.
     */
    public static final int BLOCKED_HOSTS_TAB = 0;
    /**
     * The allowed hosts tab index.
     */
    public static final int ALLOWED_HOSTS_TAB = 1;
    /**
     * The redirected hosts tab index.
     */
    public static final int REDIRECTED_HOSTS_TAB = 2;
    /**
     * The view model.
     */
    private ListsViewModel listsViewModel;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get requested tab from Intent
        final Intent intent = getIntent();
        final int tab = intent.getIntExtra(TAB, BLOCKED_HOSTS_TAB);

        // Set view content.
        final ListsFragmentBinding listsFragmentBinding = ListsFragmentBinding.inflate(getLayoutInflater());
        setContentView(listsFragmentBinding.getRoot());

        // Configure actionbar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        // Create pager adapter
        final ListsFragmentPagerAdapter pagerAdapter = new ListsFragmentPagerAdapter(this);
        // Set view pager adapter
        listsFragmentBinding.listsViewPager.setAdapter(pagerAdapter);
        // Add view pager on page listener to set selected tab according the selected page
        listsFragmentBinding.listsViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                listsFragmentBinding.navigationView.getMenu().getItem(position).setChecked(true);
                pagerAdapter.ensureActionModeCanceled();
            }
        });
        // Add navigation view item selected listener to change view pager current item
        listsFragmentBinding.navigationView.setOnItemSelectedListener(item -> {
            final int itemId = item.getItemId();

            if (itemId == R.id.lists_navigation_blocked) {
                listsFragmentBinding.listsViewPager.setCurrentItem(0);
                return true;
            }

            if (itemId == R.id.lists_navigation_allowed) {
                listsFragmentBinding.listsViewPager.setCurrentItem(1);
                return true;
            }

            if (itemId == R.id.lists_navigation_redirected) {
                listsFragmentBinding.listsViewPager.setCurrentItem(2);
                return true;
            }

            return false;
        });

        // Display requested tab
        listsFragmentBinding.listsViewPager.setCurrentItem(tab);

        // Set add action button listener
        listsFragmentBinding.listsAdd.setOnClickListener(clickedView -> {
            // Get current fragment position
            final int currentItemPosition = listsFragmentBinding.listsViewPager.getCurrentItem();
            // Add item to the current fragment
            pagerAdapter.addItem(currentItemPosition);
        });
        /*
         * Configure snackbar.
         */

        // Create apply snackbar
        final ApplyConfigurationSnackbar applySnackbar = new ApplyConfigurationSnackbar(listsFragmentBinding.coordinator, false, false);
        // Bind snackbar to view models
        this.listsViewModel = new ViewModelProvider(this).get(ListsViewModel.class);
        this.listsViewModel.getModelChanged().observe(this, applySnackbar.createObserver());
        // Get the intent, verify the action and get the query
        handleQuery(intent);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleQuery(intent);
    }

    private void handleQuery(final Intent intent) {
        if (intent != null && ACTION_SEARCH.equals(intent.getAction()))
            this.listsViewModel.search(intent.getStringExtra(SearchManager.QUERY));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.list_menu, menu);
        // Get the SearchView and set the searchable configuration
        final SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        if (searchManager != null) {
            final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == R.id.menu_toggle_source) {
            this.listsViewModel.toggleSources();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (this.listsViewModel.isSearching()) this.listsViewModel.clearSearch();
        else super.onBackPressed();
    }
}

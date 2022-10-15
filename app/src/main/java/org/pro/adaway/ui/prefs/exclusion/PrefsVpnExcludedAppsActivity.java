package org.pro.adaway.ui.prefs.exclusion;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.pro.adaway.R;
import org.pro.adaway.databinding.VpnExcludedAppActivityBinding;
import org.pro.adaway.helper.PreferenceHelper;
import org.pro.adaway.ui.ThemedActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This activity is the activity to select excluded applications from VPN.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class PrefsVpnExcludedAppsActivity extends ThemedActivity {
    private UserApp[] userApplications;
    private UserAppRecycleViewAdapter adapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final VpnExcludedAppActivityBinding binding = VpnExcludedAppActivityBinding.inflate(getLayoutInflater());
        final RecyclerView recyclerView = binding.getRoot();
        setContentView(recyclerView);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Get recycler view
        recyclerView.setHasFixedSize(true);
        // Defile recycler layout
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Create recycler adapter
        this.userApplications = getUserApplications();
        this.adapter = new UserAppRecycleViewAdapter(this);
        recyclerView.setAdapter(this.adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.vpn_excluded_app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        if (itemId == R.id.select_all) {
            excludeApplications(this.userApplications);
            this.adapter.notifyItemRangeChanged(0, this.adapter.getItemCount());
            return true;
        }
        if (itemId == R.id.deselect_all) {
            includeApplications(this.userApplications);
            this.adapter.notifyItemRangeChanged(0, this.adapter.getItemCount());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Get installed user applications.
     *
     * @return The installed user applications.
     */
    UserApp[] getUserApplications() {
        if (this.userApplications == null) {
            final ApplicationInfo self = getApplicationInfo();
            final PackageManager packageManager = getPackageManager();
            final Set<String> excludedApps = PreferenceHelper.getVpnExcludedApps(this);

            final ArrayList<UserApp> list = new ArrayList<>(0);
            for (final ApplicationInfo appInfo : packageManager.getInstalledApplications(0)) {
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                if (Objects.equals(appInfo.packageName, self.packageName)) continue;
                list.add(new UserApp(
                        packageManager.getApplicationLabel(appInfo),
                        appInfo.packageName,
                        packageManager.getApplicationIcon(appInfo),
                        excludedApps.contains(appInfo.packageName)));
            }
            Collections.sort(list);
            this.userApplications = list.toArray(new UserApp[0]);
        }
        return this.userApplications;
    }

    /**
     * Exclude applications from VPN.
     *
     * @param applications The applications to exclude.
     */
    void excludeApplications(@NonNull final UserApp... applications) {
        for (final UserApp application : applications) application.excluded = true;
        updatePreferences();
    }

    /**
     * Include applications into VPN.
     *
     * @param applications The application to include.
     */
    void includeApplications(@NonNull final UserApp... applications) {
        for (final UserApp application : applications) application.excluded = false;
        updatePreferences();
    }

    private void updatePreferences() {
        final Set<String> excludedApplicationPackageNames = new HashSet<>(0, 0.95f);
        for (final UserApp userApp : this.userApplications)
            if (userApp.excluded) excludedApplicationPackageNames.add(userApp.packageName.toString());
        PreferenceHelper.setVpnExcludedApps(this, excludedApplicationPackageNames);
    }
}

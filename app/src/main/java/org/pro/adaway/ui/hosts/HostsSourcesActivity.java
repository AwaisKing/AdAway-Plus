package org.pro.adaway.ui.hosts;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import org.pro.adaway.R;
import org.pro.adaway.ui.ThemedActivity;

/**
 * This activity display hosts list items.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class HostsSourcesActivity extends ThemedActivity {
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set view content.
        setContentView(R.layout.hosts_sources_activity);

        // Configure actionbar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        // Set fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.hosts_sources_container, new HostsSourcesFragment())
                .commit();
    }
}

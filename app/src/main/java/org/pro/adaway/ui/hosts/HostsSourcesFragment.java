/*
 * Copyright (C) 2011-2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This file is part of AdAway.
 *
 * AdAway is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdAway is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdAway.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.pro.adaway.ui.hosts;

import static org.pro.adaway.ui.source.SourceEditActivity.SOURCE_ID;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.pro.adaway.databinding.HostsSourcesFragmentBinding;
import org.pro.adaway.db.entity.HostsSource;
import org.pro.adaway.ui.adblocking.ApplyConfigurationSnackbar;
import org.pro.adaway.ui.source.SourceEditActivity;

/**
 * This class is a {@link Fragment} to display and manage hosts sources.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class HostsSourcesFragment extends Fragment  {
    /**
     * The view model (<code>null</code> if view is not created).
     */
    private HostsSourcesViewModel mViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        // Get context
        Context context = getActivity();
        if (context == null) context = getContext();
        if (context == null && container != null) context = container.getContext();
        if (context == null) context = inflater.getContext();

        // Initialize view model
        this.mViewModel = new ViewModelProvider(this).get(HostsSourcesViewModel.class);
        final LifecycleOwner lifecycleOwner = getViewLifecycleOwner();

        // Create fragment view
        final HostsSourcesFragmentBinding fragmentBinding = HostsSourcesFragmentBinding.inflate(inflater);

        // Get lists layout to attached snackbar to
        final CoordinatorLayout coordinatorLayout = fragmentBinding.getRoot();
        // Create apply snackbar
        final ApplyConfigurationSnackbar applySnackbar = new ApplyConfigurationSnackbar(coordinatorLayout, true, true);
        // Bind snakbar to view models
        this.mViewModel.getHostsSources().observe(lifecycleOwner, applySnackbar.createObserver());

        // Store recycler view
        fragmentBinding.hostsSourcesList.setHasFixedSize(true);
        // Defile recycler layout
        fragmentBinding.hostsSourcesList.setLayoutManager(new LinearLayoutManager(context));
        // Create recycler adapter
        final HostsSourcesAdapter adapter = new HostsSourcesAdapter(this);
        fragmentBinding.hostsSourcesList.setAdapter(adapter);
        // Bind adapter to view model
        this.mViewModel.getHostsSources().observe(lifecycleOwner, adapter::submitList);

        // Set click listener to display menu add entry
        fragmentBinding.hostsSourcesAdd.setOnClickListener(actionButton -> startSourceEdition(null));
        // Return fragment view
        return coordinatorLayout;
    }

    /**
     * Toggle host source enable status.
     *
     * @param source The hosts source to toggle status.
     */
    void toggleEnabled(final HostsSource source) {
        this.mViewModel.toggleSourceEnabled(source);
    }

    /**
     * Start an action.
     *
     * @param source     The hosts source to start the action.
     */
    void edit(final HostsSource source) {
        startSourceEdition(source);
    }

    private void startSourceEdition(@Nullable final HostsSource source) {
        final Intent intent = new Intent(requireContext(), SourceEditActivity.class);
        if (source != null) intent.putExtra(SOURCE_ID, source.getId());
        startActivity(intent);
    }
}

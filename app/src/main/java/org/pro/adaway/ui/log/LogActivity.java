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

package org.pro.adaway.ui.log;

import static org.pro.adaway.ui.Animations.hideView;
import static org.pro.adaway.ui.Animations.showView;
import static java.lang.Boolean.TRUE;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.pro.adaway.R;
import org.pro.adaway.databinding.LogActivityBinding;
import org.pro.adaway.databinding.LogRedirectDialogBinding;
import org.pro.adaway.db.entity.ListType;
import org.pro.adaway.ui.ThemedActivity;
import org.pro.adaway.ui.adblocking.ApplyConfigurationSnackbar;
import org.pro.adaway.ui.dialog.AlertDialogValidator;
import org.pro.adaway.util.Clipboard;
import org.pro.adaway.util.RegexUtils;

/**
 * This class is an {@link android.app.Activity} to show DNS request log entries.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class LogActivity extends ThemedActivity {
    private LogActivityBinding binding;
    /**
     * The view model (<code>null</code> if activity is not created).
     */
    private LogViewModel mViewModel;
    /**
     * The snackbar notification (<code>null</code> if activity is not created).
     */
    private ApplyConfigurationSnackbar mApplySnackbar;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.binding = LogActivityBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Get view model
        this.mViewModel = new ViewModelProvider(this).get(LogViewModel.class);
        // Configure swipe layout.
        this.binding.swipeRefresh.setOnRefreshListener(this.mViewModel::updateLogs);
        // Configure empty view.
        if (this.mViewModel.areBlockedRequestsIgnored())
            this.binding.emptyTextView.append(getString(R.string.log_blocked_requests_ignored));

        // Get recycler view
        this.binding.logList.setHasFixedSize(true);
        // Defile recycler layout
        this.binding.logList.setLayoutManager(new LinearLayoutManager(this));
        // Create recycler adapter
        final LogAdapter adapter = new LogAdapter(this);
        this.binding.logList.setAdapter(adapter);

        // Configure fab.
        this.binding.toggleLogRecording.setOnClickListener(v -> this.mViewModel.toggleRecording());
        this.mViewModel.isRecording().observe(this, recording -> this.binding.toggleLogRecording
                .setImageResource(TRUE.equals(recording) ? R.drawable.ic_pause_24dp : R.drawable.ic_record_24dp));

        // Create apply snackbar
        this.mApplySnackbar = new ApplyConfigurationSnackbar(this.binding.swipeRefresh, false, false);

        // Bind view model to the list view
        this.mViewModel.getLogs().observe(this, logEntries -> {
            if (logEntries.isEmpty()) {
                showView(this.binding.emptyTextView);
            } else {
                hideView(this.binding.emptyTextView);
            }
            adapter.submitList(logEntries);
            this.binding.swipeRefresh.setRefreshing(false);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mark as loading data
        this.binding.swipeRefresh.setRefreshing(true);
        // Load initial data
        this.mViewModel.updateLogs();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.log_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.sort) {
            this.mViewModel.toggleSort();
            return true;
        }
        if (itemId == R.id.delete) {
            this.mViewModel.clearLogs();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Add a {@link org.pro.adaway.db.entity.HostListItem}.
     *
     * @param hostName The item host name.
     * @param type     The item type.
     */
    void addListItem(@NonNull final String hostName, @NonNull final ListType type) {
        // Check view model and snackbar notification
        if (this.mViewModel == null || this.mApplySnackbar == null) return;
        // Check type other than redirection
        if (type != ListType.REDIRECTED) {
            // Insert list item
            this.mViewModel.addListItem(hostName, type, null);
            // Display snackbar notification
            this.mApplySnackbar.notifyUpdateAvailable();
        } else {
            // Create dialog view
            final LayoutInflater inflater = LayoutInflater.from(this);
            final LogRedirectDialogBinding redirectBinding = LogRedirectDialogBinding.inflate(inflater);
            // Create dialog
            final AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                    .setCancelable(true)
                    .setTitle(R.string.log_redirect_dialog_title)
                    .setView(redirectBinding.getRoot())
                    // Setup buttons
                    .setPositiveButton(R.string.button_add, (dialog, which) -> {
                        // Close dialog
                        dialog.dismiss();
                        // Check IP is valid
                        final String ip = redirectBinding.redirectIp.getText().toString();
                        if (RegexUtils.isValidIP(ip)) {
                            // Insert list item
                            this.mViewModel.addListItem(hostName, type, ip);
                            // Display snackbar notification
                            this.mApplySnackbar.notifyUpdateAvailable();
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.dismiss())
                    .create();
            // Show dialog
            alertDialog.show();
            // Set button validation behavior
            redirectBinding.redirectIp.addTextChangedListener(new AlertDialogValidator(alertDialog, RegexUtils::isValidIP, false));
        }
    }

    /**
     * Remove a {@link org.pro.adaway.db.entity.HostListItem}
     *
     * @param hostName The item host name.
     */
    void removeListItem(@NonNull final String hostName) {
        if (this.mViewModel != null && this.mApplySnackbar != null) {
            this.mViewModel.removeListItem(hostName);
            this.mApplySnackbar.notifyUpdateAvailable();
        }
    }

    /**
     * Open an host into the user browser.
     *
     * @param hostName The host name to open.
     */
    void openHostInBrowser(@NonNull final String hostName) {
        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://" + hostName)));
    }

    /**
     * Copy an host into the clipboard.
     *
     * @param hostName The list to copy hosts.
     */
    void copyHostToClipboard(@NonNull final String hostName) {
        Clipboard.copyHostToClipboard(this, hostName);
    }
}

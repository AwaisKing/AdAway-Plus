package org.pro.adaway.ui.welcome;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import org.pro.adaway.R;
import org.pro.adaway.databinding.WelcomeSyncLayoutBinding;
import org.pro.adaway.model.error.HostError;
import org.pro.adaway.ui.home.HomeViewModel;

import static org.pro.adaway.ui.Animations.hideView;
import static org.pro.adaway.ui.Animations.showView;

/**
 * This class is a fragment to first sync the main hosts source.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class WelcomeSyncFragment extends WelcomeFragment {
    private WelcomeSyncLayoutBinding binding;
    private HomeViewModel homeViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        this.binding = WelcomeSyncLayoutBinding.inflate(inflater, container, false);
        bindRetry();

        this.homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        final LifecycleOwner lifecycleOwner = getViewLifecycleOwner();
        this.homeViewModel.isAdBlocked().observe(lifecycleOwner, adBlocked -> {
            if (adBlocked) {
                notifySynced();
            }
        });
        this.homeViewModel.getError().observe(lifecycleOwner, this::notifyError);
        this.homeViewModel.sync();

        return this.binding.getRoot();
    }

    private void bindRetry() {
        this.binding.retryCardView.setOnClickListener(this::retry);
    }

    private void notifySynced() {
        this.homeViewModel.enableAllSources();
        this.binding.headerTextView.setText(R.string.welcome_synced_header);
        hideView(this.binding.progressBar);
        hideView(this.binding.retryCardView);
        hideView(this.binding.errorSyncImageView);
        showView(this.binding.syncedImageView);
        allowNext();
    }

    private void notifyError(@NonNull final HostError error) {
        final Resources resources = getResources();
        final String errorMessage = resources.getText(error.getMessageKey()).toString();
        final String syncError = resources.getText(R.string.welcome_sync_error).toString();
        final String retryMessage = String.format(syncError, errorMessage);
        this.binding.errorTextView.setText(
                resources.getString(R.string.welcome_sync_error, errorMessage)
        );
        hideView(this.binding.progressBar);
        showView(this.binding.errorSyncImageView);
        showView(this.binding.retryCardView);
    }

    private void retry(final View view) {
        hideView(this.binding.retryCardView);
        hideView(this.binding.errorSyncImageView);
        showView(this.binding.progressBar);
        this.homeViewModel.sync();
    }
}

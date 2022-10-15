package org.pro.adaway.ui.update;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.pro.adaway.ui.support.SupportActivity.SPONSORSHIP_LINK;
import static org.pro.adaway.ui.support.SupportActivity.SUPPORT_LINK;
import static org.pro.adaway.ui.support.SupportActivity.bindLink;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import org.pro.adaway.R;
import org.pro.adaway.databinding.UpdateActivityBinding;
import org.pro.adaway.model.update.Manifest;
import org.pro.adaway.ui.ThemedActivity;

/**
 * This class is the application main activity.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class UpdateActivity extends ThemedActivity {
    private UpdateActivityBinding activityBinding;
    private UpdateViewModel updateViewModel;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.activityBinding = UpdateActivityBinding.inflate(getLayoutInflater());
        setContentView(this.activityBinding.getRoot());

        this.updateViewModel = new ViewModelProvider(this).get(UpdateViewModel.class);
        bindListeners();
        bindManifest();
        bindProgress();
    }

    private void bindListeners() {
        this.activityBinding.updateButton.setOnClickListener(this::startUpdate);
        bindLink(this.activityBinding.updateDonateButton, SUPPORT_LINK);
        bindLink(this.activityBinding.updateSponsorButton, SPONSORSHIP_LINK);
    }

    private void bindManifest() {
        this.updateViewModel.getAppManifest().observe(this, manifest -> {
            if (manifest.updateAvailable) showUpdate(manifest);
            else markUpToDate(manifest);
        });
    }

    private void bindProgress() {
        this.updateViewModel.getDownloadProgress().observe(this, progress -> {
            this.activityBinding.updateButton.setVisibility(INVISIBLE);
            this.activityBinding.downloadProgressBar.setVisibility(VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                this.activityBinding.downloadProgressBar.setProgress(progress.progress, true);
            else
                this.activityBinding.downloadProgressBar.setProgress(progress.progress);
            this.activityBinding.progressTextView.setText(progress.format(this));
        });
    }

    private void refreshChangelog(@NonNull final Manifest manifest) {
        this.activityBinding.changelogTextView.setText(manifest.changelog);
    }

    private void markUpToDate(@NonNull final Manifest manifest) {
        this.activityBinding.headerTextView.setText(R.string.update_up_to_date_header);
        this.activityBinding.updateButton.setVisibility(GONE);
        refreshChangelog(manifest);
    }

    private void showUpdate(@NonNull final Manifest manifest) {
        this.activityBinding.headerTextView.setText(R.string.update_update_available_header);
        this.activityBinding.updateButton.setVisibility(VISIBLE);
        refreshChangelog(manifest);
    }

    private void startUpdate(final View view) {
        this.activityBinding.updateButton.setVisibility(INVISIBLE);
        this.activityBinding.downloadProgressBar.setVisibility(VISIBLE);
        this.updateViewModel.update();
    }
}

package org.pro.adaway.ui.prefs.exclusion;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.pro.adaway.databinding.VpnExcludedAppEntryBinding;

/**
 * This class is the {@link RecyclerView.Adapter} for the {@link PrefsVpnExcludedAppsActivity}.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class UserAppRecycleViewAdapter extends RecyclerView.Adapter<UserAppRecycleViewAdapter.ViewHolder> {
    private final PrefsVpnExcludedAppsActivity excludedAppsActivity;

    /**
     * Constructor.
     *
     * @param excludedAppsActivity The user applications.
     */
    UserAppRecycleViewAdapter(final PrefsVpnExcludedAppsActivity excludedAppsActivity) {
        this.excludedAppsActivity = excludedAppsActivity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final VpnExcludedAppEntryBinding binding = VpnExcludedAppEntryBinding.inflate(layoutInflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final UserApp[] applications = this.excludedAppsActivity.getUserApplications();
        final VpnExcludedAppEntryBinding entryBinding = holder.binding;
        final UserApp application = applications[position];

        entryBinding.excludedSwitch.setOnCheckedChangeListener(null);
        entryBinding.iconImageView.setImageDrawable(application.icon);
        entryBinding.nameTextView.setText(application.name);
        entryBinding.packageTextView.setText(application.packageName);
        entryBinding.excludedSwitch.setChecked(application.excluded);
        entryBinding.excludedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) this.excludedAppsActivity.excludeApplications(application);
            else this.excludedAppsActivity.includeApplications(application);
        });
        entryBinding.rowLayout.setOnClickListener(v ->
                entryBinding.excludedSwitch.setChecked(!entryBinding.excludedSwitch.isChecked()));
    }

    @Override
    public int getItemCount() {
        return this.excludedAppsActivity.getUserApplications().length;
    }

    /**
     * This class is a the {@link RecyclerView.ViewHolder} for the app list view.
     *
     * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final VpnExcludedAppEntryBinding binding;

        /**
         * Constructor.
         *
         * @param binding The hosts sources view binding.
         */
        ViewHolder(@NonNull final VpnExcludedAppEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

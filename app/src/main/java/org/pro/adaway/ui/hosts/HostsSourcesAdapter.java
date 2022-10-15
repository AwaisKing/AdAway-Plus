package org.pro.adaway.ui.hosts;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.pro.adaway.R;
import org.pro.adaway.databinding.HostsSourcesCardBinding;
import org.pro.adaway.db.entity.HostsSource;
import org.threeten.bp.Duration;
import org.threeten.bp.ZonedDateTime;

import java.util.Objects;

/**
 * This class is a the {@link RecyclerView.Adapter} for the hosts sources view.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class HostsSourcesAdapter extends ListAdapter<HostsSource, HostsSourcesAdapter.ViewHolder> {
    /**
     * This callback is use to compare hosts sources.
     */
    private static final DiffUtil.ItemCallback<HostsSource> DIFF_CALLBACK = new DiffUtil.ItemCallback<HostsSource>() {
        @Override
        public boolean areItemsTheSame(@NonNull final HostsSource oldSource, @NonNull final HostsSource newSource) {
            final String oldSourceUrl = oldSource.getUrl();
            final String newSourceUrl = newSource.getUrl();
            return oldSourceUrl.equals(newSourceUrl);
        }

        @Override
        public boolean areContentsTheSame(@NonNull final HostsSource oldSource, @NonNull final HostsSource newSource) {
            // NOTE: if you use equals, your object must properly override Object#equals()
            // Incorrectly returning false here will result in too many animations.
            return Objects.equals(oldSource, newSource);
        }
    };
    private static final String[] QUANTITY_PREFIXES = new String[]{"k", "M", "G"};

    /**
     * Context for the adapter
     */
    private final Context context;
    /**
     * LayoutInflater for adapter and views
     */
    private final LayoutInflater layoutInflater;
    /**
     * This callback is use to call view actions.
     */
    @NonNull
    private final HostsSourcesFragment sourcesFragment;

    /**
     * Constructor.
     *
     * @param sourcesFragment The sources fragment callback.
     */
    HostsSourcesAdapter(@NonNull final HostsSourcesFragment sourcesFragment) {
        super(DIFF_CALLBACK);
        this.sourcesFragment = sourcesFragment;
        this.context = sourcesFragment.getContext();
        this.layoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public HostsSourcesAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new ViewHolder(HostsSourcesCardBinding.inflate(layoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final HostsSource source = getItem(position);
        final HostsSourcesCardBinding cardBinding = holder.cardBinding;

        cardBinding.sourceEnabledCheckBox.setChecked(source.isEnabled());
        cardBinding.sourceEnabledCheckBox.setOnClickListener(view -> sourcesFragment.toggleEnabled(source));
        cardBinding.sourceLabelTextView.setText(source.getLabel());
        cardBinding.sourceUrlTextView.setText(source.getUrl());
        cardBinding.sourceUpdateTextView.setText(getUpdateText(source));
        cardBinding.sourceSizeTextView.setText(getHostCount(source));

        holder.itemView.setOnClickListener(view -> sourcesFragment.edit(source));
    }

    /**
     * Get the approximate delay from a date to now.
     *
     * @param from The date from which computes the delay.
     *
     * @return The approximate delay.
     */
    private String getApproximateDelay(final ZonedDateTime from) {
        final Resources resources = context.getResources(); // Get resource for plurals
        final ZonedDateTime now = ZonedDateTime.now(); // Get current date in UTC timezone
        long delay = Duration.between(from, now).toMinutes(); // Get delay between from and now in minutes
        // Check if delay is lower than an hour
        if (delay < 60) return resources.getString(R.string.hosts_source_few_minutes);

        // Get delay in hours
        delay /= 60;
        // Check if delay is lower than a day
        if (delay < 24) {
            final int hours = (int) delay;
            return resources.getQuantityString(R.plurals.hosts_source_hours, hours, hours);
        }

        // Get delay in days
        delay /= 24;
        // Check if delay is lower than a month
        if (delay < 30) {
            final int days = (int) delay;
            return resources.getQuantityString(R.plurals.hosts_source_days, days, days);
        }

        // Get delay in months
        final int months = (int) delay / 30;
        return resources.getQuantityString(R.plurals.hosts_source_months, months, months);
    }

    private String getUpdateText(@NonNull final HostsSource source) {
        // Check if source is enabled
        if (!source.isEnabled()) return context.getString(R.string.hosts_source_disabled);
        // Get date modification variables from source
        final ZonedDateTime onlineModificationDate = source.getOnlineModificationDate();
        final ZonedDateTime localModificationDate = source.getLocalModificationDate();
        // Check modification dates
        final boolean lastOnlineModificationDefined = onlineModificationDate != null;
        final boolean lastLocalModificationDefined = localModificationDate != null;
        // Check if last online modification date is known
        if (lastOnlineModificationDefined) {
            // Get last online modification delay
            final String approximateDelay = getApproximateDelay(onlineModificationDate);
            if (!lastLocalModificationDefined)
                return context.getString(R.string.hosts_source_last_update, approximateDelay);
            if (onlineModificationDate.isAfter(localModificationDate))
                return context.getString(R.string.hosts_source_need_update, approximateDelay);
            return context.getString(R.string.hosts_source_up_to_date, approximateDelay);
        } else {
            if (lastLocalModificationDefined)
                return context.getString(R.string.hosts_source_installed, getApproximateDelay(localModificationDate));
            return context.getString(R.string.hosts_source_unknown_status);
        }
    }

    private String getHostCount(@NonNull final HostsSource source) {
        // Note: NumberFormat.getCompactNumberInstance is Java 12 only
        // Check empty source
        int size = source.getSize();
        if (size <= 0 || !source.isEnabled()) {
            return "";
        }
        // Compute size decimal length
        int length = 1;
        while (size > 10) {
            size /= 10;
            length++;
        }
        // Compute prefix to use
        int prefixIndex = (length - 1) / 3 - 1;
        // Return formatted count
        size = source.getSize();

        final String retStr;
        if (prefixIndex < 0) retStr = Integer.toString(size);
        else {
            if (prefixIndex >= QUANTITY_PREFIXES.length) {
                prefixIndex = QUANTITY_PREFIXES.length - 1;
                size = 13;
            }
            size = Math.toIntExact(Math.round(size / Math.pow(10, (prefixIndex + 1) * 3D)));
            retStr = Integer.toString(size).concat(QUANTITY_PREFIXES[prefixIndex]);
        }

        return context.getString(R.string.hosts_count, retStr);
    }

    /**
     * This class is a the {@link RecyclerView.ViewHolder} for the hosts sources view.
     *
     * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final HostsSourcesCardBinding cardBinding;

        /**
         * Constructor.
         *
         * @param cardBinding The hosts sources view.
         */
        ViewHolder(@NonNull final HostsSourcesCardBinding cardBinding) {
            super(cardBinding.getRoot());
            this.cardBinding = cardBinding;
        }
    }
}

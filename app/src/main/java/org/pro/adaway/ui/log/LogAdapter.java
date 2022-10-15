package org.pro.adaway.ui.log;

import static android.graphics.PorterDuff.Mode.MULTIPLY;
import static org.pro.adaway.db.entity.ListType.ALLOWED;
import static org.pro.adaway.db.entity.ListType.BLOCKED;
import static org.pro.adaway.db.entity.ListType.REDIRECTED;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.pro.adaway.R;
import org.pro.adaway.databinding.LogEntryBinding;
import org.pro.adaway.db.entity.ListType;

/**
 * This class is a the {@link RecyclerView.Adapter} for the DNS request log view.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class LogAdapter extends ListAdapter<LogEntry, LogAdapter.ViewHolder> {
    /**
     * This callback is use to compare hosts sources.
     */
    private static final DiffUtil.ItemCallback<LogEntry> DIFF_CALLBACK = new DiffUtil.ItemCallback<LogEntry>() {
        @Override
        public boolean areItemsTheSame(@NonNull final LogEntry oldEntry, @NonNull final LogEntry newEntry) {
            return oldEntry.getHost().equals(newEntry.getHost());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final LogEntry oldEntry, @NonNull final LogEntry newEntry) {
            return oldEntry.equals(newEntry);
        }
    };

    /**
     * The activity callbacks.
     */
    private final LogActivity logActivity;
    private final LayoutInflater layoutInflater;

    LogAdapter(final LogActivity logActivity) {
        super(DIFF_CALLBACK);
        this.layoutInflater = LayoutInflater.from(logActivity);
        this.logActivity = logActivity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        return new ViewHolder(LogEntryBinding.inflate(layoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        // Get log entry
        final LogEntry entry = getItem(position);
        // Set host name
        holder.binding.hostnameTextView.setText(entry.getHost());
        holder.binding.hostnameTextView.setOnClickListener(v -> this.logActivity.openHostInBrowser(entry.getHost()));
        holder.binding.hostnameTextView.setOnLongClickListener(v -> {
            this.logActivity.copyHostToClipboard(entry.getHost());
            return true;
        });
        // Set type status
        bindImageView(holder.binding.blockImageView, BLOCKED, entry);
        bindImageView(holder.binding.allowImageView, ALLOWED, entry);
        bindImageView(holder.binding.redirectionImageView, REDIRECTED, entry);
    }

    private void bindImageView(final ImageView imageView, final ListType type, @NonNull final LogEntry entry) {
        if (type == entry.getType()) {
            final int primaryColor = ResourcesCompat.getColor(logActivity.getResources(), R.color.primary, logActivity.getTheme());
            imageView.setColorFilter(primaryColor, MULTIPLY);
            imageView.setOnClickListener(v -> logActivity.removeListItem(entry.getHost()));
        } else {
            imageView.clearColorFilter();
            imageView.setOnClickListener(v -> logActivity.addListItem(entry.getHost(), type));
        }
    }

    /**
     * This class is a the {@link RecyclerView.ViewHolder} for the log entry view.
     *
     * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final LogEntryBinding binding;

        /**
         * Constructor.
         *
         * @param binding The log entry view binding.
         */
        ViewHolder(@NonNull final LogEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

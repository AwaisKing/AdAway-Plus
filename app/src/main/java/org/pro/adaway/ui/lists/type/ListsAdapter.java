package org.pro.adaway.ui.lists.type;

import static org.pro.adaway.db.entity.HostsSource.USER_SOURCE_ID;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import org.pro.adaway.databinding.CheckboxListEntryBinding;
import org.pro.adaway.databinding.CheckboxListTwoEntriesBinding;
import org.pro.adaway.db.entity.HostListItem;

/**
 * This class is a the {@link RecyclerView.Adapter} for the hosts list view.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class ListsAdapter extends PagingDataAdapter<HostListItem, ListsAdapter.ViewHolder> {
    /**
     * This callback is use to compare hosts sources.
     */
    private static final DiffUtil.ItemCallback<HostListItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<HostListItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull final HostListItem oldItem, @NonNull final HostListItem newItem) {
            return oldItem.getHost().equals(newItem.getHost());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final HostListItem oldItem, @NonNull final HostListItem newItem) {
            // NOTE: if you use equals, your object must properly override Object#equals()
            // Incorrectly returning false here will result in too many animations.
            return oldItem.equals(newItem);
        }
    };

    /**
     * This callback is use to call view actions.
     */
    @NonNull
    private final AbstractListFragment abstractListFragment;
    /**
     * Whether the list item needs two rows or not.
     */
    private final boolean twoRows;
    /**
     * LayoutInflater to inflate views
     */
    private final LayoutInflater layoutInflater;

    /**
     * Constructor.
     *
     * @param abstractListFragment The abstract fragment callback.
     * @param twoRows              Whether the list items need two rows or not.
     */
    ListsAdapter(@NonNull final AbstractListFragment abstractListFragment, final boolean twoRows) {
        super(DIFF_CALLBACK);
        this.layoutInflater = LayoutInflater.from(abstractListFragment.getContext());
        this.abstractListFragment = abstractListFragment;
        this.twoRows = twoRows;
    }

    @NonNull
    @Override
    public ListsAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final ViewBinding viewBinding = this.twoRows
                ? CheckboxListTwoEntriesBinding.inflate(layoutInflater, parent, false)
                : CheckboxListEntryBinding.inflate(layoutInflater, parent, false);
        return new ViewHolder(viewBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final HostListItem item = getItem(position);
        final ViewBinding viewBinding = holder.viewBinding;

        final CheckBox enabledCheckBox;
        final TextView hostTextView;
        final TextView redirectionTextView;
        if (viewBinding instanceof CheckboxListTwoEntriesBinding) {
            final CheckboxListTwoEntriesBinding entriesBinding = (CheckboxListTwoEntriesBinding) viewBinding;
            redirectionTextView = entriesBinding.checkboxListSubtext;
            enabledCheckBox = entriesBinding.checkboxListCheckbox;
            hostTextView = entriesBinding.checkboxListText;
        } else {
            final CheckboxListEntryBinding entryBinding = (CheckboxListEntryBinding) viewBinding;
            redirectionTextView = null;
            enabledCheckBox = entryBinding.checkboxListCheckbox;
            hostTextView = entryBinding.checkboxListText;
        }

        // Data might be null if not loaded yet
        if (item == null) {
            enabledCheckBox.setChecked(true);
            enabledCheckBox.setEnabled(false);
            enabledCheckBox.setOnClickListener(null);
            hostTextView.setText(null);
            if (redirectionTextView != null) redirectionTextView.setText(null);
            holder.itemView.setOnLongClickListener(null);
            return;
        }

        final boolean editable = item.getSourceId() == USER_SOURCE_ID;
        enabledCheckBox.setEnabled(editable);
        enabledCheckBox.setChecked(item.isEnabled());
        enabledCheckBox.setOnClickListener(editable ? view -> this.abstractListFragment.toggleItemEnabled(item) : null);
        hostTextView.setText(item.getHost());
        if (redirectionTextView != null) redirectionTextView.setText(item.getRedirection());
        holder.itemView.setOnLongClickListener(editable ?
                view -> this.abstractListFragment.startAction(item, holder.itemView) :
                view -> this.abstractListFragment.copyHostToClipboard(item));
    }

    /**
     * This class is a the {@link RecyclerView.ViewHolder} for the hosts list view.
     *
     * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ViewBinding viewBinding;

        /**
         * Constructor.
         *
         * @param viewBinding The hosts sources view {@link ViewBinding}.
         */
        ViewHolder(@NonNull final ViewBinding viewBinding) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;
        }
    }
}

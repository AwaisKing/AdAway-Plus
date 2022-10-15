package org.pro.adaway.ui.lists.type;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagingData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.pro.adaway.R;
import org.pro.adaway.databinding.HostsListsFragmentBinding;
import org.pro.adaway.db.entity.HostListItem;
import org.pro.adaway.ui.lists.ListsViewModel;
import org.pro.adaway.util.Clipboard;

/**
 * This class is a {@link Fragment} to display and manage lists of {@link org.pro.adaway.ui.lists.ListsActivity}.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public abstract class AbstractListFragment extends Fragment {
    /**
     * The view model (<code>null</code> if view is not created).
     */
    protected ListsViewModel mViewModel;
    /**
     * The current activity (<code>null</code> if view is not created).
     */
    protected FragmentActivity mActivity;
    /**
     * The current action mode when item is selection (<code>null</code> if no action started).
     */
    private ActionMode mActionMode;
    /**
     * The action mode callback (<code>null</code> if view is not created).
     */
    private ActionMode.Callback mActionCallback;
    /**
     * The hosts list related to the current action (<code>null</code> if view is not created).
     */
    private HostListItem mActionItem;
    /**
     * The view related hosts source of the current action (<code>null</code> if view is not created).
     */
    private View mActionSourceView;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        // Store activity
        this.mActivity = requireActivity();

        // Create fragment view
        final HostsListsFragmentBinding fragmentBinding = HostsListsFragmentBinding.inflate(inflater, container, false);
        final RecyclerView rvHostsList = fragmentBinding.getRoot();

        // Store recycler view
        rvHostsList.setHasFixedSize(true);
        // Defile recycler layout
        rvHostsList.setLayoutManager(new LinearLayoutManager(this.mActivity));
        // Create recycler adapter
        final ListsAdapter adapter = new ListsAdapter(this, isTwoRowsItem());
        rvHostsList.setAdapter(adapter);

        // Create action mode callback to display edit/delete menu
        this.mActionCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(final ActionMode actionMode, final Menu menu) {
                // Get menu inflater
                final MenuInflater inflater = actionMode.getMenuInflater();
                // Set action mode title
                actionMode.setTitle(R.string.checkbox_list_context_title);
                // Inflate edit/delete menu
                inflater.inflate(R.menu.checkbox_list_context, menu);
                // Return action created
                return true;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode actionMode, final MenuItem item) {
                // Check action item
                if (mActionItem == null) return false;

                // Check item identifier
                final int itemId = item.getItemId();
                if (itemId == R.id.edit_action) {
                    // Edit action item
                    editItem(mActionItem);
                    // Finish action mode
                    mActionMode.finish();
                    return true;
                }

                if (itemId == R.id.delete_action) {
                    // Delete action item
                    deleteItem(mActionItem);
                    // Finish action mode
                    mActionMode.finish();
                    return true;
                }

                return false;
            }

            @Override
            public void onDestroyActionMode(final ActionMode actionMode) {
                // Clear view background color
                if (mActionSourceView != null) mActionSourceView.setBackgroundColor(Color.TRANSPARENT);
                // Clear current source and its view
                mActionItem = null;
                mActionSourceView = null;
                // Clear action mode
                mActionMode = null;
            }

            @Override
            public boolean onPrepareActionMode(final ActionMode actionMode, final Menu menu) {
                // Nothing special to do
                return false;
            }
        };

        // Get view model and bind it to the list view
        this.mViewModel = new ViewModelProvider(this.mActivity).get(ListsViewModel.class);
        getData().observe(getViewLifecycleOwner(), data -> adapter.submitData(getLifecycle(), data));

        // Return created view
        return rvHostsList;
    }

    /**
     * Start an action.
     *
     * @param item       The list to start the action.
     * @param sourceView The list related view.
     *
     * @return <code>true</code> if the action was started, <code>false</code> otherwise.
     */
    boolean startAction(final HostListItem item, final View sourceView) {
        // Check if there is already a current action
        if (this.mActionMode != null) return false;
        // Store current source and its view
        this.mActionItem = item;
        this.mActionSourceView = sourceView;
        // Get current item background color
        final int currentItemBackgroundColor = ResourcesCompat.getColor(getResources(), R.color.selected_background, mActivity.getTheme());
        // Apply background color to view
        this.mActionSourceView.setBackgroundColor(currentItemBackgroundColor);
        // Start action mode and store it
        this.mActionMode = this.mActivity.startActionMode(this.mActionCallback);
        // Return event consumed
        return true;
    }

    /**
     * Copy an hosts into clipboard.
     *
     * @param item The list to copy hosts.
     */
    boolean copyHostToClipboard(@NonNull final HostListItem item) {
        Clipboard.copyHostToClipboard(this.mActivity, item.getHost());
        return true;
    }

    /**
     * Ensure action mode is cancelled.
     */
    public void ensureActionModeCanceled() {
        if (this.mActionMode != null) this.mActionMode.finish();
    }

    protected abstract LiveData<PagingData<HostListItem>> getData();

    protected boolean isTwoRowsItem() {
        return false;
    }

    /**
     * Display a UI to add an item to the list.
     */
    public abstract void addItem();

    protected abstract void editItem(final HostListItem item);

    protected void deleteItem(final HostListItem item) {
        this.mViewModel.removeListItem(item);
    }

    /**
     * Toggle item enable status.
     *
     * @param item The list to toggle status.
     */
    void toggleItemEnabled(final HostListItem item) {
        this.mViewModel.toggleItemEnabled(item);
    }
}

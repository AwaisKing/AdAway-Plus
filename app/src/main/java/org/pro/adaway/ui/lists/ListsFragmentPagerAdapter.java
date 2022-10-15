package org.pro.adaway.ui.lists;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.pro.adaway.ui.lists.type.AbstractListFragment;
import org.pro.adaway.ui.lists.type.AllowedHostsFragment;
import org.pro.adaway.ui.lists.type.BlockedHostsFragment;
import org.pro.adaway.ui.lists.type.RedirectedHostsFragment;

import static org.pro.adaway.ui.lists.ListsActivity.BLOCKED_HOSTS_TAB;
import static org.pro.adaway.ui.lists.ListsActivity.REDIRECTED_HOSTS_TAB;
import static org.pro.adaway.ui.lists.ListsActivity.ALLOWED_HOSTS_TAB;

/**
 * This class is a {@link FragmentStateAdapter} to store lists tab fragments.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class ListsFragmentPagerAdapter extends FragmentStateAdapter {
    /**
     * The number of fragment.
     */
    private static final int FRAGMENT_COUNT = 3;
    /**
     * The blacklist fragment (<code>null</code> until first retrieval).
     */
    private final AbstractListFragment blacklistFragment;
    /**
     * The whitelist fragment (<code>null</code> until first retrieval).
     */
    private final AbstractListFragment whitelistFragment;
    /**
     * The redirection list fragment (<code>null</code> until first retrieval).
     */
    private final AbstractListFragment redirectionListFragment;

    /**
     * Constructor.
     */
    ListsFragmentPagerAdapter(final FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.blacklistFragment = new BlockedHostsFragment();
        this.whitelistFragment = new AllowedHostsFragment();
        this.redirectionListFragment = new RedirectedHostsFragment();
    }

    @NonNull
    @Override
    public Fragment createFragment(final int position) {
        if (position == BLOCKED_HOSTS_TAB) return this.blacklistFragment;
        if (position == ALLOWED_HOSTS_TAB) return this.whitelistFragment;
        if (position == REDIRECTED_HOSTS_TAB) return this.redirectionListFragment;
        throw new IllegalStateException("Position " + position + " is not supported.");
    }

    @Override
    public int getItemCount() {
        return FRAGMENT_COUNT;
    }

    /**
     * Ensure action mode is cancelled.
     */
    void ensureActionModeCanceled() {
        if (this.blacklistFragment != null)
            this.blacklistFragment.ensureActionModeCanceled();
        if (this.whitelistFragment != null)
            this.whitelistFragment.ensureActionModeCanceled();
        if (this.redirectionListFragment != null)
            this.redirectionListFragment.ensureActionModeCanceled();
    }

    /**
     * Add an item into the requested fragment.
     *
     * @param position The fragment position.
     */
    void addItem(final int position) {
        if (position == BLOCKED_HOSTS_TAB) {
            if (this.blacklistFragment != null)
                this.blacklistFragment.addItem();
        } else if (position == ALLOWED_HOSTS_TAB) {
            if (this.whitelistFragment != null)
                this.whitelistFragment.addItem();
        } else if (position == REDIRECTED_HOSTS_TAB) {
            if (this.redirectionListFragment != null)
                this.redirectionListFragment.addItem();
        }
    }
}

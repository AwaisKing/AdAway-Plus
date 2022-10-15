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

package org.pro.adaway.ui.help;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;

import org.pro.adaway.R;
import org.pro.adaway.databinding.HelpActivityBinding;
import org.pro.adaway.ui.ThemedActivity;

public class HelpActivity extends ThemedActivity {
    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final HelpActivityBinding helpActivityBinding = HelpActivityBinding.inflate(getLayoutInflater());
        setContentView(helpActivityBinding.getRoot());

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        helpActivityBinding.pager.setAdapter(new TabsAdapter(this));

        new TabLayoutMediator(
                helpActivityBinding.tabLayout,
                helpActivityBinding.pager,
                (tab, position) -> tab.setText(getTabName(position))
        ).attach();
    }

    @StringRes
    private int getTabName(final int position) {
        switch (position) {
            case 0:
                return R.string.help_tab_faq;
            case 1:
                return R.string.help_tab_problems;
            case 2:
                return R.string.help_tab_s_on_s_off;
            default:
                throw new IllegalStateException("Position " + position + " is not supported.");
        }
    }

    private static class TabsAdapter extends FragmentStateAdapter {
        private final Fragment faqFragment = new HelpFragmentHtml().setHtmlFile(R.raw.help_faq);
        private final Fragment problemsFragment = new HelpFragmentHtml().setHtmlFile(R.raw.help_problems);
        private final Fragment sonSofFragment = new HelpFragmentHtml().setHtmlFile(R.raw.help_s_on_s_off);

        TabsAdapter(@NonNull final FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(final int position) {
            switch (position) {
                case 0:
                    return this.faqFragment;
                case 1:
                    return this.problemsFragment;
                case 2:
                    return this.sonSofFragment;
                default:
                    throw new IllegalStateException("Position " + position + " is not supported.");
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}

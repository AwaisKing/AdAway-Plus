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
package org.pro.adaway.ui.lists.type;

import static org.pro.adaway.db.entity.ListType.BLOCKED;

import android.text.Editable;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;
import androidx.paging.PagingData;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.pro.adaway.R;
import org.pro.adaway.databinding.ListsBlockedDialogBinding;
import org.pro.adaway.db.entity.HostListItem;
import org.pro.adaway.ui.dialog.AlertDialogValidator;
import org.pro.adaway.util.RegexUtils;

/**
 * This class is a {@link AbstractListFragment} to display and manage blocked hosts.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class BlockedHostsFragment extends AbstractListFragment {
    @Override
    protected LiveData<PagingData<HostListItem>> getData() {
        return this.mViewModel.getBlockedListItems();
    }

    @Override
    public void addItem() {
        // Create dialog view
        final LayoutInflater layoutInflater = LayoutInflater.from(this.mActivity);
        final ListsBlockedDialogBinding dialogBinding = ListsBlockedDialogBinding.inflate(layoutInflater);
        // Create dialog
        final AlertDialog alertDialog = new MaterialAlertDialogBuilder(this.mActivity)
                .setCancelable(true)
                .setTitle(R.string.list_add_dialog_black)
                .setView(dialogBinding.getRoot())
                // Setup buttons
                .setPositiveButton(
                        R.string.button_add,
                        (dialog, which) -> {
                            // Close dialog
                            dialog.dismiss();
                            // Check if hostname is valid
                            final String hostname = "" + dialogBinding.listDialogHostname.getText();
                            if (RegexUtils.isValidHostname(hostname)) {
                                // Insert host to black list
                                this.mViewModel.addListItem(BLOCKED, hostname, null);
                            }
                        })
                .setNegativeButton(
                        R.string.button_cancel,
                        (dialog, which) -> dialog.dismiss()
                )
                .create();
        // Show dialog
        alertDialog.show();
        // Set button validation behavior
        dialogBinding.listDialogHostname.addTextChangedListener(new AlertDialogValidator(alertDialog, RegexUtils::isValidHostname, false));
    }

    @Override
    protected void editItem(@NonNull final HostListItem item) {
        // Create dialog view
        final LayoutInflater layoutInflater = LayoutInflater.from(this.mActivity);
        final ListsBlockedDialogBinding dialogBinding = ListsBlockedDialogBinding.inflate(layoutInflater);
        // Set hostname
        dialogBinding.listDialogHostname.setText(item.getHost());
        // Move cursor to end of EditText
        final Editable inputEditContent = dialogBinding.listDialogHostname.getText();
        dialogBinding.listDialogHostname.setSelection(inputEditContent != null ? inputEditContent.length() : 0);
        // Create dialog
        final AlertDialog alertDialog = new MaterialAlertDialogBuilder(this.mActivity)
                .setCancelable(true)
                .setTitle(R.string.list_edit_dialog_black)
                .setView(dialogBinding.getRoot())
                // Setup buttons
                .setPositiveButton(
                        R.string.button_save,
                        (dialog, which) -> {
                            // Close dialog
                            dialog.dismiss();
                            // Check hostname validity
                            final String hostname = dialogBinding.listDialogHostname.getText().toString();
                            if (RegexUtils.isValidHostname(hostname)) {
                                // Update list item
                                this.mViewModel.updateListItem(item, hostname, null);
                            }
                        })
                .setNegativeButton(
                        R.string.button_cancel
                        , (dialog, which) -> dialog.dismiss()
                )
                .create();
        // Show dialog
        alertDialog.show();
        // Set button validation behavior
        dialogBinding.listDialogHostname.addTextChangedListener(new AlertDialogValidator(alertDialog, RegexUtils::isValidHostname, true));
    }
}

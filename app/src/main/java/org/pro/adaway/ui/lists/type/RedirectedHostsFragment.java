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

import android.text.Editable;
import android.view.LayoutInflater;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;
import androidx.paging.PagingData;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.pro.adaway.R;
import org.pro.adaway.databinding.ListsRedirectedDialogBinding;
import org.pro.adaway.db.entity.HostListItem;
import org.pro.adaway.db.entity.ListType;
import org.pro.adaway.ui.dialog.AlertDialogValidator;
import org.pro.adaway.util.RegexUtils;

/**
 * This class is a {@link AbstractListFragment} to display and manage redirected hosts.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class RedirectedHostsFragment extends AbstractListFragment {
    @Override
    protected boolean isTwoRowsItem() {
        return true;
    }

    @Override
    protected LiveData<PagingData<HostListItem>> getData() {
        return this.mViewModel.getRedirectedListItems();
    }

    @Override
    public void addItem() {
        // Create dialog view
        final LayoutInflater layoutInflater = LayoutInflater.from(this.mActivity);
        final ListsRedirectedDialogBinding dialogBinding = ListsRedirectedDialogBinding.inflate(layoutInflater);

        // Create dialog
        final AlertDialog alertDialog = new MaterialAlertDialogBuilder(this.mActivity)
                .setCancelable(true)
                .setTitle(R.string.list_add_dialog_redirect)
                .setView(dialogBinding.getRoot())
                // Setup buttons
                .setPositiveButton(
                        R.string.button_add,
                        (dialog, which) -> {
                            // Close dialog
                            dialog.dismiss();
                            // Check if hostname and IP are valid
                            final String hostname = dialogBinding.listDialogHostname.getText().toString();
                            final String ip = dialogBinding.listDialogIp.getText().toString();
                            if (RegexUtils.isValidHostname(hostname) && RegexUtils.isValidIP(ip)) {
                                // Insert host to redirection list
                                this.mViewModel.addListItem(ListType.REDIRECTED, hostname, ip);
                            }
                        }
                )
                .setNegativeButton(
                        R.string.button_cancel,
                        (dialog, which) -> dialog.dismiss()
                )
                .create();
        // Show dialog
        alertDialog.show();
        // Set button validation behavior
        final AlertDialogValidator validator = new AlertDialogValidator(
                alertDialog,
                input -> {
                    final String hostname = dialogBinding.listDialogHostname.getText().toString();
                    final String ip = dialogBinding.listDialogIp.getText().toString();
                    return RegexUtils.isValidHostname(hostname) && RegexUtils.isValidIP(ip);
                },
                false
        );

        dialogBinding.listDialogHostname.addTextChangedListener(validator);
        dialogBinding.listDialogIp.addTextChangedListener(validator);
    }

    @Override
    protected void editItem(@NonNull final HostListItem item) {
        // Create dialog view
        final LayoutInflater layoutInflater = LayoutInflater.from(this.mActivity);
        final ListsRedirectedDialogBinding dialogBinding = ListsRedirectedDialogBinding.inflate(layoutInflater);

        final EditText hostnameEditText = dialogBinding.listDialogHostname;
        final EditText ipEditText = dialogBinding.listDialogIp;

        // Set hostname and IP
        hostnameEditText.setText(item.getHost());
        ipEditText.setText(item.getRedirection());
        // Move cursor to end of EditText
        final Editable hostnameEditContent = hostnameEditText.getText();
        hostnameEditText.setSelection(hostnameEditContent.length());
        // Create dialog
        final AlertDialog alertDialog = new MaterialAlertDialogBuilder(this.mActivity)
                .setCancelable(true)
                .setTitle(getString(R.string.list_edit_dialog_redirect))
                .setView(dialogBinding.getRoot())
                // Set buttons
                .setPositiveButton(R.string.button_save,
                        (dialog, which) -> {
                            // Close dialog
                            dialog.dismiss();
                            // Check hostname and IP validity
                            final String hostname = hostnameEditText.getText().toString();
                            final String ip = ipEditText.getText().toString();
                            if (RegexUtils.isValidHostname(hostname) && RegexUtils.isValidIP(ip)) {
                                // Update list item
                                this.mViewModel.updateListItem(item, hostname, ip);
                            }
                        }
                )
                .setNegativeButton(
                        R.string.button_cancel,
                        (dialog, which) -> dialog.dismiss()
                )
                .create();
        // Show dialog
        alertDialog.show();
        // Set button validation behavior
        final AlertDialogValidator validator = new AlertDialogValidator(
                alertDialog,
                input -> {
                    final String hostname = hostnameEditText.getText().toString();
                    final String ip = ipEditText.getText().toString();
                    return RegexUtils.isValidHostname(hostname) && RegexUtils.isValidIP(ip);
                },
                true
        );
        hostnameEditText.addTextChangedListener(validator);
        ipEditText.addTextChangedListener(validator);
    }
}
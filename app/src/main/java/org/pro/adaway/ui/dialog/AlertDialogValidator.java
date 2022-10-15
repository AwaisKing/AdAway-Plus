package org.pro.adaway.ui.dialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.arch.core.util.Function;

import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;

/**
 * This class is a {@link TextWatcher} to validate an alert dialog field.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class AlertDialogValidator implements TextWatcher {
    /**
     * The button to change status.
     */
    private final Button mButton;
    /**
     * The field validator.
     */
    private final Function<String, Boolean> validator;

    /**
     * Constructor.
     *
     * @param dialog       The button to change status.
     * @param validator    The field validator.
     * @param initialState The validation initial state.
     */
    public AlertDialogValidator(@NonNull final AlertDialog dialog, final Function<String, Boolean> validator, final boolean initialState) {
        this.mButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        this.mButton.setEnabled(initialState);
        this.validator = validator;
    }

    @Override
    public void afterTextChanged(@NonNull final Editable s) {
        this.mButton.setEnabled(this.validator.apply(s.toString()));
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {}
}

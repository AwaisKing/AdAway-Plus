package org.pro.adaway.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.pro.adaway.databinding.BottomDrawerLayoutBinding;
import org.pro.adaway.ui.home.HomeActivity;

public class BottomSheetDrawer extends BottomSheetDialogFragment implements View.OnClickListener {
    private HomeActivity homeActivity;
    private BottomDrawerLayoutBinding drawerLayoutBinding;

    public BottomSheetDrawer() {
        super();
    }

    public BottomSheetDrawer setHomeActivity(final HomeActivity homeActivity) {
        this.homeActivity = homeActivity;
        return this;
    }

    public void show() {
        if (homeActivity == null) throw new RuntimeException("HomeActivity is null");
        this.show(homeActivity.getSupportFragmentManager(), null);
    }

    @Override
    public void onClick(final View v) {
        if (homeActivity == null || drawerLayoutBinding == null) return;

        if (v == drawerLayoutBinding.cardViewGitHub) {
            homeActivity.showProjectPage();
            dismiss();

        } else if (v == drawerLayoutBinding.cardViewSettings) {
            homeActivity.startPrefsActivity();
            dismiss();

        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setDismissWithAnimation(true);

        HomeActivity homeActivity = this.homeActivity;
        if (homeActivity == null) {
            if (getActivity() instanceof HomeActivity)
                homeActivity = (HomeActivity) getActivity();
            else if (getContext() instanceof HomeActivity)
                homeActivity = (HomeActivity) getContext();
        }
        this.homeActivity = homeActivity;
        if (homeActivity == null) throw new RuntimeException("HomeActivity is null");

        drawerLayoutBinding = BottomDrawerLayoutBinding.inflate(getLayoutInflater());
        final ConstraintLayout layoutBindingRoot = drawerLayoutBinding.getRoot();

        setupClickListenersForAllCardViews(layoutBindingRoot);

        dialog.setContentView(layoutBindingRoot);
        return dialog;
    }

    private void setupClickListenersForAllCardViews(final View view) {
        if (view instanceof CardView)
            view.setOnClickListener(this);

        else if (view instanceof ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;

            final int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = viewGroup.getChildAt(i);
                setupClickListenersForAllCardViews(child);
            }
        }
    }
}
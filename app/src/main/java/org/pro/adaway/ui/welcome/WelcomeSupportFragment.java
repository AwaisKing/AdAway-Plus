package org.pro.adaway.ui.welcome;

import static android.view.View.VISIBLE;
import static org.pro.adaway.ui.support.SupportActivity.SPONSORSHIP_LINK;
import static org.pro.adaway.ui.support.SupportActivity.SUPPORT_LINK;
import static org.pro.adaway.ui.support.SupportActivity.animateHeart;
import static org.pro.adaway.ui.support.SupportActivity.bindLink;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.pro.adaway.databinding.WelcomeSupportLayoutBinding;

/**
 * This class is a fragment to inform user how to support the application development.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class WelcomeSupportFragment extends WelcomeFragment {
    private WelcomeSupportLayoutBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        this.binding = WelcomeSupportLayoutBinding.inflate(inflater, container, false);

        animateHeart(this.binding.headerImageView);
        bindSupport();
        showAndBindSponsorship();

        return this.binding.getRoot();
    }

    @Override
    protected boolean canGoNext() {
        return true;
    }

    private void bindSupport() {
        bindLink(this.binding.headerImageView, SUPPORT_LINK);
        bindLink(this.binding.headerTextView, SUPPORT_LINK);
        bindLink(this.binding.paypalCardView, SUPPORT_LINK);
    }

    private void showAndBindSponsorship() {
        this.binding.sponsorshipCardView.setVisibility(VISIBLE);
        bindLink(this.binding.sponsorshipCardView, SPONSORSHIP_LINK);
    }
}

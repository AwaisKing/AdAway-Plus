package org.pro.adaway.ui.welcome;

import static org.pro.adaway.ui.Animations.hideView;
import static org.pro.adaway.ui.Animations.showView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import org.pro.adaway.R;
import org.pro.adaway.databinding.WelcomeActivityBinding;
import org.pro.adaway.ui.ThemedActivity;
import org.pro.adaway.ui.home.HomeActivity;

/**
 * This class is a welcome activity to run first time setup on the user device.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class WelcomeActivity extends ThemedActivity {
    private WelcomePagerAdapter pagerAdapter;
    private WelcomeActivityBinding binding;
    private ImageView[] dotImageViews;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = WelcomeActivityBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());
        buildPager();
        bindBackButton();
        bindNextButton();
        bindDots();
    }

    private void buildPager() {
        this.pagerAdapter = new WelcomePagerAdapter(this);
        this.binding.viewPager.setAdapter(this.pagerAdapter);
        this.binding.viewPager.setUserInputEnabled(false);
    }

    private void bindNextButton() {
        this.binding.nextButton.setOnClickListener(view -> goNext());
    }

    private void bindBackButton() {
        this.binding.backButton.setOnClickListener(view -> goBack());
    }

    private void bindDots() {
        this.dotImageViews = new ImageView[]{
                this.binding.dot1ImageView,
                this.binding.dot2ImageView,
                this.binding.dot3ImageView,
        };
        highlightDot(this.binding.viewPager.getCurrentItem());
        this.binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                highlightDot(position);
            }
        });
    }

    private void highlightDot(final int position) {
        for (int index = 0; index < this.dotImageViews.length; index++) {
            if (index == position) {
                this.dotImageViews[index].setImageResource(R.drawable.dot);
                this.dotImageViews[index].animate().alpha(0.7F).scaleX(1.2F).scaleY(1.2F);
            } else {
                this.dotImageViews[index].setImageResource(R.drawable.dot_outline);
                this.dotImageViews[index].animate().alpha(0.5F).scaleX(1F).scaleY(1F);
            }
        }
    }

    @Override
    public void onBackPressed() {
        final int currentItem = this.binding.viewPager.getCurrentItem();
        if (currentItem == 0) super.onBackPressed();
        else goBack();
    }

    public void allowNext() {
        final boolean isFinalPage = this.binding.viewPager.getCurrentItem() == this.pagerAdapter.getItemCount() - 1;
        this.binding.nextButton.setText(isFinalPage ? R.string.welcome_finish_button : R.string.welcome_next_button);
        showView(this.binding.nextButton);
    }

    public void blockNext() {
        hideView(this.binding.nextButton);
    }

    private void allowBack() {
        showView(this.binding.backButton);
    }

    private void blockBack() {
        hideView(this.binding.backButton);
    }

    private void goNext() {
        int currentItem = this.binding.viewPager.getCurrentItem();
        final int count = this.pagerAdapter.getItemCount();
        if (currentItem >= count - 1) {
            startHomeActivity();
            return;
        }
        currentItem++;
        this.binding.viewPager.setCurrentItem(currentItem);
        allowBack();
        if (this.pagerAdapter.createFragment(currentItem).canGoNext()) allowNext();
        else blockNext();
    }

    private void goBack() {
        final int currentItem = this.binding.viewPager.getCurrentItem();
        if (currentItem == 0) return;
        this.binding.viewPager.setCurrentItem(currentItem - 1);
        if (currentItem <= 1) blockBack();
        allowNext();
    }

    private void startHomeActivity() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}

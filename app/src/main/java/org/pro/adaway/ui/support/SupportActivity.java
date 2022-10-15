package org.pro.adaway.ui.support;

import static android.content.Intent.ACTION_VIEW;
import static android.net.Uri.parse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.pro.adaway.databinding.SupportActivityBinding;
import org.pro.adaway.ui.ThemedActivity;

/**
 * This class is an activity for users to show their supports to the project.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class SupportActivity extends ThemedActivity {
    /**
     * The support link.
     */
    public static final Uri SUPPORT_LINK = parse("https://paypal.me/BruceBUJON");
    /**
     * The sponsorship link.
     */
    public static final Uri SPONSORSHIP_LINK = parse("https://github.com/sponsors/PerfectSlayer");

    public static void animateHeart(final ImageView heartImageView) {
        final PropertyValuesHolder growScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1F, 1.2F);
        final PropertyValuesHolder growScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1F, 1.2F);
        final Animator growAnimator = ObjectAnimator.ofPropertyValuesHolder(heartImageView, growScaleX, growScaleY);
        growAnimator.setDuration(200);
        growAnimator.setStartDelay(2000);

        final PropertyValuesHolder shrinkScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2F, 1F);
        final PropertyValuesHolder shrinkScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2F, 1F);
        final Animator shrinkAnimator = ObjectAnimator.ofPropertyValuesHolder(heartImageView, shrinkScaleX, shrinkScaleY);
        growAnimator.setDuration(400);

        final AnimatorSet animationSet = new AnimatorSet();
        animationSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                animationSet.start();
            }
        });
        animationSet.playSequentially(growAnimator, shrinkAnimator);
        animationSet.start();
    }

    public static void bindLink(@NonNull final View view, final Uri uri) {
        view.setOnClickListener(v -> {
            final Context context = view.getContext();
            context.startActivity(new Intent(ACTION_VIEW, uri));
        });
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SupportActivityBinding activityBinding = SupportActivityBinding.inflate(getLayoutInflater());
        setContentView(activityBinding.getRoot());

        animateHeart(activityBinding.headerImageView);

        bindLink(activityBinding.headerImageView, SUPPORT_LINK);
        bindLink(activityBinding.paypalCardView, SUPPORT_LINK);
        bindLink(activityBinding.sponsorshipCardView, SPONSORSHIP_LINK);
    }
}

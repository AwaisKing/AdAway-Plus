package org.pro.adaway.ui.home;

import static org.pro.adaway.model.adblocking.AdBlockMethod.UNDEFINED;
import static org.pro.adaway.model.adblocking.AdBlockMethod.VPN;
import static org.pro.adaway.ui.Animations.removeView;
import static org.pro.adaway.ui.Animations.showView;
import static org.pro.adaway.ui.lists.ListsActivity.ALLOWED_HOSTS_TAB;
import static org.pro.adaway.ui.lists.ListsActivity.BLOCKED_HOSTS_TAB;
import static org.pro.adaway.ui.lists.ListsActivity.REDIRECTED_HOSTS_TAB;
import static org.pro.adaway.ui.lists.ListsActivity.TAB;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.R;
import org.pro.adaway.databinding.HomeActivityBinding;
import org.pro.adaway.databinding.HomeContentBinding;
import org.pro.adaway.helper.NotificationHelper;
import org.pro.adaway.helper.PreferenceHelper;
import org.pro.adaway.model.adblocking.AdBlockMethod;
import org.pro.adaway.model.error.HostError;
import org.pro.adaway.ui.ThemedActivity;
import org.pro.adaway.ui.adblocking.InfoCardView;
import org.pro.adaway.ui.dialog.BottomSheetDrawer;
import org.pro.adaway.ui.help.HelpActivity;
import org.pro.adaway.ui.hosts.HostsSourcesActivity;
import org.pro.adaway.ui.lists.ListsActivity;
import org.pro.adaway.ui.log.LogActivity;
import org.pro.adaway.ui.prefs.PrefsActivity;
import org.pro.adaway.ui.support.SupportActivity;
import org.pro.adaway.ui.update.UpdateActivity;
import org.pro.adaway.ui.welcome.WelcomeActivity;

/**
 * This class is the application main activity.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class HomeActivity extends ThemedActivity {
    ///// http://sbc.io/hosts/alternates/gambling/hosts
    /**
     * The project link.
     */
    private static final String PROJECT_LINK = "https://github.com/AwaisKing/AdAway-Plus";

    private HomeViewModel homeViewModel;
    private HomeActivityBinding activityBinding;
    private ActivityResultLauncher<Intent> prepareVpnLauncher;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationHelper.clearUpdateNotifications(this);
        if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "Starting main activity");
        this.activityBinding = HomeActivityBinding.inflate(getLayoutInflater());
        setContentView(this.activityBinding.getRoot());

        this.homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        this.homeViewModel.isAdBlocked().observe(this, this::notifyAdBlocked);
        this.homeViewModel.getError().observe(this, this::notifyError);

        applyActionBar();
        applyBadgeShapes();
        bindAppVersion();
        bindHostCounter();
        bindSourceCounter();
        bindPending();
        bindState();
        bindClickListeners();

        this.prepareVpnLauncher = registerForActivityResult(new StartActivityForResult(), result -> {});

        if (savedInstanceState == null) checkUpdateAtStartup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkFirstStep();
    }

    private void checkFirstStep() {
        final AdBlockMethod adBlockMethod = PreferenceHelper.getAdBlockMethod(this);
        final Intent prepareIntent;
        if (adBlockMethod == UNDEFINED) {
            // Start welcome activity
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        } else if (adBlockMethod == VPN && (prepareIntent = VpnService.prepare(this)) != null) {
            // Prepare VPN
            this.prepareVpnLauncher.launch(prepareIntent);
        }
    }

    private void checkUpdateAtStartup() {
        final boolean checkAppUpdateAtStartup = PreferenceHelper.getUpdateCheckAppStartup(this);
        if (checkAppUpdateAtStartup) {
            this.homeViewModel.checkForAppUpdate();
        }
        final boolean checkUpdateAtStartup = PreferenceHelper.getUpdateCheck(this);
        if (checkUpdateAtStartup) {
            this.homeViewModel.update();
        }
    }

    private void applyActionBar() {
        setSupportActionBar(this.activityBinding.bottomBar);
    }

    private void applyBadgeShapes() {
        final HomeContentBinding content = this.activityBinding.content;

        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        final float topEdgeRound = displayMetrics.density * 23.5f;
        final float cardElevation = displayMetrics.density * 2f;

        final InfoCardView topEdge = new InfoCardView(topEdgeRound);

        content.blockedHostCardView.setShapeAppearanceModel(content.blockedHostCardView
                .getShapeAppearanceModel().toBuilder().setTopEdge(topEdge).build());
        content.blockedHostCardView.setCardElevation(cardElevation);

        content.allowedHostCardView.setShapeAppearanceModel(content.allowedHostCardView
                .getShapeAppearanceModel().toBuilder().setTopEdge(topEdge).build());
        content.allowedHostCardView.setCardElevation(cardElevation);

        content.redirectHostCardView.setShapeAppearanceModel(content.redirectHostCardView
                .getShapeAppearanceModel().toBuilder().setTopEdge(topEdge).build());
        content.redirectHostCardView.setCardElevation(cardElevation);


        content.logCardView.setShapeAppearanceModel(content.logCardView
                .getShapeAppearanceModel().toBuilder().setTopEdge(topEdge).build());
        content.logCardView.setCardElevation(cardElevation);

        content.helpCardView.setShapeAppearanceModel(content.helpCardView
                .getShapeAppearanceModel().toBuilder().setTopEdge(topEdge).build());
        content.helpCardView.setCardElevation(cardElevation);

        content.supportCardView.setShapeAppearanceModel(content.supportCardView
                .getShapeAppearanceModel().toBuilder().setTopEdge(topEdge).build());
        content.supportCardView.setCardElevation(cardElevation);

    }

    private void bindAppVersion() {
        final TextView versionTextView = this.activityBinding.content.versionTextView;
        versionTextView.setText(this.homeViewModel.getVersionName());
        versionTextView.setOnClickListener(this::showUpdate);

        this.homeViewModel.getAppManifest().observe(this, manifest -> {
            versionTextView.setClickable(manifest.updateAvailable);
            if (manifest.updateAvailable) {
                versionTextView.setTypeface(versionTextView.getTypeface(), Typeface.BOLD);
                versionTextView.setText(R.string.update_available);
            }
        });
    }

    private void bindHostCounter() {
        final Function<Integer, CharSequence> stringMapper = count -> Integer.toString(count);

        final TextView blockedHostCountTextView = this.activityBinding.content.blockedHostCounterTextView;
        final TextView allowedHostCountTextView = this.activityBinding.content.allowedHostCounterTextView;
        final TextView redirectHostCountTextView = this.activityBinding.content.redirectHostCounterTextView;

        final LiveData<Integer> blockedHostCount = this.homeViewModel.getBlockedHostCount();
        final LiveData<Integer> allowedHostCount = this.homeViewModel.getAllowedHostCount();
        final LiveData<Integer> redirectHostCount = this.homeViewModel.getRedirectHostCount();

        Transformations.map(blockedHostCount, stringMapper).observe(this, blockedHostCountTextView::setText);
        Transformations.map(allowedHostCount, stringMapper).observe(this, allowedHostCountTextView::setText);
        Transformations.map(redirectHostCount, stringMapper).observe(this, redirectHostCountTextView::setText);
    }

    private void bindSourceCounter() {
        final Resources resources = getResources();

        final TextView upToDateSourcesTextView = this.activityBinding.content.upToDateSourcesTextView;
        final LiveData<Integer> upToDateSourceCount = this.homeViewModel.getUpToDateSourceCount();
        upToDateSourceCount.observe(this, count ->
                upToDateSourcesTextView.setText(resources.getQuantityString(R.plurals.up_to_date_source_label, count, count))
        );

        final TextView outdatedSourcesTextView = this.activityBinding.content.outdatedSourcesTextView;
        final LiveData<Integer> outdatedSourceCount = this.homeViewModel.getOutdatedSourceCount();
        outdatedSourceCount.observe(this, count ->
                outdatedSourcesTextView.setText(resources.getQuantityString(R.plurals.outdated_source_label, count, count))
        );
    }

    private void bindPending() {
        this.homeViewModel.getPending().observe(this, pending -> {
            if (pending) {
                showView(this.activityBinding.content.sourcesProgressBar);
                showView(this.activityBinding.content.stateTextView);
            } else {
                removeView(this.activityBinding.content.sourcesProgressBar);
            }
        });
    }

    private void bindState() {
        this.homeViewModel.getState().observe(this, text -> {
            this.activityBinding.content.stateTextView.setText(text);
            if (text.isEmpty()) {
                removeView(this.activityBinding.content.stateTextView);
            } else {
                showView(this.activityBinding.content.stateTextView);
            }
        });
    }

    private void bindClickListeners() {
        this.activityBinding.bottomBar.setNavigationOnClickListener(v -> new BottomSheetDrawer().setHomeActivity(this).show());
        this.activityBinding.content.blockedHostCardView.setOnClickListener(v -> startHostListActivity(BLOCKED_HOSTS_TAB));
        this.activityBinding.content.allowedHostCardView.setOnClickListener(v -> startHostListActivity(ALLOWED_HOSTS_TAB));
        this.activityBinding.content.redirectHostCardView.setOnClickListener(v -> startHostListActivity(REDIRECTED_HOSTS_TAB));
        this.activityBinding.content.sourcesCardView.setOnClickListener(this::startHostsSourcesActivity);
        this.activityBinding.content.checkForUpdate.setOnClickListener(v -> this.homeViewModel.update());
        this.activityBinding.content.updateSources.setOnClickListener(v -> this.homeViewModel.sync());
        this.activityBinding.content.logCardView.setOnClickListener(this::startDnsLogActivity);
        this.activityBinding.content.helpCardView.setOnClickListener(this::startHelpActivity);
        this.activityBinding.content.supportCardView.setOnClickListener(this::showSupportActivity);
        this.activityBinding.fab.setOnClickListener(v -> this.homeViewModel.toggleAdBlocking());
    }

    /**
     * Start hosts lists activity.
     *
     * @param tab The tab to show.
     */
    private void startHostListActivity(final int tab) {
        startActivity(new Intent(this, ListsActivity.class).putExtra(TAB, tab));
    }

    /**
     * Start hosts source activity.
     *
     * @param view The event source view.
     */
    private void startHostsSourcesActivity(final View view) {
        startActivity(new Intent(this, HostsSourcesActivity.class));
    }

    /**
     * Start help activity.
     *
     * @param view The source event view.
     */
    private void startHelpActivity(final View view) {
        startActivity(new Intent(this, HelpActivity.class));
    }

    /**
     * Show development project page.
     */
    public void showProjectPage() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_LINK)));
    }

    /**
     * Show support activity.
     *
     * @param view The source event view.
     */
    private void showSupportActivity(final View view) {
        startActivity(new Intent(this, SupportActivity.class));
    }

    /**
     * Start preferences activity.
     */
    public void startPrefsActivity() {
        startActivity(new Intent(this, PrefsActivity.class));
    }

    /**
     * Start DNS log activity.
     *
     * @param view The source event view.
     */
    private void startDnsLogActivity(final View view) {
        startActivity(new Intent(this, LogActivity.class));
    }

    private void showUpdate(final View view) {
        startActivity(new Intent(this, UpdateActivity.class));
    }

    private void notifyAdBlocked(final boolean adBlocked) {
        final int color = adBlocked ? ResourcesCompat.getColor(getResources(), R.color.primary, getTheme()) : Color.GRAY;
        this.activityBinding.content.headerFrameLayout.setBackgroundColor(color);
        this.activityBinding.fab.setImageResource(adBlocked ? R.drawable.ic_pause_24dp : R.drawable.logo);
    }

    private void notifyError(final HostError error) {
        removeView(this.activityBinding.content.stateTextView);
        if (error == null) return;

        final String message = getString(error.getDetailsKey()) + "\n\n" + getString(R.string.error_dialog_help);
        new MaterialAlertDialogBuilder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(error.getMessageKey())
                .setMessage(message)
                .setPositiveButton(R.string.button_close, (dialog, id) -> dialog.dismiss())
                .setNegativeButton(R.string.button_help, (dialog, id) -> {
                    dialog.dismiss();
                    startActivity(new Intent(this, HelpActivity.class));
                })
                .create()
                .show();
    }
}

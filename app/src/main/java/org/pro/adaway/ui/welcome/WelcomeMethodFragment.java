package org.pro.adaway.ui.welcome;

import static android.app.Activity.RESULT_OK;
import static org.pro.adaway.model.adblocking.AdBlockMethod.ROOT;
import static org.pro.adaway.model.adblocking.AdBlockMethod.UNDEFINED;
import static org.pro.adaway.model.adblocking.AdBlockMethod.VPN;
import static java.lang.Boolean.TRUE;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.VpnService;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import org.pro.adaway.R;
import org.pro.adaway.databinding.WelcomeMethodLayoutBinding;
import org.pro.adaway.helper.PreferenceHelper;

/**
 * This class is a fragment to setup the ad blocking method.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class WelcomeMethodFragment extends WelcomeFragment {
    private WelcomeMethodLayoutBinding binding;
    private ActivityResultLauncher<Intent> prepareVpnLauncher;
    @ColorInt
    private int cardColor;
    @ColorInt
    private int cardEnabledColor;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        this.binding = WelcomeMethodLayoutBinding.inflate(inflater, container, false);
        this.prepareVpnLauncher = registerForActivityResult(new StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) notifyVpnEnabled();
            else notifyVpnDisabled();
        });

        this.binding.rootCardView.setOnClickListener(this::checkRoot);
        this.binding.vpnCardView.setOnClickListener(this::enableVpnService);

        final Resources.Theme theme = requireContext().getTheme();
        final Resources resources = getResources();

        this.cardColor = ResourcesCompat.getColor(resources, R.color.cardBackground, theme);
        this.cardEnabledColor = ResourcesCompat.getColor(resources, R.color.cardEnabledBackground, theme);
        return this.binding.getRoot();
    }

    private void checkRoot(@Nullable final View view) {
        notifyVpnDisabled();
        Shell.getShell();
        if (TRUE.equals(Shell.isAppGrantedRoot())) notifyRootEnabled();
        else notifyRootDisabled(true);
    }

    private void enableVpnService(@Nullable final View view) {
        notifyRootDisabled(false);
        final Context context = getContext();
        if (context == null) return;

        // Check user authorization
        final Intent prepareIntent = VpnService.prepare(context);
        if (prepareIntent == null) notifyVpnEnabled();
        else this.prepareVpnLauncher.launch(prepareIntent);
    }

    private void notifyRootEnabled() {
        PreferenceHelper.setAbBlockMethod(requireContext(), ROOT);
        this.binding.rootCardView.setCardBackgroundColor(this.cardEnabledColor);
        this.binding.vpnCardView.setCardBackgroundColor(this.cardColor);
        allowNext();
    }

    private void notifyRootDisabled(final boolean showDialog) {
        PreferenceHelper.setAbBlockMethod(requireContext(), UNDEFINED);
        this.binding.rootCardView.setCardBackgroundColor(this.cardColor);
        this.binding.vpnCardView.setCardBackgroundColor(this.cardColor);
        if (showDialog) {
            blockNext();
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.welcome_root_missing_title)
                    .setMessage(R.string.welcome_root_missile_description)
                    .setPositiveButton(R.string.button_close, null)
                    .create()
                    .show();
        }
    }

    private void notifyVpnEnabled() {
        PreferenceHelper.setAbBlockMethod(requireContext(), VPN);
        this.binding.rootCardView.setCardBackgroundColor(this.cardColor);
        this.binding.vpnCardView.setCardBackgroundColor(this.cardEnabledColor);
        allowNext();
    }

    private void notifyVpnDisabled() {
        PreferenceHelper.setAbBlockMethod(requireContext(), UNDEFINED);
        this.binding.rootCardView.setCardBackgroundColor(this.cardColor);
        this.binding.vpnCardView.setCardBackgroundColor(this.cardColor);
        blockNext();
    }
}

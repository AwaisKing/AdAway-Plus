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
package org.pro.adaway.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import org.pro.adaway.R;
import org.pro.adaway.model.adblocking.AdBlockMethod;
import org.pro.adaway.util.Constants;
import org.pro.adaway.vpn.VpnStatus;

import java.util.Collections;
import java.util.Set;

public final class PreferenceHelper {
    private PreferenceHelper() {}

    public static int getDarkThemeMode(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        final String pref = prefs.getString(
                context.getString(R.string.pref_dark_theme_mode_key),
                context.getResources().getString(R.string.pref_dark_theme_mode_def)
        );
        switch (pref) {
            case "MODE_NIGHT_NO":
                return AppCompatDelegate.MODE_NIGHT_NO;
            case "MODE_NIGHT_YES":
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    public static boolean getUpdateCheck(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_update_check_key),
                context.getResources().getBoolean(R.bool.pref_update_check_def)
        );
    }

    public static boolean getEnableIpv6(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_enable_ipv6_key),
                context.getResources().getBoolean(R.bool.pref_enable_ipv6_def)
        );
    }

    public static boolean getUpdateCheckAppStartup(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_update_check_app_startup_key),
                context.getResources().getBoolean(R.bool.pref_update_check_app_startup_def)
        );
    }

    public static boolean getUpdateCheckAppDaily(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_update_check_app_daily_key),
                context.getResources().getBoolean(R.bool.pref_update_check_app_daily_def)
        );
    }

    public static boolean getIncludeBetaReleases(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_update_include_beta_releases_key),
                context.getResources().getBoolean(R.bool.pref_update_include_beta_releases_def)
        );
    }

    public static boolean getUpdateCheckHostsDaily(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_update_check_hosts_daily_key),
                context.getResources().getBoolean(R.bool.pref_update_check_hosts_daily_def)
        );
    }

    public static boolean getAutomaticUpdateDaily(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_automatic_update_daily_key),
                context.getResources().getBoolean(R.bool.pref_automatic_update_daily_def)
        );
    }

    public static boolean getUpdateOnlyOnWifi(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_update_only_on_wifi_key),
                context.getResources().getBoolean(R.bool.pref_update_only_on_wifi_def)
        );
    }

    public static String getRedirectionIpv4(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getString(
                context.getString(R.string.pref_redirection_ipv4_key),
                context.getString(R.string.pref_redirection_ipv4_def)
        );
    }

    public static String getRedirectionIpv6(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getString(
                context.getString(R.string.pref_redirection_ipv6_key),
                context.getString(R.string.pref_redirection_ipv6_def)
        );
    }

    public static boolean getWebServerEnabled(@NonNull final Context context) {
        final SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_webserver_enabled_key),
                context.getResources().getBoolean(R.bool.pref_webserver_enabled_def)
        );
    }

    public static boolean getWebServerIcon(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_webserver_icon_key),
                context.getResources().getBoolean(R.bool.pref_webserver_icon_def)
        );
    }

    public static AdBlockMethod getAdBlockMethod(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return AdBlockMethod.fromCode(prefs.getInt(
                context.getString(R.string.pref_ad_block_method_key),
                context.getResources().getInteger(R.integer.pref_ad_block_method_key_def)
        ));
    }

    public static void setAbBlockMethod(@NonNull final Context context, @NonNull final AdBlockMethod method) {
        final SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(context.getString(R.string.pref_ad_block_method_key), method.toCode());
        editor.apply();
    }

    public static VpnStatus getVpnServiceStatus(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return VpnStatus.fromCode(prefs.getInt(
                context.getString(R.string.pref_vpn_service_status_key),
                context.getResources().getInteger(R.integer.pref_vpn_service_status_def)
        ));
    }

    public static void setVpnServiceStatus(@NonNull final Context context, @NonNull final VpnStatus status) {
        final SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(context.getString(R.string.pref_vpn_service_status_key), status.toCode());
        editor.apply();
    }

    public static boolean getVpnServiceOnBoot(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_vpn_service_on_boot_key),
                context.getResources().getBoolean(R.bool.pref_vpn_service_on_boot_def)
        );
    }

    public static boolean getVpnWatchdogEnabled(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_vpn_watchdog_enabled_key),
                context.getResources().getBoolean(R.bool.pref_vpn_watchdog_enabled_def)
        );
    }

    public static boolean getDebugEnabled(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(
                context.getString(R.string.pref_enable_debug_key),
                context.getResources().getBoolean(R.bool.pref_enable_debug_def)
        );
    }

    public static String getVpnExcludedSystemApps(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getString(
                context.getString(R.string.pref_vpn_excluded_system_apps_key),
                context.getString(R.string.pref_vpn_excluded_system_apps_default)
        );
    }

    public static Set<String> getVpnExcludedApps(@NonNull final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getStringSet(
                context.getString(R.string.pref_vpn_excluded_user_apps_key),
                Collections.emptySet()
        );
    }

    public static void setVpnExcludedApps(@NonNull final Context context, final Set<String> excludedApplicationPackageNames) {
        final SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(context.getString(R.string.pref_vpn_excluded_user_apps_key), excludedApplicationPackageNames);
        editor.apply();
    }
}

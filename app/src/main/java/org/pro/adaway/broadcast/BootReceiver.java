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

package org.pro.adaway.broadcast;

import static org.pro.adaway.model.adblocking.AdBlockMethod.ROOT;
import static org.pro.adaway.model.adblocking.AdBlockMethod.VPN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.util.Log;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.helper.PreferenceHelper;
import org.pro.adaway.model.adblocking.AdBlockMethod;
import org.pro.adaway.util.WebServerUtils;
import org.pro.adaway.vpn.VpnServiceControls;

/**
 * This broadcast receiver is executed after boot.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "BootReceiver invoked.");

            final AdBlockMethod adBlockMethod = PreferenceHelper.getAdBlockMethod(context);
            // Start web server on boot if enabled in preferences
            if (adBlockMethod == ROOT && PreferenceHelper.getWebServerEnabled(context))
                WebServerUtils.startWebServer(context);
            else if (adBlockMethod == VPN && PreferenceHelper.getVpnServiceOnBoot(context)) {
                // Ensure VPN is prepared
                final Intent prepareIntent = VpnService.prepare(context);
                if (prepareIntent != null) context.startActivity(prepareIntent);
                // Start VPN service if enabled in preferences
                VpnServiceControls.start(context);
            }
        }
    }
}

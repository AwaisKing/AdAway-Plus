package org.pro.adaway.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.pro.adaway.BuildConfig;

/**
 * This broadcast receiver is executed at application update.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class UpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (BuildConfig.DEBUG && Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            Log.d("AWAISKING_APP", "UpdateReceiver invoked :: Application update to version " + BuildConfig.VERSION_NAME);
        }
    }
}

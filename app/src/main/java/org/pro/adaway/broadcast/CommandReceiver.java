package org.pro.adaway.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.pro.adaway.AdAwayApplication;
import org.pro.adaway.BuildConfig;
import org.pro.adaway.model.adblocking.AdBlockModel;
import org.pro.adaway.model.error.HostErrorException;
import org.pro.adaway.util.AppExecutors;

/**
 * This broadcast receiver listens to commands from broadcast.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class CommandReceiver extends BroadcastReceiver {
    /**
     * This action allows to send commands to the application. See {@link Command} for extra values.
     */
    public static final String SEND_COMMAND_ACTION = "org.pro.adaway.action.SEND_COMMAND";
    private static final AppExecutors EXECUTORS = AppExecutors.getInstance();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent != null && SEND_COMMAND_ACTION.equals(intent.getAction())) {
            final AdBlockModel adBlockModel = ((AdAwayApplication) context.getApplicationContext()).getAdBlockModel();
            final Command command = Command.readFromIntent(intent);
            if (BuildConfig.DEBUG) {
                Log.i("AWAISKING_APP", "CommandReceiver invoked with command " + command);
            }
            EXECUTORS.diskIO().execute(() -> executeCommand(adBlockModel, command));
        }
    }

    private void executeCommand(final AdBlockModel adBlockModel, final Command command) {
        try {
            switch (command) {
                case START:
                    adBlockModel.apply();
                    break;
                case STOP:
                    adBlockModel.revert();
                    break;
                case UNKNOWN:
                    if (BuildConfig.DEBUG) {
                        Log.i("AWAISKING_APP", "Failed to run an unsupported command.");
                    }
                    break;
            }
        } catch (final HostErrorException e) {
            if (BuildConfig.DEBUG) {
                Log.w("AWAISKING_APP", "Failed to apply ad block command " + command + ".", e);
            }
        }
    }
}

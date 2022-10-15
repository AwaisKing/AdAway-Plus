package org.pro.adaway.tile;

import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;

import org.pro.adaway.AdAwayApplication;
import org.pro.adaway.BuildConfig;
import org.pro.adaway.model.adblocking.AdBlockModel;
import org.pro.adaway.model.error.HostErrorException;
import org.pro.adaway.util.AppExecutors;

import java.util.concurrent.atomic.AtomicBoolean;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;

/**
 * This class is a {@link TileService} to toggle ad-blocking.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class AdBlockingTileService extends TileService {
    private final AtomicBoolean toggling = new AtomicBoolean(false);

    @Override
    public void onTileAdded() {
        boolean adBlocked = getModel().isApplied().getValue() == Boolean.TRUE;
        updateTile(adBlocked);
    }

    @Override
    public void onStartListening() {
        LiveData<Boolean> applied = getModel().isApplied();
        applied.observeForever(this::updateTile);
    }

    @Override
    public void onStopListening() {
        LiveData<Boolean> applied = getModel().isApplied();
        applied.removeObserver(this::updateTile);
    }

    @Override
    public void onClick() {
        AppExecutors.getInstance()
                .diskIO()
                .execute(this::toggleAdBlocking);
    }

    private void updateTile(boolean adBlocked) {
        Tile tile = getQsTile();
        tile.setState(adBlocked ? STATE_ACTIVE : STATE_INACTIVE);
        tile.updateTile();
    }

    private void toggleAdBlocking() {
        if (this.toggling.get()) {
            return;
        }
        AdBlockModel model = getModel();
        try {
            this.toggling.set(true);
            if (model.isApplied().getValue() == Boolean.TRUE) {
                model.revert();
            } else {
                model.apply();
            }
        } catch (HostErrorException e) {
            if (BuildConfig.DEBUG) Log.w("AWAISKING_APP", "Failed to toggle ad-blocking.", e);
        } finally {
            this.toggling.set(false);
        }
    }

    private AdBlockModel getModel() {
        return ((AdAwayApplication) getApplication()).getAdBlockModel();
    }
}

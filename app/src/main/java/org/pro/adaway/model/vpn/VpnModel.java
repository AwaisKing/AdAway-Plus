package org.pro.adaway.model.vpn;

import static org.pro.adaway.model.adblocking.AdBlockMethod.VPN;
import static org.pro.adaway.model.error.HostError.ENABLE_VPN_FAIL;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.R;
import org.pro.adaway.db.AppDatabase;
import org.pro.adaway.db.dao.HostEntryDao;
import org.pro.adaway.db.entity.HostEntry;
import org.pro.adaway.model.adblocking.AdBlockMethod;
import org.pro.adaway.model.adblocking.AdBlockModel;
import org.pro.adaway.model.error.HostErrorException;
import org.pro.adaway.vpn.VpnServiceControls;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * This class is the model to represent VPN service configuration.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class VpnModel extends AdBlockModel {
    private final HostEntryDao hostEntryDao;
    private final LruCache<String, HostEntry> blockCache;
    private final LinkedHashSet<String> logs;
    private boolean recordingLogs;
    private int requestCount;

    /**
     * Constructor.
     *
     * @param context The application context.
     */
    public VpnModel(Context context) {
        super(context);
        AppDatabase database = AppDatabase.getInstance(context);
        this.hostEntryDao = database.hostEntryDao();
        this.blockCache = new LruCache<String, HostEntry>(4 * 1024) {
            @Override
            protected HostEntry create(String key) {
                return VpnModel.this.hostEntryDao.getEntry(key);
            }
        };
        this.logs = new LinkedHashSet<>();
        this.recordingLogs = false;
        this.requestCount = 0;
        this.applied.postValue(VpnServiceControls.isRunning(context));
    }

    @Override
    public AdBlockMethod getMethod() {
        return VPN;
    }

    @Override
    public void apply() throws HostErrorException {
        // Clear cache
        this.blockCache.evictAll();
        // Start VPN
        boolean started = VpnServiceControls.start(this.context);
        this.applied.postValue(started);
        if (!started) {
            throw new HostErrorException(ENABLE_VPN_FAIL);
        }
        setState(R.string.status_vpn_configuration_updated);
    }

    @Override
    public void revert() {
        VpnServiceControls.stop(this.context);
        this.applied.postValue(false);
    }

    @Override
    public boolean isRecordingLogs() {
        return this.recordingLogs;
    }

    @Override
    public void setRecordingLogs(boolean recording) {
        this.recordingLogs = recording;
    }

    @Override
    public List<String> getLogs() {
        return new ArrayList<>(this.logs);
    }

    @Override
    public void clearLogs() {
        this.logs.clear();
    }

    /**
     * Checks host entry related to an host name.
     *
     * @param host A hostname to check.
     *
     * @return The related host entry.
     */
    public HostEntry getEntry(String host) {
        // Compute miss rate periodically
        this.requestCount++;
        if (this.requestCount >= 1000) {
            int hits = this.blockCache.hitCount();
            int misses = this.blockCache.missCount();
            double missRate = 100D * (hits + misses) / misses;
            if (BuildConfig.DEBUG)
                Log.d("AWAISKING_APP", "Host cache miss rate: " + missRate);
            this.requestCount = 0;
        }
        // Add host to logs
        if (this.recordingLogs) {
            this.logs.add(host);
        }
        // Check cache
        return this.blockCache.get(host);
    }
}

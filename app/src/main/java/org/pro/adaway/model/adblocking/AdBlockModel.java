package org.pro.adaway.model.adblocking;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.model.error.HostErrorException;
import org.pro.adaway.model.root.RootModel;
import org.pro.adaway.model.vpn.VpnModel;

import java.util.List;

/**
 * This class is the base model for all ad block model.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public abstract class AdBlockModel {
    /**
     * The application context.
     */
    protected final Context context;
    /**
     * The hosts installation status:
     * <ul>
     * <li>{@code null} if not defined,</li>
     * <li>{@code true} if hosts list is installed,</li>
     * <li>{@code false} if default hosts file.</li>
     * </ul>
     */
    protected final MutableLiveData<Boolean> applied;
    /**
     * The model state.
     */
    private final MutableLiveData<String> state;

    /**
     * Constructor.
     *
     * @param context The application context.
     */
    protected AdBlockModel(final Context context) {
        this.context = context;
        this.state = new MutableLiveData<>();
        this.applied = new MutableLiveData<>();
    }

    /**
     * Instantiate ad block model.
     *
     * @param context The application context.
     * @param method  The ad block method to get model.
     *
     * @return The instantiated model.
     */
    @NonNull
    public static AdBlockModel build(final Context context, @NonNull final AdBlockMethod method) {
        if (method == AdBlockMethod.ROOT) return new RootModel(context);
        if (method == AdBlockMethod.VPN) return new VpnModel(context);
        return new UndefinedBlockModel(context);
    }

    /**
     * Get ad block method.
     *
     * @return The ad block method of this model.
     */
    public abstract AdBlockMethod getMethod();

    /**
     * Checks if hosts list is applied.
     *
     * @return {@code true} if applied, {@code false} if default.
     */
    public LiveData<Boolean> isApplied() {
        return this.applied;
    }

    /**
     * Apply hosts list.
     *
     * @throws HostErrorException If the model configuration could not be applied.
     */
    public abstract void apply() throws HostErrorException;

    /**
     * Revert the hosts list to the default one.
     *
     * @throws HostErrorException If the model configuration could not be revert.
     */
    public abstract void revert() throws HostErrorException;

    /**
     * Get the model state.
     *
     * @return The model state.
     */
    public LiveData<String> getState() {
        return this.state;
    }

    protected void setState(@StringRes final int stateResId, final Object... details) {
        final String state = this.context.getString(stateResId, details);
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", state);
        this.state.postValue(state);
    }

    /**
     * Get whether log are recoding or not.
     *
     * @return {@code true} if logs are recoding, {@code false} otherwise.
     */
    public abstract boolean isRecordingLogs();

    /**
     * Set log recoding.
     *
     * @param recording {@code true} to record logs, {@code false} otherwise.
     */
    public abstract void setRecordingLogs(boolean recording);

    /**
     * Get logs.
     *
     * @return The logs unique and sorted by date, older first.
     */
    public abstract List<String> getLogs();

    /**
     * Clear logs.
     */
    public abstract void clearLogs();
}

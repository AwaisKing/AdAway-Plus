package org.pro.adaway.model.adblocking;

import android.content.Context;

import java.util.Collections;
import java.util.List;

/**
 * This class is a stub model when no ad block method is defined.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class UndefinedBlockModel extends AdBlockModel {
    /**
     * Constructor.
     *
     * @param context The application context.
     */
    public UndefinedBlockModel(final Context context) {
        super(context);
    }

    @Override
    public AdBlockMethod getMethod() {
        return AdBlockMethod.UNDEFINED;
    }

    @Override
    public void apply() {
        // Unsupported operation
    }

    @Override
    public void revert() {
        // Unsupported operation
    }

    @Override
    public boolean isRecordingLogs() {
        return false;
    }

    @Override
    public void setRecordingLogs(final boolean recording) {
        // Unsupported operation
    }

    @Override
    public List<String> getLogs() {
        return Collections.emptyList();
    }

    @Override
    public void clearLogs() {
        // Unsupported operation
    }
}

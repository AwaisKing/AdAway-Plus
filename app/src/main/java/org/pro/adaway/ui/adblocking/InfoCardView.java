package org.pro.adaway.ui.adblocking;

import androidx.annotation.NonNull;

import com.google.android.material.shape.EdgeTreatment;
import com.google.android.material.shape.ShapePath;

public class InfoCardView extends EdgeTreatment {
    private final float size;

    public InfoCardView(final float size) {
        this.size = size;
    }

    @Override
    public void getEdgePath(final float length, final float center, final float interpolation, @NonNull final ShapePath shapePath) {
        float circleRadius = size * interpolation;
        shapePath.lineTo(center - circleRadius, 0);
        shapePath.addArc(
                center - circleRadius, -circleRadius,
                center + circleRadius, circleRadius,
                180f, // todo was 180f
                -180f   // todo was -180f
        );
        shapePath.lineTo(length, 0);
    }
}
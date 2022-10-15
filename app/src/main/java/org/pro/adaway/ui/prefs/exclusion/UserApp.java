package org.pro.adaway.ui.prefs.exclusion;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/**
 * This class represents an installed user application to exclude / include into the VPN.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class UserApp implements Comparable<UserApp> {
    final String name;
    final CharSequence packageName;
    final Drawable icon;
    boolean excluded;

    UserApp(@NonNull final CharSequence name, final CharSequence packageName, final Drawable icon, final boolean excluded) {
        this.name = name.toString();
        this.packageName = packageName;
        this.icon = icon;
        this.excluded = excluded;
    }

    @Override
    public int compareTo(@NonNull final UserApp o) {
        return this.name.compareTo(o.name);
    }
}

package org.pro.adaway.model.update;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static android.content.pm.PackageManager.GET_SIGNATURES;
import static android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES;

import androidx.annotation.NonNull;

import org.pro.adaway.BuildConfig;

/**
 * This enumerates represents the stores to get AdAway updates.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public enum UpdateStore {
    /**
     * The official store (usually GitHub releases) with AdAway signing key.
     */
    ADAWAY("adaway", "D647FDAC42961502AC78F99919B8E1901747E8DA78FE13E1EABA688FECC4C99E"),
    /**
     * The F-Droid store with F-Droid signing key.
     */
    F_DROID("fdroid", "42203F1AC857426D1496E971DB96FBE1F88C25C9E1F895A5C98D703891292277"),
    /**
     * An unknown store.
     */
    UNKNOWN("unknown", "");
    /**
     * The store name.
     */
    public final String storeName;
    /**
     * The store singing certificate digest.
     */
    public final String sign;

    UpdateStore(final String name, final String sign) {
        this.storeName = name;
        this.sign = sign;
    }

    /**
     * Get the store of the running application.
     *
     * @param context The application context.
     *
     * @return The application store, {@link #UNKNOWN} if store can't be defined.
     */
    @SuppressLint("PackageManagerGetSignatures")
    public static UpdateStore getApkStore(@NonNull final Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final String packageName = context.getPackageName();
        final Signature[] signatures;
        try {
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? GET_SIGNING_CERTIFICATES : GET_SIGNATURES;
            final PackageInfo packageInfo = packageManager.getPackageInfo(packageName, flags);

            // Signatures are not used for security reason. Only to guess the flavor of the app.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) signatures = packageInfo.signatures;
            else signatures = packageInfo.signingInfo.getSigningCertificateHistory();
        } catch (final PackageManager.NameNotFoundException e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "Failed to get application package info.", e);
            return UpdateStore.UNKNOWN;
        }
        return UpdateStore.getFromSigns(signatures);
    }

    private static UpdateStore getFromSigns(final Signature[] signatures) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (final Signature signature : signatures) {
                md.update(signature.toByteArray());
                final String sign = bytesToHex(md.digest());
                for (final UpdateStore store : UpdateStore.values()) {
                    if (store.sign.equals(sign)) {
                        return store;
                    }
                }
            }
        } catch (final NoSuchAlgorithmException e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "SHA-256 algorithm is no supported.", e);
        }
        return UpdateStore.UNKNOWN;
    }

    private static String bytesToHex(final byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        final char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Get the store name.
     *
     * @return The store name.
     */
    public String getName() {
        return this.storeName;
    }
}

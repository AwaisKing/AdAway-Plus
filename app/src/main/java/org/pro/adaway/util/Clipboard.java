package org.pro.adaway.util;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.pro.adaway.R;

/**
 * This class manages the clipboard interactions.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public final class Clipboard {
    private Clipboard() {}

    /**
     * Copy a host into the clipboard for the user.
     *
     * @param context The application context.
     * @param host    The host to copy to the clipboard
     */
    public static void copyHostToClipboard(@NonNull final Context context, final String host) {
        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clipData = ClipData.newPlainText("Host", host);
        clipboard.setPrimaryClip(clipData);
        Toast.makeText(context, R.string.clipboard_host_copied, LENGTH_SHORT).show();
    }
}

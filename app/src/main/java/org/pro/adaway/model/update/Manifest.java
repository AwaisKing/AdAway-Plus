package org.pro.adaway.model.update;

import androidx.core.text.HtmlCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.pro.adaway.BuildConfig;

/**
 * This class is represent an application manifest.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class Manifest {
    public final String apkUrl;
    public final String version;
    public final int versionCode;
    public final CharSequence changelog;
    public final boolean updateAvailable;

    public Manifest(final String manifest) throws JSONException {
        final JSONObject manifestObject = new JSONObject(manifest);
        this.apkUrl = manifestObject.getString("apkUrl");
        this.version = manifestObject.getString("version");
        this.versionCode = manifestObject.getInt("versionCode");
        this.updateAvailable = this.versionCode > BuildConfig.VERSION_CODE;
        this.changelog = HtmlCompat.fromHtml(manifestObject.getString("changelog"), HtmlCompat.FROM_HTML_MODE_COMPACT);
    }
}

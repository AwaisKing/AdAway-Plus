package org.pro.adaway.model.backup;

import org.pro.adaway.db.entity.HostListItem;
import org.pro.adaway.db.entity.HostsSource;
import org.json.JSONException;
import org.json.JSONObject;

import static org.pro.adaway.db.entity.HostsSource.USER_SOURCE_ID;

import androidx.annotation.NonNull;

/**
 * This class defines user lists and hosts sources file format.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
final class BackupFormat {
    /*
     * Source backup format.
     */
    static final String SOURCES_KEY = "sources";
    static final String SOURCE_LABEL_ATTRIBUTE = "label";
    static final String SOURCE_URL_ATTRIBUTE = "url";
    static final String SOURCE_ENABLED_ATTRIBUTE = "enabled";
    static final String SOURCE_ALLOW_ATTRIBUTE = "allow";
    static final String SOURCE_REDIRECT_ATTRIBUTE = "redirect";
    /*
     * User source backup format.
     */
    static final String BLOCKED_KEY = "blocked";
    static final String ALLOWED_KEY = "allowed";
    static final String REDIRECTED_KEY = "redirected";
    static final String ENABLED_ATTRIBUTE = "enabled";
    static final String HOST_ATTRIBUTE = "host";
    static final String REDIRECT_ATTRIBUTE = "redirect";

    private BackupFormat() {}

    @NonNull
    static JSONObject sourceToJson(@NonNull final HostsSource source) throws JSONException {
        return new JSONObject()
                .put(SOURCE_LABEL_ATTRIBUTE, source.getLabel())
                .put(SOURCE_URL_ATTRIBUTE, source.getUrl())
                .put(SOURCE_ENABLED_ATTRIBUTE, source.isEnabled())
                .put(SOURCE_ALLOW_ATTRIBUTE, source.isAllowEnabled())
                .put(SOURCE_REDIRECT_ATTRIBUTE, source.isRedirectEnabled());
    }

    @NonNull
    static HostsSource sourceFromJson(@NonNull final JSONObject sourceObject) throws JSONException {
        final HostsSource source = new HostsSource();
        source.setLabel(sourceObject.getString(SOURCE_LABEL_ATTRIBUTE));
        final String url = sourceObject.getString(SOURCE_URL_ATTRIBUTE);
        if (!HostsSource.isValidUrl(url)) throw new JSONException("Invalid source URL: " + url);
        source.setUrl(url);
        source.setEnabled(sourceObject.getBoolean(SOURCE_ENABLED_ATTRIBUTE));
        source.setAllowEnabled(sourceObject.getBoolean(SOURCE_ALLOW_ATTRIBUTE));
        source.setRedirectEnabled(sourceObject.getBoolean(SOURCE_REDIRECT_ATTRIBUTE));
        return source;
    }

    @NonNull
    static JSONObject hostToJson(@NonNull final HostListItem host) throws JSONException {
        final JSONObject hostObject = new JSONObject();
        hostObject.put(HOST_ATTRIBUTE, host.getHost());
        final String redirection = host.getRedirection();
        if (redirection != null && !redirection.isEmpty()) hostObject.put(REDIRECT_ATTRIBUTE, redirection);
        hostObject.put(ENABLED_ATTRIBUTE, host.isEnabled());
        return hostObject;
    }

    @NonNull
    static HostListItem hostFromJson(@NonNull final JSONObject hostObject) throws JSONException {
        final HostListItem host = new HostListItem();
        host.setHost(hostObject.getString(HOST_ATTRIBUTE));
        if (hostObject.has(REDIRECT_ATTRIBUTE)) host.setRedirection(hostObject.getString(REDIRECT_ATTRIBUTE));
        host.setEnabled(hostObject.getBoolean(ENABLED_ATTRIBUTE));
        host.setSourceId(USER_SOURCE_ID);
        return host;
    }
}

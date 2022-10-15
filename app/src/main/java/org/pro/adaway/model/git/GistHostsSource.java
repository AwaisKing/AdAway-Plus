package org.pro.adaway.model.git;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.pro.adaway.BuildConfig;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeParseException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * This class is an utility class to get information from GitHub gist hosting.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class GistHostsSource extends GitHostsSource {
    /**
     * The gist identifier.
     */
    private final String gistIdentifier;

    /**
     * Constructor.
     *
     * @param url The hosts file URL hosted on GitHub gist.
     *
     * @throws MalformedURLException If the URl is not a gist URL.
     */
    GistHostsSource(final String url) throws MalformedURLException {
        // Check URL path
        final URL parsedUrl = new URL(url);
        final String path = parsedUrl.getPath();
        final String[] pathParts = path.split("/");
        if (pathParts.length < 2)
            throw new MalformedURLException("The GitHub gist URL " + url + " is not valid.");
        // Extract gist identifier from path
        this.gistIdentifier = pathParts[2];
    }

    @Override
    @Nullable
    public ZonedDateTime getLastUpdate() {
        // Create commit API request URL
        final String commitApiUrl = "https://api.github.com/gists/" + this.gistIdentifier;
        // Create client and request
        final OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder().url(commitApiUrl).build();
        try (final Response execute = client.newCall(request).execute();
             final ResponseBody body = execute.body()) {
            return parseJsonBody(Objects.requireNonNull(body).string());
        } catch (final IOException | JSONException exception) {
            if (BuildConfig.DEBUG)
                Log.e("AWAISKING_APP", "Unable to get commits from API.", exception);
            // Return failed
            return null;
        }
    }

    @Nullable
    private ZonedDateTime parseJsonBody(final String body) throws JSONException {
        final JSONObject gistObject = new JSONObject(body);
        final String dateString = gistObject.getString("updated_at");
        ZonedDateTime date = null;
        try {
            date = ZonedDateTime.parse(dateString);
        } catch (final DateTimeParseException exception) {
            if (BuildConfig.DEBUG)
                Log.e("AWAISKING_APP", "Failed to parse commit date: " + dateString + ".", exception);
        }
        return date;
    }
}

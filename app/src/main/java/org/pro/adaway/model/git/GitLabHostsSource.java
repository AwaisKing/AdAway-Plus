package org.pro.adaway.model.git;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pro.adaway.BuildConfig;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import awaisome.compat.StringJoiner;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * This class is an utility class to get information from GitLab hosts source hosting.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class GitLabHostsSource extends GitHostsSource {
    /**
     * The GitHub owner name.
     */
    private final String owner;
    /**
     * The GitHub repository name.
     */
    private final String repo;
    /**
     * The GitLab reference name.
     */
    private final String ref;
    /**
     * The GitLab (hosts) file path.
     */
    private final String path;

    GitLabHostsSource(final String url) throws MalformedURLException {
        // Check URL path
        final URL parsedUrl = new URL(url);
        final String path = parsedUrl.getPath();
        final String[] pathParts = path.split("/");
        if (pathParts.length < 5)
            throw new MalformedURLException("The GitLab user content URL " + url + " is not valid.");

        // Extract components from path
        this.owner = pathParts[1];
        this.repo = pathParts[2];
        this.ref = pathParts[4];
        final StringJoiner joiner = new StringJoiner("/");
        long toSkip = 5;
        for (final String pathPart : pathParts) {
            if (toSkip > 0) {
                toSkip--;
                continue;
            }
            joiner.add(pathPart);
        }
        this.path = joiner.toString();
    }

    /**
     * Get last update of the hosts file.
     *
     * @return The last update date, {@code null} if the date could not be retrieved.
     */
    @Override
    @Nullable
    public ZonedDateTime getLastUpdate() {
        // Create commit API request URL
        final String commitApiUrl = "https://gitlab.com/api/v4/projects/" + this.owner + "%2F" + this.repo
                + "/repository/commits?path=" + this.path + "&ref_name=" + this.ref;
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
        final JSONArray commitArray = new JSONArray(body);
        final int nbrOfCommits = commitArray.length();
        ZonedDateTime date = null;
        for (int i = 0; i < nbrOfCommits && date == null; i++) {
            final JSONObject commitItemObject = commitArray.getJSONObject(i);
            final String dateString = commitItemObject.getString("committed_date");
            try {
                date = ZonedDateTime.parse(dateString);
            } catch (final Throwable exception) {
                if (BuildConfig.DEBUG)
                    Log.w("AWAISKING_APP", "Failed to parse commit date: " + dateString + ".", exception);
            }
        }
        return date;
    }
}

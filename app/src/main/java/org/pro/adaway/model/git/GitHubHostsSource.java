package org.pro.adaway.model.git;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pro.adaway.BuildConfig;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeParseException;

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
 * This class is an utility class to get information from GitHub repository hosting.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class GitHubHostsSource extends GitHostsSource {
    /**
     * The GitHub owner name.
     */
    private final String owner;
    /**
     * The GitHub repository name.
     */
    private final String repo;
    /**
     * The GitHub blob (hosts file) path.
     */
    private final String blobPath;

    /**
     * Constructor.
     *
     * @param url The hosts file URL hosted on GitHub.
     *
     * @throws MalformedURLException If the URl is not a GitHub URL.
     */
    GitHubHostsSource(final String url) throws MalformedURLException {
        // Check URL path
        final URL parsedUrl = new URL(url);
        final String path = parsedUrl.getPath();
        final String[] pathParts = path.split("/");
        if (pathParts.length < 5)
            throw new MalformedURLException("The GitHub user content URL " + url + " is not valid.");
        // Extract components from path
        this.owner = pathParts[1];
        this.repo = pathParts[2];
        final StringJoiner joiner = new StringJoiner("/");
        long toSkip = 4;
        for (final String pathPart : pathParts) {
            if (toSkip > 0) {
                toSkip--;
                continue;
            }
            joiner.add(pathPart);
        }
        this.blobPath = joiner.toString();
    }

    @Override
    @Nullable
    public ZonedDateTime getLastUpdate() {
        // Create commit API request URL
        final String commitApiUrl = "https://api.github.com/repos/" + this.owner + "/" + this.repo
                + "/commits?per_page=1&path=" + this.blobPath;
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
            final JSONObject commitObject = commitItemObject.getJSONObject("commit");
            final JSONObject committerObject = commitObject.getJSONObject("committer");
            final String dateString = committerObject.getString("date");
            try {
                date = ZonedDateTime.parse(dateString);
            } catch (final DateTimeParseException exception) {
                if (BuildConfig.DEBUG)
                    Log.w("AWAISKING_APP", "Failed to parse commit date: " + dateString + ".", exception);
            }
        }
        return date;
    }
}

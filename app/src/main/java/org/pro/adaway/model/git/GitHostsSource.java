package org.pro.adaway.model.git;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.threeten.bp.ZonedDateTime;

import java.net.MalformedURLException;

/**
 * This class is an utility class to get information from Git hosted hosts sources.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public abstract class GitHostsSource {
    /**
     * The GitHub repository URL.
     */
    private static final String GITHUB_REPO_URL = "https://raw.githubusercontent.com/";
    /**
     * The GitHub gist URL.
     */
    private static final String GITHUB_GIST_URL = "https://gist.githubusercontent.com";
    /**
     * The GitLab URL.
     */
    private static final String GITLAB_URL = "https://gitlab.com/";

    /**
     * Check if a hosts file url is hosted on Git hosting.
     *
     * @param url The url to check.
     *
     * @return {@code true} if the hosts file is hosted on Git hosting, {@code false} otherwise.
     */
    public static boolean isHostedOnGit(@NonNull final String url) {
        return url.startsWith(GITHUB_REPO_URL)
                || url.startsWith(GITHUB_GIST_URL)
                || url.startsWith(GITLAB_URL);
    }

    /**
     * Get the GitHub hosts source.
     *
     * @param url The URL to get source from.
     *
     * @return The GitHub hosts source.
     *
     * @throws MalformedURLException If the URL is not a GitHub URL or not a supported GitHub URL.
     */
    @NonNull
    public static GitHostsSource getSource(@NonNull final String url) throws MalformedURLException {
        if (url.startsWith(GITHUB_REPO_URL)) return new GitHubHostsSource(url);
        if (url.startsWith(GITHUB_GIST_URL)) return new GistHostsSource(url);
        if (url.startsWith(GITLAB_URL)) return new GitLabHostsSource(url);
        throw new MalformedURLException("URL is not a supported Git hosting URL");
    }

    /**
     * Get last update of the hosts file.
     *
     * @return The last update date, {@code null} if the date could not be retrieved.
     */
    @Nullable
    public abstract ZonedDateTime getLastUpdate();
}

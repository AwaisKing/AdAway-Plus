/*
 * Copyright (C) 2011-2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This file is part of AdAway.
 *
 * AdAway is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdAway is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdAway.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.pro.adaway.util;

import static org.pro.adaway.model.root.ShellUtils.isBundledExecutableRunning;
import static org.pro.adaway.model.root.ShellUtils.killBundledExecutable;
import static org.pro.adaway.model.root.ShellUtils.runBundledExecutable;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.security.KeyChain;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.R;
import org.pro.adaway.helper.PreferenceHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;

import javax.net.ssl.SSLHandshakeException;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is an utility class to control web server execution.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class WebServerUtils {
    public static final String TEST_URL = "https://localhost/internal-test";
    private static final String WEB_SERVER_EXECUTABLE = "webserver";
    private static final String LOCALHOST_CERTIFICATE = "localhost-2108.crt";
    private static final String LOCALHOST_CERTIFICATE_KEY = "localhost-2108.key";

    /**
     * Start the web server in new thread with RootTools
     *
     * @param context The application context.
     */
    public static void startWebServer(final Context context) {
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Starting web server…");

        final okio.Path resourcePath = okio.Path.get(context.getFilesDir()).resolve(WEB_SERVER_EXECUTABLE);
        inflateResources(context, resourcePath);

        final String parameters = "--resources " + resourcePath.toFile().getAbsolutePath()
                + (PreferenceHelper.getWebServerIcon(context) ? " --icon" : "")
                + " > /dev/null 2>&1";
        runBundledExecutable(context, WEB_SERVER_EXECUTABLE, parameters);
    }

    /**
     * Stop the web server.
     */
    public static void stopWebServer() {
        killBundledExecutable(WEB_SERVER_EXECUTABLE);
    }

    /**
     * Checks if web server is running
     *
     * @return <code>true</code> if webs server is running, <code>false</code> otherwise.
     */
    public static boolean isWebServerRunning() {
        return isBundledExecutableRunning(WEB_SERVER_EXECUTABLE);
    }

    /**
     * Get the web server state description.
     *
     * @return The web server state description.
     */
    @StringRes
    public static int getWebServerState() {
        final OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(TEST_URL)
                .build();
        try (final Response response = client.newCall(request).execute()) {
            return response.isSuccessful() ?
                    R.string.pref_webserver_state_running_and_installed :
                    R.string.pref_webserver_state_not_running;
        } catch (final SSLHandshakeException e) {
            return R.string.pref_webserver_state_running_not_installed;
        } catch (final ConnectException e) {
            return R.string.pref_webserver_state_not_running;
        } catch (final IOException e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "Failed to test web server.", e);
            return R.string.pref_webserver_state_not_running;
        }
    }

    /**
     * Prompt user to install web server certificate.
     *
     * @param context The application context.
     */
    public static void installCertificate(@NonNull final Context context) {
        final AssetManager assetManager = context.getAssets();
        try (final InputStream inputStream = assetManager.open(LOCALHOST_CERTIFICATE);
             final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            final byte[] bytes = outputStream.toByteArray();
            final X509Certificate x509 = X509Certificate.getInstance(bytes);
            final Intent intent = KeyChain.createInstallIntent();
            intent.putExtra(KeyChain.EXTRA_CERTIFICATE, x509.getEncoded());
            intent.putExtra(KeyChain.EXTRA_NAME, "AdAway");
            context.startActivity(intent);
        } catch (final IOException e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "Failed to read certificate.", e);
        } catch (final CertificateException e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "Failed to parse certificate.", e);
        }
    }

    public static void copyCertificate(@NonNull final Context context, final Uri uri) {
        final ContentResolver contentResolver = context.getContentResolver();
        final AssetManager assetManager = context.getAssets();
        try (final InputStream inputStream = assetManager.open(LOCALHOST_CERTIFICATE);
             final OutputStream outputStream = contentResolver.openOutputStream(uri)) {
            if (outputStream == null) throw new IOException("Failed to open " + uri);
            final byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, read);
        } catch (final IOException e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "Failed to copy certificate.", e);
        }
    }

    private static void inflateResources(@NonNull final Context context, final okio.Path target) {
        final AssetManager assetManager = context.getAssets();
        try {
            inflateResource(assetManager, LOCALHOST_CERTIFICATE, target);
            inflateResource(assetManager, LOCALHOST_CERTIFICATE_KEY, target);
            inflateResource(assetManager, "icon.svg", target);
            inflateResource(assetManager, "test.html", target);
        } catch (final IOException e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "Failed to inflate web server resources.", e);
        }
    }

    private static void inflateResource(final AssetManager assetManager, final String resource, @NonNull final okio.Path target) throws IOException {
        final File file = target.toFile();
        if (!file.isDirectory()) // noinspection ResultOfMethodCallIgnored
            file.mkdirs();

        final okio.Path targetFile = target.resolve(resource);
        final File fileTarget = targetFile.toFile();
        if (!fileTarget.isFile()) {
            try (final InputStream inputStream = assetManager.open(resource); final FileOutputStream outputStream = new FileOutputStream(fileTarget)) {
                final byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, read);
            }
        }
    }
}

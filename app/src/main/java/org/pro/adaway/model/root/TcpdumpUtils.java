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

package org.pro.adaway.model.root;

import static org.pro.adaway.model.root.ShellUtils.isBundledExecutableRunning;
import static org.pro.adaway.model.root.ShellUtils.killBundledExecutable;
import static org.pro.adaway.model.root.ShellUtils.mergeAllLines;
import static org.pro.adaway.model.root.ShellUtils.runBundledExecutable;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import org.pro.adaway.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okio.BufferedSource;
import okio.Okio;

class TcpdumpUtils {
    private static final String TCPDUMP_EXECUTABLE = "tcpdump";
    private static final String TCPDUMP_LOG = "dns_log.txt";
    private static final String TCPDUMP_HOSTNAME_REGEX = "(?:A\\?|AAAA\\?)\\s(\\S+)\\.\\s";
    private static final Pattern TCPDUMP_HOSTNAME_PATTERN = Pattern.compile(TCPDUMP_HOSTNAME_REGEX);

    /**
     * Private constructor.
     */
    private TcpdumpUtils() {}

    /**
     * Checks if tcpdump is running
     *
     * @return true if tcpdump is running
     */
    static boolean isTcpdumpRunning() {
        return isBundledExecutableRunning(TCPDUMP_EXECUTABLE);
    }

    /**
     * Start tcpdump tool.
     *
     * @param context The application context.
     *
     * @return returns true if starting worked
     */
    static boolean startTcpdump(final Context context) {
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Starting tcpdump...");
        checkSystemTcpdump();

        final File file = getLogFile(context);
        try {
            // Create log file before using it with tcpdump if not exists
            if (!file.exists() && !file.createNewFile()) return false;
        } catch (final IOException e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "Problem while getting cache directory!", e);
            return false;
        }

        // "-i any": listen on any network interface
        // "-p": disable promiscuous mode (doesn't work anyway)
        // "-l": Make stdout line buffered. Useful if you want to see the data while
        // capturing it.
        // "-v": verbose
        // "-t": don't print a timestamp
        // "-s 0": capture first 512 bit of packet to get DNS content
        final String parameters = "-i any -p -l -v -t -s 512 'udp dst port 53' >> " + file + " 2>&1";

        return runBundledExecutable(context, TCPDUMP_EXECUTABLE, parameters);
    }

    /**
     * Stop tcpdump.
     */
    static void stopTcpdump() {
        killBundledExecutable(TCPDUMP_EXECUTABLE);
    }

    /**
     * Check if tcpdump binary in bundled in the system.
     */
    static void checkSystemTcpdump() {
        try {
            final Shell.Result result = Shell.cmd("tcpdump --version").exec();
            final int exitCode = result.getCode();
            final String output = mergeAllLines(result.getOut());
            if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "Tcpdump " + (exitCode == 0 ? "present"
                    : "missing (" + exitCode + ")") + "\n" + output);
        } catch (final Exception exception) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "Failed to check system tcpdump binary.", exception);
        }
    }

    /**
     * Get the tcpdump log file.
     *
     * @param context The application context.
     *
     * @return The tcpdump log file.
     */
    @NonNull
    static File getLogFile(@NonNull final Context context) {
        return new File(context.getCacheDir(), TCPDUMP_LOG);
    }

    /**
     * Get the tcpdump log content.
     *
     * @param context The application context.
     *
     * @return The tcpdump log file content.
     */
    @NonNull
    static List<String> getLogs(final Context context) {
        final File logFile = getLogFile(context);

        // Check if the log file exists
        if (!logFile.exists()) return Collections.emptyList();

        try (final BufferedSource buffer = Okio.buffer(Okio.source(logFile))) {
            final ArrayList<String> allLines = new ArrayList<>(0);
            while (true) {
                final String line = buffer.readUtf8Line();
                if (line == null) break;
                allLines.add(line);
            }

            final ArrayList<String> list = new ArrayList<>(0);
            final Set<String> uniqueValues = new HashSet<>(0, 0.95f);
            for (final String allLine : allLines) {
                final String tcpdumpHostname = getTcpdumpHostname(allLine);
                if (tcpdumpHostname != null && uniqueValues.add(tcpdumpHostname))
                    list.add(tcpdumpHostname);
            }

            return list;
        } catch (final IOException exception) {
            if (BuildConfig.DEBUG)
                Log.e("AWAISKING_APP", "Can not get cache directory.", exception);
            return Collections.emptyList();
        }
    }

    /**
     * Delete log file of tcpdump.
     *
     * @param context The application context.
     */
    static boolean clearLogFile(final Context context) {
        // Get the log file
        final File file = getLogFile(context);
        // Check if file exists
        if (!file.exists()) return true;
        // Truncate the file content

        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            // Only truncate the file
            try {
                outputStream.flush();
            } catch (final Throwable e) {
                // ignore
            }
        } catch (final IOException exception) {
            if (BuildConfig.DEBUG)
                Log.e("AWAISKING_APP", "Error while truncating the tcpdump file!", exception);
            // Return failed to clear the log file
            return false;
        }
        // Return successfully clear the log file
        return true;
    }

    /**
     * Gets hostname out of tcpdump log line.
     *
     * @param input One line from dns log.
     *
     * @return A hostname or {code null} if no DNS query in the input.
     */
    @Nullable
    private static String getTcpdumpHostname(final String input) {
        final Matcher tcpdumpHostnameMatcher = TCPDUMP_HOSTNAME_PATTERN.matcher(input);
        if (tcpdumpHostnameMatcher.find()) return tcpdumpHostnameMatcher.group(1);
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Does not find: " + input);
        return null;
    }
}

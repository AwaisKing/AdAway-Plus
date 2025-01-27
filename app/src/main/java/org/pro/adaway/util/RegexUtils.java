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

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;

import org.pro.adaway.BuildConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {
    private static final Pattern WILDCARD_PATTERN = Pattern.compile("[*?]");

    /**
     * Check whether a hostname is valid.
     *
     * @param hostname The hostname to validate.
     *
     * @return return {@code true} if hostname is valid, {@code false} otherwise.
     */
    public static boolean isValidHostname(final String hostname) {
        return InternetDomainName.isValid(hostname);
    }

    /**
     * Check whether a wildcard hostname is valid.
     * Wildcard hostname is an hostname with one of the following wildcard:
     * <ul>
     * <li>{@code *} for any character sequence,</li>
     * <li>{@code ?} for any character</li>
     * </ul>
     * <p/>
     * Wildcard validation is quite tricky, because wildcards can be placed anywhere and can match with
     * anything. To make sure we don't dismiss certain valid wildcard host names, we trim wildcards
     * or replace them with an alphanumeric character for further validation.<br/>
     * We only reject whitelist host names which cannot match against valid host names under any circumstances.
     *
     * @param hostname The wildcard hostname to validate.
     *
     * @return return {@code true} if wildcard hostname is valid, {@code false} otherwise.
     */
    public static boolean isValidWildcardHostname(final String hostname) {
        // Clear wildcards from host name then validate it
        final Matcher matcher = WILDCARD_PATTERN.matcher(hostname);
        final String clearedHostname = matcher.replaceAll("");
        // Replace wildcards from host name by an alphanumeric character
        final String replacedHostname = matcher.replaceAll("a");
        // Check if any hostname is valid
        return isValidHostname(clearedHostname) || isValidHostname(replacedHostname);
    }

    /**
     * Check if an IP address is valid.
     *
     * @param ip The IP to validate.
     *
     * @return {@code true} if the IP is valid, {@code false} otherwise.
     */
    public static boolean isValidIP(final String ip) {
        try {
            InetAddresses.forString(ip);
            return true;
        } catch (final IllegalArgumentException exception) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "Invalid IP address: " + ip, exception);
            return false;
        }
    }

    /**
     * Transforms String with * and ? characters to regex String, convert "example*.*" to regex
     * "^example.*\\..*$", from <a href="http://www.rgagnon.com/javadetails/java-0515.html">http://www.rgagnon.com/javadetails/java-0515.html</a>
     */
    @NonNull
    public static String wildcardToRegex(@NonNull final String wildcard) {
        final StringBuilder regex = new StringBuilder(wildcard.length());
        regex.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            final char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                // escape special regex-characters
                case '(':
                case ')':
                case '[':
                case ']':
                case '$':
                case '^':
                case '.':
                case '{':
                case '}':
                case '|':
                case '\\':
                    regex.append("\\");
                    regex.append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }
        regex.append('$');
        return regex.toString();
    }
}

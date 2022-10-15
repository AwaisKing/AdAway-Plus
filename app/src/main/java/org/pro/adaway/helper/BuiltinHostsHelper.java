package org.pro.adaway.helper;

import android.content.Context;

import org.pro.adaway.R;
import org.pro.adaway.db.dao.HostsSourceDao;
import org.pro.adaway.db.entity.HostsSource;

import java.io.Serializable;

public class BuiltinHostsHelper {
    private static final BuiltinHostHolder[] builtinHosts = {
            // AdAway official
            new BuiltinHostHolder(R.string.hosts_adaway_source, "https://adaway.org/hosts.txt"),
            // StevenBlack
            new BuiltinHostHolder(R.string.hosts_stevenblack_source, "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"),
            // Pete Lowe
            new BuiltinHostHolder(R.string.hosts_peterlowe_source, "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext"),
            // badmojr 1Hosts
            new BuiltinHostHolder(R.string.hosts_badmojr_source, "https://raw.githubusercontent.com/badmojr/1Hosts/master/Lite/hosts.txt"),
    };

    private BuiltinHostsHelper() {}

    public static void insertBuiltinHostsInDAO(final Context context, final HostsSourceDao hostsSourceDao) {
        for (final BuiltinHostHolder builtinHost : builtinHosts) {
            final HostsSource hostsSource = new HostsSource();
            hostsSource.setLabel(context.getString(builtinHost.hostLabel));
            hostsSource.setUrl(builtinHost.hostUrl);
            hostsSourceDao.insert(hostsSource);
        }
    }

    private static class BuiltinHostHolder implements Serializable {
        private static final long serialVersionUID = 8784454691997632892L;
        private final String hostUrl;
        private final int hostLabel;

        public BuiltinHostHolder(final int hostLabel, final String hostUrl) {
            this.hostLabel = hostLabel;
            this.hostUrl = hostUrl;
        }
    }
}
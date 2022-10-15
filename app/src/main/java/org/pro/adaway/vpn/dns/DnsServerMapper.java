package org.pro.adaway.vpn.dns;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_VPN;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static java.util.Collections.emptyList;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.VpnService;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;

import androidx.annotation.NonNull;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.helper.PreferenceHelper;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import awaisome.compat.StringJoiner;
import awaisome.compat.Optional;

/**
 * This class is in charge of mapping DNS server addresses between network DNS and fake DNS.
 * <p>
 * Fake DNS addresses are registered as VPN interface DNS to capture DNS traffic.
 * Each original DNS server is directly mapped to one fake address.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class DnsServerMapper {
    /**
     * The TEST NET addresses blocks, defined in RFC5735.
     */
    private static final String[] TEST_NET_ADDRESS_BLOCKS = {
            "192.0.2.0/24", // TEST-NET-1
            "198.51.100.0/24", // TEST-NET-2
            "203.0.113.0/24" // TEST-NET-3
    };
    /**
     * This IPv6 address prefix for documentation, defined in RFC3849.
     */
    private static final String IPV6_ADDRESS_PREFIX_RESERVED_FOR_DOCUMENTATION = "2001:db8::/32";
    /**
     * VPN network IPv6 interface prefix length.
     */
    private static final int IPV6_PREFIX_LENGTH = 120;
    /**
     * The original DNS servers.
     */
    private final List<InetAddress> dnsServers;

    /**
     * Constructor.
     */
    public DnsServerMapper() {
        this.dnsServers = new ArrayList<>(0);
    }

    /**
     * Configure the VPN.
     * <p>
     * Add interface address per IP family and fake DNS server per system DNS server.
     *
     * @param context The application context.
     * @param builder The builder of the VPN to configure.
     */
    public void configureVpn(final Context context, final VpnService.Builder builder) {
        // Get DNS servers
        final List<InetAddress> dnsServers = getNetworkDnsServers(context);
        // Configure tunnel network address
        final Subnet ipv4Subnet = addIpv4Address(builder);
        final Subnet ipv6Subnet = hasIpV6DnsServers(context, dnsServers) ? addIpv6Address(builder) : null;
        // Configure DNS mapping
        this.dnsServers.clear();
        for (final InetAddress dnsServer : dnsServers) {
            final Subnet subnetForDnsServer = dnsServer instanceof Inet4Address ? ipv4Subnet : ipv6Subnet;
            if (subnetForDnsServer == null) continue;
            this.dnsServers.add(dnsServer);
            final int serverIndex = this.dnsServers.size();
            final InetAddress dnsAddressAlias = subnetForDnsServer.getAddress(serverIndex);
            if (BuildConfig.DEBUG)
                Log.i("AWAISKING_APP", "Mapping DNS server " + dnsServer + " as " + dnsAddressAlias);
            builder.addDnsServer(dnsAddressAlias);
            if (dnsServer instanceof Inet4Address)
                builder.addRoute(dnsAddressAlias, 32);
        }
    }

    public InetAddress getDefaultDnsServerAddress() {
        if (this.dnsServers.isEmpty()) {
            try {
                return InetAddress.getByName("1.1.1.1");
            } catch (final UnknownHostException e) {
                throw new IllegalStateException("Failed to parse hardcoded DNS IP address.", e);
            }
        }
        // Return last DNS server added
        return this.dnsServers.get(this.dnsServers.size() - 1);
    }

    /**
     * Get the original DNS server address from fake DNS server address.
     *
     * @param fakeDnsAddress The fake DNS address to get the original DNS server address.
     *
     * @return The original DNS server address, wrapped into an {@link Optional} or {@link Optional#empty()} if it does not exists.
     */
    Optional<InetAddress> getDnsServerFromFakeAddress(@NonNull final InetAddress fakeDnsAddress) {
        final byte[] address = fakeDnsAddress.getAddress();
        final int index = address[address.length - 1] - 2;
        if (index < 0 || index >= this.dnsServers.size()) return Optional.empty();
        final InetAddress dnsAddress = this.dnsServers.get(index);
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "handleDnsRequest: Incoming packet to " +
                fakeDnsAddress.getHostAddress() + " AKA " + index + " AKA " + dnsAddress.getHostAddress());
        return Optional.of(dnsAddress);
    }

    /**
     * Get the DNS server addresses from the device networks.
     *
     * @param context The application context.
     *
     * @return The DNS server addresses, an empty collection if no network.
     */
    private List<InetAddress> getNetworkDnsServers(@NonNull final Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        dumpNetworkInfo(connectivityManager);
        final Network activeNetwork = VERSION.SDK_INT >= Build.VERSION_CODES.M ? connectivityManager.getActiveNetwork() : null;
        if (activeNetwork == null) return getAnyNonVpnNetworkDns(connectivityManager);
        if (isNotVpnNetwork(connectivityManager, activeNetwork)) {
            if (BuildConfig.DEBUG)
                Log.d("AWAISKING_APP", "Get DNS servers from active network " + activeNetwork);
            return getNetworkDnsServers(connectivityManager, activeNetwork);
        }
        return getDnsFromNonVpnNetworkWithMatchingTransportType(connectivityManager, activeNetwork);
    }

    /**
     * Dump all network properties to logs.
     *
     * @param connectivityManager The connectivity manager.
     */
    private void dumpNetworkInfo(final ConnectivityManager connectivityManager) {
        if (!BuildConfig.DEBUG || VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        Log.d("AWAISKING_APP", "Dumping network and dns configuration:");

        final Network activeNetwork = connectivityManager.getActiveNetwork();
        for (final Network network : connectivityManager.getAllNetworks()) {
            final NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            final boolean cellular = networkCapabilities != null && networkCapabilities.hasTransport(TRANSPORT_CELLULAR);
            final boolean wifi = networkCapabilities != null && networkCapabilities.hasTransport(TRANSPORT_WIFI);
            final boolean vpn = networkCapabilities != null && networkCapabilities.hasTransport(TRANSPORT_VPN);
            final LinkProperties linkProperties = connectivityManager.getLinkProperties(network);

            final String dnsList;
            if (linkProperties == null) dnsList = "none";
            else {
                final StringJoiner joiner = new StringJoiner(", ");
                for (final InetAddress inetAddress : linkProperties.getDnsServers())
                    joiner.add(inetAddress.toString());
                dnsList = joiner.toString();
            }

            Log.d("AWAISKING_APP", "Network " + network + " " + (network.equals(activeNetwork) ? "[default]" : "[other]")
                    + ": " + (cellular ? "cellular" : "") + "" + (wifi ? "WiFi" : "") + (vpn ? " VPN" : "")
                    + " with dns " + dnsList);
        }
    }

    /**
     * Get the DNS server addresses of any network without VPN capability.
     *
     * @param connectivityManager The connectivity manager.
     *
     * @return The DNS server addresses, an empty collection if no applicable DNS server found.
     */
    @NonNull
    private List<InetAddress> getAnyNonVpnNetworkDns(@NonNull final ConnectivityManager connectivityManager) {
        for (final Network network : connectivityManager.getAllNetworks()) {
            if (isNotVpnNetwork(connectivityManager, network)) {
                final List<InetAddress> dnsServers = getNetworkDnsServers(connectivityManager, network);
                if (!dnsServers.isEmpty()) {
                    if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Get DNS servers from non VPN network " + network);
                    return dnsServers;
                }
            }
        }
        return emptyList();
    }

    /**
     * Get the DNS server addresses of a network with the same transport type as the active network except VPN.
     *
     * @param connectivityManager The connectivity manager.
     * @param activeNetwork       The active network to filter similar transport type.
     *
     * @return The DNS server addresses, an empty collection if no applicable DNS server found.
     */
    @NonNull
    private List<InetAddress> getDnsFromNonVpnNetworkWithMatchingTransportType(@NonNull final ConnectivityManager connectivityManager,
                                                                               final Network activeNetwork) {
        // Get active network transport
        final NetworkCapabilities activeNetworkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (activeNetworkCapabilities == null) return emptyList();
        int activeNetworkTransport = -1;
        if (activeNetworkCapabilities.hasTransport(TRANSPORT_CELLULAR)) activeNetworkTransport = TRANSPORT_CELLULAR;
        else if (activeNetworkCapabilities.hasTransport(TRANSPORT_WIFI)) activeNetworkTransport = TRANSPORT_WIFI;
        // Check all network to find one without VPN and matching transport
        for (final Network network : connectivityManager.getAllNetworks()) {
            final NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            if (networkCapabilities == null) continue;
            if (networkCapabilities.hasTransport(activeNetworkTransport) && !networkCapabilities.hasTransport(TRANSPORT_VPN)) {
                final List<InetAddress> dns = getNetworkDnsServers(connectivityManager, network);
                if (!dns.isEmpty()) {
                    if (BuildConfig.DEBUG)
                        Log.d("AWAISKING_APP", "Get DNS servers from non VPN matching type network " + network);
                    return dns;
                }
            }
        }
        return emptyList();
    }

    /**
     * Get the DNS server addresses of a network.
     *
     * @param connectivityManager The connectivity manager.
     * @param network             The network to get DNS server addresses.
     *
     * @return The DNS server addresses, an empty collection if no network.
     */
    private List<InetAddress> getNetworkDnsServers(@NonNull final ConnectivityManager connectivityManager, final Network network) {
        final LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        if (linkProperties == null) return emptyList();
        return linkProperties.getDnsServers();
    }

    /**
     * Check a network does not have VPN transport.
     *
     * @param connectivityManager The connectivity manager.
     * @param network             The network to check.
     *
     * @return <code>true</code> if a network is not a VPN, <code>false</code> otherwise.
     */
    private boolean isNotVpnNetwork(final ConnectivityManager connectivityManager, final Network network) {
        if (network == null) return false;
        final NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
        return networkCapabilities != null && !networkCapabilities.hasTransport(TRANSPORT_VPN);
    }

    /**
     * Add IPv4 network address to the VPN.
     *
     * @param builder The build of the VPN to configure.
     *
     * @return The IPv4 address of the VPN network.
     */
    @NonNull
    private Subnet addIpv4Address(final VpnService.Builder builder) {
        for (final String addressBlock : TEST_NET_ADDRESS_BLOCKS) {
            try {
                final Subnet subnet = Subnet.parse(addressBlock);
                final InetAddress address = subnet.getAddress(0);
                builder.addAddress(address, subnet.prefixLength);
                if (BuildConfig.DEBUG)
                    Log.d("AWAISKING_APP", "Set " + address + " as IPv4 network address to tunnel interface.");
                return subnet;
            } catch (final IllegalArgumentException e) {
                if (BuildConfig.DEBUG)
                    Log.w("AWAISKING_APP", "Failed to add " + addressBlock + " network address to tunnel interface.", e);
            }
        }
        throw new IllegalStateException("Failed to add any IPv4 address for TEST-NET to tunnel interface.");
    }

    /**
     * Add IPv6 network address to the VPN.
     *
     * @param builder The build of the VPN to configure.
     *
     * @return The IPv4 address of the VPN network.
     */
    @NonNull
    private Subnet addIpv6Address(@NonNull final VpnService.Builder builder) {
        final Subnet subnet = Subnet.parse(IPV6_ADDRESS_PREFIX_RESERVED_FOR_DOCUMENTATION);
        builder.addAddress(subnet.address, IPV6_PREFIX_LENGTH);
        if (BuildConfig.DEBUG)
            Log.d("AWAISKING_APP", "Set " + subnet.address + " as IPv6 network address to tunnel interface.");
        return subnet;
    }

    private boolean hasIpV6DnsServers(final Context context, @NonNull final Collection<InetAddress> dnsServers) {
        boolean hasIpv6Server = false;
        for (final InetAddress server : dnsServers) {
            if (server instanceof Inet6Address) {
                hasIpv6Server = true;
                break;
            }
        }

        final boolean hasOnlyOnServer = dnsServers.size() == 1;
        final boolean isIpv6Enabled = PreferenceHelper.getEnableIpv6(context);
        return (isIpv6Enabled || hasOnlyOnServer) && hasIpv6Server;
    }
}

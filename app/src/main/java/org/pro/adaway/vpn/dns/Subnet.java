package org.pro.adaway.vpn.dns;

import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class represents a sub network.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class Subnet {
    /**
     * The subnet address.
     */
    final InetAddress address;
    /**
     * The subnet prefix length.
     */
    final int prefixLength;

    /**
     * Parse a subnet string representation.
     *
     * @param subnet The subnet string representation.
     *
     * @return The parsed subnet.
     */
    @NonNull
    static Subnet parse(@NonNull final String subnet) {
        final String[] parts = subnet.split("/");
        try {
            return new Subnet(InetAddress.getByName(parts[0]), parseInt(parts[1]));
        } catch (final UnknownHostException e) {
            throw new IllegalStateException("Failed to parse hardcoded documentation subnet.", e);
        }
    }

    private Subnet(final InetAddress address, final int prefixLength) {
        this.address = requireNonNull(address, "Subnet address cannot be null.");
        this.prefixLength = prefixLength;
    }

    /**
     * Get an address of the subnet.
     *
     * @param index The host index.
     *
     * @return An address of the subnet.
     */
    InetAddress getAddress(final int index) {
        final byte[] address = this.address.getAddress();
        address[address.length - 1] = (byte) (index + 1);
        try {
            return InetAddress.getByAddress(address);
        } catch (final UnknownHostException e) {
            throw new IllegalStateException("Failed to create address " + index + " in subnet " + this);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return this.address + "/" + this.prefixLength;
    }
}

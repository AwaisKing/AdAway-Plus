/*
 * Derived from dns66:
 * Copyright (C) 2016-2019 Julian Andres Klode <jak@jak-linux.org>
 *
 * Derived from AdBuster:
 * Copyright (C) 2016 Daniel Brodie <dbrodie@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Contributions shall also be provided under any later versions of the
 * GPL.
 */
package org.pro.adaway.vpn.dns;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.pro.adaway.AdAwayApplication;
import org.pro.adaway.BuildConfig;
import org.pro.adaway.db.entity.HostEntry;
import org.pro.adaway.db.entity.ListType;
import org.pro.adaway.model.vpn.VpnModel;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pro.adaway.vpn.worker.VpnWorker;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import awaisome.compat.Optional;

/**
 * Creates and parses packets, and sends packets to a remote socket or the device using VpnWorker.
 */
public class DnsPacketProxy {
    // Choose a value that is smaller than the time needed to unblock a host.
    private static final int NEGATIVE_CACHE_TTL_SECONDS = 5;
    private static final SOARecord NEGATIVE_CACHE_SOA_RECORD;

    static {
        try {
            // Let's use a guaranteed invalid hostname here, clients are not supposed to use
            // our fake values, the whole thing just exists for negative caching.
            final Name name = new Name("adaway.vpn.invalid.");
            NEGATIVE_CACHE_SOA_RECORD = new SOARecord(name, DClass.IN, NEGATIVE_CACHE_TTL_SECONDS,
                    name, name, 0, 0, 0, 0, NEGATIVE_CACHE_TTL_SECONDS);
        } catch (final TextParseException e) {
            throw new RuntimeException(e);
        }
    }

    private final VpnWorker vpnWorker;
    private final DnsServerMapper dnsServerMapper;
    private VpnModel vpnModel;

    public DnsPacketProxy(final VpnWorker vpnWorker, final DnsServerMapper dnsServerMapper) {
        this.vpnWorker = vpnWorker;
        this.dnsServerMapper = dnsServerMapper;
    }

    /**
     * Initializes the rules database and the list of upstream servers.
     *
     * @param context The context we are operating in (for the database).
     */
    public void initialize(@NonNull final Context context) {
        this.vpnModel = (VpnModel) ((AdAwayApplication) context.getApplicationContext()).getAdBlockModel();
    }

    /**
     * Handles a responsePayload from an upstream DNS server
     *
     * @param requestPacket   The original request packet
     * @param responsePayload The payload of the response
     */
    public void handleDnsResponse(@NonNull final IpPacket requestPacket, final byte[] responsePayload) {
        final UdpPacket udpOutPacket = (UdpPacket) requestPacket.getPayload();
        final UdpPacket.Builder payLoadBuilder = new UdpPacket.Builder(udpOutPacket)
                .srcPort(udpOutPacket.getHeader().getDstPort())
                .dstPort(udpOutPacket.getHeader().getSrcPort())
                .srcAddr(requestPacket.getHeader().getDstAddr())
                .dstAddr(requestPacket.getHeader().getSrcAddr())
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .payloadBuilder(new UnknownPacket.Builder().rawData(responsePayload));

        final IpPacket ipOutPacket;
        if (requestPacket instanceof IpV4Packet) {
            ipOutPacket = new IpV4Packet.Builder((IpV4Packet) requestPacket)
                    .srcAddr((Inet4Address) requestPacket.getHeader().getDstAddr())
                    .dstAddr((Inet4Address) requestPacket.getHeader().getSrcAddr())
                    .correctChecksumAtBuild(true)
                    .correctLengthAtBuild(true)
                    .payloadBuilder(payLoadBuilder)
                    .build();
        } else {
            ipOutPacket = new IpV6Packet.Builder((IpV6Packet) requestPacket)
                    .srcAddr((Inet6Address) requestPacket.getHeader().getDstAddr())
                    .dstAddr((Inet6Address) requestPacket.getHeader().getSrcAddr())
                    .correctLengthAtBuild(true)
                    .payloadBuilder(payLoadBuilder)
                    .build();
        }
        this.vpnWorker.queueDeviceWrite(ipOutPacket);
    }

    /**
     * Handles a DNS request, by either blocking it or forwarding it to the remote location.
     *
     * @param packetData The packet data to read
     *
     * @throws IOException If some network error occurred
     */
    public void handleDnsRequest(final byte[] packetData) throws IOException {
        final IpPacket ipPacket;
        try {
            ipPacket = (IpPacket) IpSelector.newPacket(packetData, 0, packetData.length);
        } catch (final Exception e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "handleDnsRequest: Discarding invalid IP packet", e);
            return;
        }

        // Check UDP protocol
        if (ipPacket.getHeader().getProtocol() != IpNumber.UDP) return;

        final UdpPacket updPacket;
        final Packet udpPayload;

        try {
            updPacket = (UdpPacket) ipPacket.getPayload();
            udpPayload = updPacket.getPayload();
        } catch (final Exception e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "handleDnsRequest: Discarding unknown packet type " + ipPacket.getHeader(), e);
            return;
        }

        final InetAddress packetAddress = ipPacket.getHeader().getDstAddr();
        final int packetPort = updPacket.getHeader().getDstPort().valueAsInt();
        final Optional<InetAddress> dnsAddressOptional = this.dnsServerMapper.getDnsServerFromFakeAddress(packetAddress);
        if (!dnsAddressOptional.isPresent()) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "Cannot find mapped DNS for " + packetAddress.getHostAddress());
            return;
        }
        final InetAddress dnsAddress = dnsAddressOptional.get();

        if (udpPayload == null) {
            if (BuildConfig.DEBUG)
                Log.i("AWAISKING_APP", "handleDnsRequest: Sending UDP packet without payload: " + updPacket);

            // Let's be nice to Firefox. Firefox uses an empty UDP packet to
            // the gateway to reduce the RTT. For further details, please see
            // https://bugzilla.mozilla.org/show_bug.cgi?id=888268
            final DatagramPacket outPacket = new DatagramPacket(new byte[0], 0, dnsAddress, packetPort);
            vpnWorker.forwardPacket(outPacket);
            return;
        }

        final byte[] dnsRawData = udpPayload.getRawData();
        final Message dnsMsg;
        try {
            dnsMsg = new Message(dnsRawData);
        } catch (final IOException e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "handleDnsRequest: Discarding non-DNS or invalid packet", e);
            return;
        }

        if (dnsMsg.getQuestion() == null) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "handleDnsRequest: Discarding DNS packet with no query " + dnsMsg);
            return;
        }

        final Name name = dnsMsg.getQuestion().getName();
        final String dnsQueryName = name.toString(true);
        final HostEntry entry = getHostEntry(dnsQueryName);
        final ListType entryType = entry.getType();

        if (entryType == ListType.BLOCKED) {
            if (BuildConfig.DEBUG)
                Log.i("AWAISKING_APP", "handleDnsRequest: DNS Name " + dnsQueryName + " blocked!");
            final Header dnsMsgHeader = dnsMsg.getHeader();
            dnsMsgHeader.setFlag(Flags.QR);
            dnsMsgHeader.setRcode(Rcode.NOERROR);
            dnsMsg.addRecord(NEGATIVE_CACHE_SOA_RECORD, Section.AUTHORITY);
            handleDnsResponse(ipPacket, dnsMsg.toWire());
        } else if (entryType == ListType.ALLOWED) {
            if (BuildConfig.DEBUG)
                Log.i("AWAISKING_APP", "handleDnsRequest: DNS Name " + dnsQueryName + " allowed, sending to " + dnsAddress);
            final DatagramPacket outPacket = new DatagramPacket(dnsRawData, 0, dnsRawData.length, dnsAddress, packetPort);
            this.vpnWorker.forwardPacket(outPacket, data -> handleDnsResponse(ipPacket, data));
        } else if (entryType == ListType.REDIRECTED) {
            if (BuildConfig.DEBUG)
                Log.i("AWAISKING_APP", "handleDnsRequest: DNS Name " + dnsQueryName + " redirected to " + entry.getRedirection());
            final Header dnsMsgHeader = dnsMsg.getHeader();
            dnsMsgHeader.setFlag(Flags.QR);
            dnsMsgHeader.setFlag(Flags.AA);
            dnsMsgHeader.unsetFlag(Flags.RD);
            dnsMsgHeader.setRcode(Rcode.NOERROR);
            try {
                final InetAddress address = InetAddress.getByName(entry.getRedirection());
                final Record record;
                if (address instanceof Inet6Address) {
                    record = new AAAARecord(name, DClass.IN, NEGATIVE_CACHE_TTL_SECONDS, address);
                } else {
                    record = new ARecord(name, DClass.IN, NEGATIVE_CACHE_TTL_SECONDS, address);
                }
                dnsMsg.addRecord(record, Section.ANSWER);
            } catch (final UnknownHostException e) {
                if (BuildConfig.DEBUG)
                    Log.e("AWAISKING_APP", "Failed to get inet address for host " + dnsQueryName, e);
            }
            handleDnsResponse(ipPacket, dnsMsg.toWire());
        }
    }

    @NonNull
    private HostEntry getHostEntry(@NonNull final String dnsQueryName) {
        final String hostname = dnsQueryName.toLowerCase(Locale.ENGLISH);
        HostEntry entry = null;
        if (this.vpnModel != null) entry = this.vpnModel.getEntry(hostname);
        if (entry == null) {
            entry = new HostEntry();
            entry.setHost(hostname);
            entry.setType(ListType.ALLOWED);
        }
        return entry;
    }
}

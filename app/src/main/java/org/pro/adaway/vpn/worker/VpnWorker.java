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
package org.pro.adaway.vpn.worker;

import static android.system.OsConstants.ENETUNREACH;
import static android.system.OsConstants.EPERM;
import static android.system.OsConstants.POLLIN;
import static android.system.OsConstants.POLLOUT;
import static org.pro.adaway.vpn.VpnStatus.RECONNECTING_NETWORK_ERROR;
import static org.pro.adaway.vpn.VpnStatus.RUNNING;
import static org.pro.adaway.vpn.VpnStatus.STARTING;
import static org.pro.adaway.vpn.VpnStatus.STOPPED;
import static org.pro.adaway.vpn.VpnStatus.STOPPING;
import static org.pro.adaway.vpn.worker.VpnBuilder.establish;

import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructPollfd;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.core.util.Consumer;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.helper.PreferenceHelper;
import org.pro.adaway.vpn.VpnService;
import org.pro.adaway.vpn.dns.DnsPacketProxy;
import org.pro.adaway.vpn.dns.DnsQueryQueue;
import org.pro.adaway.vpn.dns.DnsServerMapper;
import org.pcap4j.packet.IpPacket;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;


// TODO Write document
// TODO It is thread safe
// TODO Rework status notification
// TODO Improve exception handling in work()
public class VpnWorker {
    /**
     * Maximum packet size is constrained by the MTU, which is given as a signed short.
     */
    private static final int MAX_PACKET_SIZE = Short.MAX_VALUE;

    /**
     * The VPN service, also used as {@link android.content.Context}.
     */
    private final VpnService vpnService;
    /**
     * The queue of packets to send to the device.
     */
    private final Queue<byte[]> deviceWrites;
    /**
     * The queue of DNS queries.
     */
    private final DnsQueryQueue dnsQueryQueue;
    // The mapping between fake and real dns addresses
    private final DnsServerMapper dnsServerMapper;
    // The object where we actually handle packets.
    private final DnsPacketProxy dnsPacketProxy;

    // TODO Comment
    private final VpnConnectionThrottler connectionThrottler;
    private final VpnConnectionMonitor connectionMonitor;

    // Watch dog that checks our connection is alive.
    private final VpnWatchdog vpnWatchDog;

    /**
     * The VPN worker executor (<code>null</code> if not started).
     */
    private final AtomicReference<ExecutorService> executor;
    /**
     * The VPN network interface, (<code>null</code> if not established).
     */
    private final AtomicReference<ParcelFileDescriptor> vpnNetworkInterface;

    /**
     * Constructor.
     *
     * @param vpnService The VPN service, also used as {@link android.content.Context}.
     */
    public VpnWorker(final VpnService vpnService) {
        this.vpnService = vpnService;
        this.deviceWrites = new LinkedList<>();
        this.dnsQueryQueue = new DnsQueryQueue();
        this.dnsServerMapper = new DnsServerMapper();
        this.dnsPacketProxy = new DnsPacketProxy(this, this.dnsServerMapper);
        this.connectionThrottler = new VpnConnectionThrottler();
        this.connectionMonitor = new VpnConnectionMonitor(this.vpnService);
        this.vpnWatchDog = new VpnWatchdog();
        this.executor = new AtomicReference<>(null);
        this.vpnNetworkInterface = new AtomicReference<>(null);
    }

    /**
     * Start the VPN worker.
     * Kill the current worker and restart it if already running.
     */
    public void start() {
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Starting VPN thread…");
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(this::work);
        executor.submit(this.connectionMonitor::monitor);
        setExecutor(executor);
        if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "VPN thread started.");
    }

    /**
     * Stop the VPN worker.
     */
    public void stop() {
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Stopping VPN thread.");
        this.connectionMonitor.reset();
        forceCloseTunnel();
        setExecutor(null);
        if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "VPN thread stopped.");
    }

    /**
     * Keep track of the worker executor.<br>
     * Shut the previous one down in exists.
     *
     * @param executor The new worker executor, <code>null</code> if no executor any more.
     */
    private void setExecutor(final ExecutorService executor) {
        final ExecutorService oldExecutor = this.executor.getAndSet(executor);
        if (oldExecutor != null) {
            if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Shutting down VPN executor…");
            oldExecutor.shutdownNow();
            if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "VPN executor shut down.");
        }
    }

    /**
     * Force close the tunnel connection.
     */
    private void forceCloseTunnel() {
        final ParcelFileDescriptor networkInterface = this.vpnNetworkInterface.get();
        if (networkInterface != null) {
            try {
                networkInterface.close();
            } catch (final IOException e) {
                if (BuildConfig.DEBUG) Log.w("AWAISKING_APP", "Failed to close VPN network interface.", e);
            }
        }
    }

    private void work() {
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Starting work…");
        // Initialize context
        this.dnsPacketProxy.initialize(this.vpnService);
        // Initialize the watchdog
        this.vpnWatchDog.initialize(PreferenceHelper.getVpnWatchdogEnabled(this.vpnService));
        // Try connecting the vpn continuously
        while (true) {
            try {
                this.connectionThrottler.throttle();
                this.vpnService.notifyVpnStatus(STARTING);
                runVpn();
                if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "Told to stop");
                this.vpnService.notifyVpnStatus(STOPPING);
                break;
            } catch (final InterruptedException e) {
                if (BuildConfig.DEBUG) Log.w("AWAISKING_APP", "Failed to wait for connexion throttling.", e);
                Thread.currentThread().interrupt();
                break;
            } catch (final VpnNetworkException | IOException e) {
                if (BuildConfig.DEBUG) Log.w("AWAISKING_APP", "Network exception in vpn thread, reconnecting…", e);
                // If an exception was thrown, notify status and try again
                this.vpnService.notifyVpnStatus(RECONNECTING_NETWORK_ERROR);
            }
        }
        this.vpnService.notifyVpnStatus(STOPPED);
        if (BuildConfig.DEBUG) Log.i("AWAISKING_APP", "Exiting work.");
    }

    private void runVpn() throws IOException, VpnNetworkException {
        // Allocate the buffer for a single packet.
        final byte[] packet = new byte[MAX_PACKET_SIZE];

        // Authenticate and configure the virtual network interface.
        try (final ParcelFileDescriptor pfd = establish(this.vpnService, this.dnsServerMapper);
             // Read and write views of the tunnel device
             final FileInputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
             final FileOutputStream outputStream = new FileOutputStream(pfd.getFileDescriptor())) {
            // Store reference to network interface to close it externally on demand
            this.vpnNetworkInterface.set(pfd);
            // Initialize connection monitor
            this.connectionMonitor.initialize();

            // Update address to ping with default DNS server
            this.vpnWatchDog.setTarget(this.dnsServerMapper.getDefaultDnsServerAddress());

            // Now we are connected. Set the flag and show the message.
            this.vpnService.notifyVpnStatus(RUNNING);

            // We keep forwarding packets till something goes wrong.
            boolean deviceOpened = true;
            while (deviceOpened) deviceOpened = doOne(inputStream, outputStream, packet);
        }
    }

    private boolean doOne(@NonNull final FileInputStream inputStream, final FileOutputStream fileOutputStream, final byte[] packet) throws IOException, VpnNetworkException {
        // Create poll FD on tunnel
        final StructPollfd deviceFd = new StructPollfd();
        deviceFd.fd = inputStream.getFD();
        deviceFd.events = (short) POLLIN;
        if (!this.deviceWrites.isEmpty()) deviceFd.events |= (short) POLLOUT;
        // Create poll FD on each DNS query socket
        final StructPollfd[] queryFds = this.dnsQueryQueue.getQueryFds();
        final StructPollfd[] polls = new StructPollfd[1 + queryFds.length];
        polls[0] = deviceFd;
        System.arraycopy(queryFds, 0, polls, 1, queryFds.length);
        final boolean deviceReadyToWrite;
        final boolean deviceReadyToRead;
        try {
            if (BuildConfig.DEBUG)
                Log.d("AWAISKING_APP", "doOne: Polling " + polls.length + " file descriptors.");
            final int numberOfEvents = Os.poll(polls, this.vpnWatchDog.getPollTimeout());
            // TODO BUG - There is a bug where the watchdog keeps doing timeout if there is no network activity
            // TODO BUG - 0 Might be a valid value if no current DNS query and everything was already sent back to device
            if (numberOfEvents == 0) {
                this.vpnWatchDog.handleTimeout();
                return true;
            }
            deviceReadyToWrite = (deviceFd.revents & POLLOUT) != 0;
            deviceReadyToRead = (deviceFd.revents & POLLIN) != 0;
        } catch (final ErrnoException e) {
            throw new IOException("Failed to wait for event on file descriptors. Error number: " + e.errno, e);
        }

        // Need to do this before reading from the device, otherwise a new insertion there could
        // invalidate one of the sockets we want to read from either due to size or time out
        // constraints
        this.dnsQueryQueue.handleResponses();
        if (deviceReadyToWrite) writeToDevice(fileOutputStream);
        if (deviceReadyToRead) return readPacketFromDevice(inputStream, packet) != -1;
        return true;
    }

    private void writeToDevice(final FileOutputStream fileOutputStream) throws IOException {
        if (BuildConfig.DEBUG)
            Log.d("AWAISKING_APP", "Write to device " + this.deviceWrites.size() + " packets.");
        try {
            while (!this.deviceWrites.isEmpty()) {
                final byte[] ipPacketData = this.deviceWrites.poll();
                fileOutputStream.write(ipPacketData);
            }
        } catch (final IOException e) {
            throw new IOException("Failed to write to tunnel output stream.", e);
        }
    }

    private int readPacketFromDevice(final FileInputStream inputStream, final byte[] packet) throws IOException {
        if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Read a packet from device.");
        // Read the outgoing packet from the input stream.
        final int length = inputStream.read(packet);
        if (length < 0) {
            // TODO Stream closed. Is there anything else to do?
            if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Tunnel input stream closed.");
        } else if (length == 0) {
            if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Read empty packet from tunnel.");
        } else {
            final byte[] readPacket = Arrays.copyOf(packet, length);
            vpnWatchDog.handlePacket(readPacket);
            dnsPacketProxy.handleDnsRequest(readPacket);
        }
        return length;
    }

    /**
     * Forward a packet to the VPN underlying network.
     *
     * @param packet The packet to forward.
     *
     * @throws IOException If the packet could not be forwarded.
     */
    public void forwardPacket(final DatagramPacket packet) throws IOException {
        try (final DatagramSocket dnsSocket = new DatagramSocket()) {
            this.vpnService.protect(dnsSocket);
            dnsSocket.send(packet);
        } catch (final IOException e) {
            throw new IOException("Failed to forward packet.", e);
        }
    }

    /**
     * Forward a packet to the VPN underlying network.
     *
     * @param outPacket The packet to forward.
     * @param callback  The callback to call with the packet response data.
     *
     * @throws IOException If the packet could not be forwarded.
     */
    public void forwardPacket(final DatagramPacket outPacket, final Consumer<byte[]> callback) throws IOException {
        DatagramSocket dnsSocket = null;
        try {
            dnsSocket = new DatagramSocket();
            // Packets to be sent to the real DNS server will need to be protected from the VPN
            this.vpnService.protect(dnsSocket);
            dnsSocket.send(outPacket);
            // Enqueue DNS query
            this.dnsQueryQueue.addQuery(dnsSocket, callback);
        } catch (final IOException e) {
            if (dnsSocket != null) dnsSocket.close();
            if (e.getCause() instanceof ErrnoException) {
                final ErrnoException errnoExc = (ErrnoException) e.getCause();
                if (errnoExc.errno == ENETUNREACH || errnoExc.errno == EPERM)
                    throw new IOException("Cannot send message:", e);
            }
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "handleDnsRequest: Could not send packet to upstream", e);
        }
    }

    /**
     * Write an IP packet to the local TUN device
     *
     * @param ipOutPacket The packet to write (a response to a DNS request)
     */
    public void queueDeviceWrite(@NonNull final IpPacket ipOutPacket) {
        final byte[] rawData = ipOutPacket.getRawData();
        // TODO Check why data could be null
        if (rawData != null) this.deviceWrites.add(rawData);
    }
}

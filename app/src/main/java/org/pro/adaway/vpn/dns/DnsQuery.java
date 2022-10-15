package org.pro.adaway.vpn.dns;

import static android.system.OsConstants.POLLIN;

import android.os.ParcelFileDescriptor;
import android.system.StructPollfd;
import android.util.Log;

import androidx.core.util.Consumer;

import org.pro.adaway.BuildConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


/**
 * This class represents a DNS query.
 *
 * @author Bruce BUJON
 */
class DnsQuery implements AutoCloseable {
    /**
     * The socket used to query DNS server.
     */
    private final DatagramSocket socket;
    /**
     * The callback to call with the query response data.
     */
    private final Consumer<byte[]> callback;
    /**
     * The pollfd related to the query to poll the OS with.
     */
    private final StructPollfd pollfd;
    /**
     * The query creation time, UNIX timestamp in seconds).
     */
    private final long time;

    /**
     * Constructor.
     *
     * @param socket   The socket used to query DNS server.
     * @param callback The callback to call with the query response data.
     */
    DnsQuery(final DatagramSocket socket, final Consumer<byte[]> callback) {
        this.socket = socket;
        this.callback = callback;
        this.time = System.currentTimeMillis() / 1000L;
        this.pollfd = new StructPollfd();
        this.pollfd.fd = ParcelFileDescriptor.fromDatagramSocket(this.socket).getFileDescriptor();
        this.pollfd.events = (short) POLLIN;
    }

    /**
     * Check whether a query is older than a timestamp.
     *
     * @param timestamp The UNIX timestamp (in seconds) to compare query time with.
     *
     * @return <code>true</code> if the query is older than the given timestamp, <code>false</code> otherwise.
     */
    boolean isOlderThan(final long timestamp) {
        return this.time < timestamp;
    }

    /**
     * Get the pollfd related to the query to poll the OS with.
     *
     * @return The pollfd related to the query to poll the OS with.
     */
    StructPollfd getPollfd() {
        return this.pollfd;
    }

    /**
     * Check whether the query is answered, meaning its socket has received data to read.
     *
     * @return {@code true} if there is data to read from {@link #socket}, {@code false} otherwise.
     */
    boolean isAnswered() {
        return (this.pollfd.revents & POLLIN) != 0;
    }

    /**
     * Read DNS query response and notify callback.
     */
    void handleResponse() {
        try {
            final byte[] responseData = new byte[1024];
            this.socket.receive(new DatagramPacket(responseData, responseData.length));
            this.callback.accept(responseData);
        } catch (final IOException e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "Could not handle DNS response.", e);
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        this.socket.close();
    }
}

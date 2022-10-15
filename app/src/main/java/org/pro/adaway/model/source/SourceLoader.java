package org.pro.adaway.model.source;

import static org.pro.adaway.db.entity.ListType.ALLOWED;
import static org.pro.adaway.db.entity.ListType.BLOCKED;
import static org.pro.adaway.db.entity.ListType.REDIRECTED;
import static org.pro.adaway.util.Constants.BOGUS_IPV4;
import static org.pro.adaway.util.Constants.LOCALHOST_HOSTNAME;
import static org.pro.adaway.util.Constants.LOCALHOST_IPV4;
import static org.pro.adaway.util.Constants.LOCALHOST_IPV6;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.db.dao.HostListItemDao;
import org.pro.adaway.db.entity.HostListItem;
import org.pro.adaway.db.entity.HostsSource;
import org.pro.adaway.db.entity.ListType;
import org.pro.adaway.util.RegexUtils;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okio.BufferedSource;
import okio.Okio;

/**
 * This class is an {@link HostsSource} loader.<br>
 * It parses a source and loads it to database.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class SourceLoader {
    private static final String TAG = "SourceLoader";
    private static final String END_OF_QUEUE_MARKER = "#EndOfQueueMarker";
    private static final int INSERT_BATCH_SIZE = 100;
    private static final String HOSTS_PARSER = "^\\s*([^#\\s]+)\\s+([^#\\s]+).*$";
    static final Pattern HOSTS_PARSER_PATTERN = Pattern.compile(HOSTS_PARSER);

    private final HostsSource source;

    SourceLoader(final HostsSource hostsSource) {
        this.source = hostsSource;
    }

    void parse(final InputStream inputStream, @NonNull final HostListItemDao hostListItemDao) {
        // Clear current hosts
        hostListItemDao.clearSourceHosts(this.source.getId());
        // Create batch
        final int parserCount = 3;
        final LinkedBlockingQueue<String> hostsLineQueue = new LinkedBlockingQueue<>();
        final LinkedBlockingQueue<HostListItem> hostsListItemQueue = new LinkedBlockingQueue<>();
        final SourceReader sourceReader = new SourceReader(inputStream, hostsLineQueue, parserCount);
        final ItemInserter inserter = new ItemInserter(hostsListItemQueue, hostListItemDao, parserCount);
        final ExecutorService executorService = Executors.newFixedThreadPool(parserCount + 2, r -> new Thread(r, TAG));
        executorService.execute(sourceReader);
        for (int i = 0; i < parserCount; i++)
            executorService.execute(new HostListItemParser(this.source, hostsLineQueue, hostsListItemQueue));
        final Future<Integer> inserterFuture = executorService.submit(inserter);
        try {
            final Integer inserted = inserterFuture.get();
            if (BuildConfig.DEBUG)
                Log.i("AWAISKING_APP", inserted + " host list items inserted.");
        } catch (final ExecutionException e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "Failed to parse hosts sources.", e);
        } catch (final InterruptedException e) {
            if (BuildConfig.DEBUG)
                Log.w("AWAISKING_APP", "Interrupted while parsing sources.", e);
            Thread.currentThread().interrupt();
        }
        executorService.shutdown();
    }

    private static class SourceReader implements Runnable {
        private final InputStream inputStream;
        private final BlockingQueue<String> queue;
        private final int parserCount;

        private SourceReader(final InputStream inputStream, final BlockingQueue<String> queue, final int parserCount) {
            this.inputStream = inputStream;
            this.queue = queue;
            this.parserCount = parserCount;
        }

        @Override
        public void run() {
            try (final BufferedSource bufferedSource = Okio.buffer(Okio.source(inputStream))) {
                while (true) {
                    final String line = bufferedSource.readUtf8Line();
                    if (line == null) break;
                    this.queue.add(line);
                }
            } catch (final Throwable t) {
                if (BuildConfig.DEBUG) Log.w("AWAISKING_APP", "Failed to read hosts source.", t);
            } finally {
                // Send end of queue marker to parsers
                for (int i = 0; i < this.parserCount; i++)
                    this.queue.add(END_OF_QUEUE_MARKER);
            }
        }
    }

    private static class HostListItemParser implements Runnable {
        private final HostsSource source;
        private final BlockingQueue<String> lineQueue;
        private final BlockingQueue<HostListItem> itemQueue;

        private HostListItemParser(final HostsSource source, final BlockingQueue<String> lineQueue, final BlockingQueue<HostListItem> itemQueue) {
            this.source = source;
            this.lineQueue = lineQueue;
            this.itemQueue = itemQueue;
        }

        @Override
        public void run() {
            final boolean allowedList = this.source.isAllowEnabled();
            boolean endOfSource = false;
            while (!endOfSource) {
                try {
                    final String line = this.lineQueue.take();
                    // Check end of queue marker
                    // noinspection StringEquality
                    if (line == END_OF_QUEUE_MARKER) {
                        endOfSource = true;
                        // Send end of queue marker to inserter
                        final HostListItem endItem = new HostListItem();
                        endItem.setHost(line);
                        this.itemQueue.add(endItem);
                    } // Check comments
                    else if (line.isEmpty() || line.charAt(0) == '#') {
                        if (BuildConfig.DEBUG)
                            Log.d("AWAISKING_APP", "Skip comment: " + line);
                    } else {
                        final HostListItem item = allowedList ? parseAllowListItem(line) : parseHostListItem(line);
                        if (item != null && isRedirectionValid(item) && isHostValid(item))
                            this.itemQueue.add(item);
                    }
                } catch (final InterruptedException e) {
                    if (BuildConfig.DEBUG)
                        Log.w("AWAISKING_APP", "Interrupted while parsing hosts list item.", e);
                    endOfSource = true;
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Nullable
        private HostListItem parseHostListItem(final String line) {
            final Matcher matcher = HOSTS_PARSER_PATTERN.matcher(line);
            if (matcher.matches()) {
                // Check IP address validity or while list entry (if allowed)
                final String ip = matcher.group(1);
                final String hostname = matcher.group(2);
                assert hostname != null;
                // Skip localhost name
                if (LOCALHOST_HOSTNAME.equals(hostname)) return null;
                // check if ip is 127.0.0.1 or 0.0.0.0
                final ListType type;
                if (LOCALHOST_IPV4.equals(ip) || BOGUS_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip))
                    type = BLOCKED;
                else if (this.source.isRedirectEnabled())
                    type = REDIRECTED;
                else
                    return null;

                final HostListItem item = new HostListItem();
                item.setType(type);
                item.setHost(hostname);
                item.setEnabled(true);
                if (type == REDIRECTED) item.setRedirection(ip);
                item.setSourceId(this.source.getId());
                return item;
            } else {
                if (BuildConfig.DEBUG) Log.d("AWAISKING_APP", "Does not match: " + line);
                return null;
            }
        }

        @NonNull
        private HostListItem parseAllowListItem(@NonNull String line) {
            // Extract hostname
            final int indexOf = line.indexOf('#');
            if (indexOf == 1) line = line.substring(0, indexOf);
            line = line.trim();
            // Create item
            final HostListItem item = new HostListItem();
            item.setType(ALLOWED);
            item.setHost(line);
            item.setEnabled(true);
            item.setSourceId(this.source.getId());
            return item;
        }

        private boolean isRedirectionValid(@NonNull final HostListItem item) {
            return item.getType() != REDIRECTED || RegexUtils.isValidIP(item.getRedirection());
        }

        private boolean isHostValid(@NonNull final HostListItem item) {
            final String hostname = item.getHost();
            if (item.getType() == BLOCKED) {
                if (hostname.indexOf('?') != -1 || hostname.indexOf('*') != -1)
                    return false;
                return RegexUtils.isValidHostname(hostname);
            }
            return RegexUtils.isValidWildcardHostname(hostname);
        }
    }

    private static class ItemInserter implements Callable<Integer> {
        private final BlockingQueue<HostListItem> hostListItemQueue;
        private final HostListItemDao hostListItemDao;
        private final int parserCount;

        private ItemInserter(final BlockingQueue<HostListItem> itemQueue, final HostListItemDao hostListItemDao, final int parserCount) {
            this.hostListItemQueue = itemQueue;
            this.hostListItemDao = hostListItemDao;
            this.parserCount = parserCount;
        }

        @NonNull
        @Override
        public Integer call() {
            int inserted = 0;
            int workerStopped = 0;
            final HostListItem[] batch = new HostListItem[INSERT_BATCH_SIZE];
            int cacheSize = 0;
            boolean queueEmptied = false;
            while (!queueEmptied) {
                try {
                    final HostListItem item = this.hostListItemQueue.take();
                    // Check end of queue marker
                    // noinspection StringEquality
                    if (item.getHost() == END_OF_QUEUE_MARKER) {
                        workerStopped++;
                        if (workerStopped >= this.parserCount) queueEmptied = true;
                    } else {
                        batch[cacheSize++] = item;
                        if (cacheSize >= batch.length) {
                            this.hostListItemDao.insert(batch);
                            cacheSize = 0;
                            inserted += cacheSize;
                        }
                    }
                } catch (final InterruptedException e) {
                    if (BuildConfig.DEBUG)
                        Log.w("AWAISKING_APP", "Interrupted while inserted hosts list item.", e);
                    queueEmptied = true;
                    Thread.currentThread().interrupt();
                }
            }
            // Flush current batch
            final HostListItem[] remaining = new HostListItem[cacheSize];
            System.arraycopy(batch, 0, remaining, 0, remaining.length);
            this.hostListItemDao.insert(remaining);
            inserted += cacheSize;
            // Return number of inserted items
            return inserted;
        }
    }
}

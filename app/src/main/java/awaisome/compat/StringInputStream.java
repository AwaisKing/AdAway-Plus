package awaisome.compat;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * extracted from StringBufferInputStream
 */
public class StringInputStream extends InputStream {
    protected String buffer;
    protected int pos;
    protected int count;

    public StringInputStream(@NonNull final String s) {
        this.buffer = s;
        this.count = s.length();
    }

    @Override
    public synchronized int read() {
        return pos < count ? buffer.charAt(pos++) & 0xFF : -1;
    }

    @Override
    public synchronized int read(final byte[] b, final int off, int len) {
        if (b == null) throw new NullPointerException();
        if (off < 0 || off > b.length || len < 0 || off + len > b.length || off + len < 0)
            throw new IndexOutOfBoundsException();

        if (pos >= count) return -1;

        final int avail = count - pos;
        if (len > avail) len = avail;

        if (len <= 0) return 0;

        getStringBytes(pos, pos + len, b, off);
        pos += len;
        return len;
    }

    @Override
    public synchronized long skip(long n) {
        if (n < 0) {
            return 0;
        }
        if (n > count - pos) {
            n = count - pos;
        }
        pos += n;
        return n;
    }

    @Override
    public synchronized int available() {
        return count - pos;
    }

    @Override
    public synchronized void reset() {
        pos = 0;
    }

    @Override
    public void close() throws IOException {
        buffer = null;
        pos = count = -1;
    }

    private void getStringBytes(final int srcBegin, final int srcEnd, final byte[] dst, final int dstBegin) {
        checkBoundsBeginEnd(srcBegin, srcEnd, buffer.length());
        Objects.requireNonNull(dst);
        checkBoundsOffCount(dstBegin, srcEnd - srcBegin, dst.length);
        int j = dstBegin, i = srcBegin;
        while (i < srcEnd) dst[j++] = (byte) buffer.charAt(i++);
    }

    private static void checkBoundsOffCount(final int offset, final int count, final int length) {
        if (offset < 0 || count < 0 || offset > length - count)
            throw new StringIndexOutOfBoundsException("offset " + offset + ", count " + count + ", length " + length);
    }

    private static void checkBoundsBeginEnd(final int begin, final int end, final int length) {
        if (begin < 0 || begin > end || end > length)
            throw new StringIndexOutOfBoundsException("begin " + begin + ", end " + end + ", length " + length);
    }
}
package awaisome.compat;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class StringJoiner {
    private final String prefix;
    private final String delimiter;
    private final String suffix;
    private StringBuilder value;
    private String emptyValue;

    public StringJoiner(final CharSequence delimiter) {
        this(delimiter, "", "");
    }

    public StringJoiner(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        Objects.requireNonNull(prefix, "The prefix must not be null");
        Objects.requireNonNull(delimiter, "The delimiter must not be null");
        Objects.requireNonNull(suffix, "The suffix must not be null");
        // make defensive copies of arguments
        this.prefix = prefix.toString();
        this.delimiter = delimiter.toString();
        this.suffix = suffix.toString();
        this.emptyValue = this.prefix + this.suffix;
    }

    public StringJoiner setEmptyValue(final CharSequence emptyValue) {
        this.emptyValue = Objects.requireNonNull(emptyValue, "The empty value must not be null").toString();
        return this;
    }

    public StringJoiner add(final CharSequence newElement) {
        prepareBuilder().append(newElement);
        return this;
    }

    public StringJoiner merge(final StringJoiner other) {
        Objects.requireNonNull(other);
        if (other.value != null) {
            final int length = other.value.length();
            // lock the length so that we can seize the data to be appended
            // before initiate copying to avoid interference, especially when
            // merge 'this'
            final StringBuilder builder = prepareBuilder();
            builder.append(other.value, other.prefix.length(), length);
        }
        return this;
    }

    private StringBuilder prepareBuilder() {
        if (value != null) {
            value.append(delimiter);
        } else {
            value = new StringBuilder().append(prefix);
        }
        return value;
    }

    public int length() {
        // Remember that we never actually append the suffix unless we return
        // the full (present) value or some sub-string or length of it, so that
        // we can add on more if we need to.
        return (value != null ? value.length() + suffix.length() :
                emptyValue.length());
    }

    @NonNull
    @Override
    public String toString() {
        if (value == null) {
            return emptyValue;
        } else {
            if (suffix.equals("")) {
                return value.toString();
            } else {
                final int initialLength = value.length();
                final String result = value.append(suffix).toString();
                // reset value to pre-append initialLength
                value.setLength(initialLength);
                return result;
            }
        }
    }
}
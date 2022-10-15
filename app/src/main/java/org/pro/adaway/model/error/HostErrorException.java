package org.pro.adaway.model.error;

import androidx.annotation.NonNull;

/**
 * This class in an {@link Exception} thrown on hosts  error.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class HostErrorException extends Exception {
    private static final long serialVersionUID = -4926071188397448561L;
    /**
     * The exception error type.
     */
    private final HostError error;

    /**
     * Constructor.
     *
     * @param error The exception error type.
     */
    public HostErrorException(@NonNull final HostError error) {
        super("An host error " + error.name() + " occurred");
        this.error = error;
    }

    /**
     * Constructor.
     *
     * @param error The exception error type.
     * @param cause The cause of this exception.
     */
    public HostErrorException(@NonNull final HostError error, final Throwable cause) {
        super("An host error " + error.name() + " occurred", cause);
        this.error = error;
    }

    /**
     * Get the error type.
     *
     * @return The exception error type
     */
    public HostError getError() {
        return this.error;
    }
}

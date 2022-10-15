package awaisome.compat;

import androidx.annotation.NonNull;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import androidx.core.util.Consumer;
import androidx.core.util.Predicate;
import androidx.core.util.Supplier;
import androidx.arch.core.util.Function;

public final class Optional<T> {
    private static final Optional<?> EMPTY = new Optional<>();
    private final T value;

    private Optional() {
        this.value = null;
    }

    public static <T> Optional<T> empty() {
        @SuppressWarnings("unchecked") final Optional<T> t = (Optional<T>) EMPTY;
        return t;
    }

    private Optional(final T value) {
        this.value = Objects.requireNonNull(value);
    }

    @NonNull
    public static <T> Optional<T> of(final T value) {
        return new Optional<>(value);
    }

    public static <T> Optional<T> ofNullable(final T value) {
        return value == null ? empty() : of(value);
    }

    public T get() {
        if (value == null) throw new NoSuchElementException("No value present");
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public boolean isEmpty() {
        return value == null;
    }

    public void ifPresent(final Consumer<? super T> action) {
        if (value != null) action.accept(value);
    }

    public void ifPresentOrElse(final Consumer<? super T> action, final Runnable emptyAction) {
        if (value != null) action.accept(value);
        else emptyAction.run();
    }

    /**
     * If a value is present, and the value matches the given predicate,
     * returns an {@code Optional} describing the value, otherwise returns an
     * empty {@code Optional}.
     *
     * @param predicate the predicate to apply to a value, if present
     *
     * @return an {@code Optional} describing the value of this
     * {@code Optional}, if a value is present and the value matches the
     * given predicate, otherwise an empty {@code Optional}
     *
     * @throws NullPointerException if the predicate is {@code null}
     */
    public Optional<T> filter(final Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (!isPresent()) return this;
        return predicate.test(value) ? this : empty();
    }

    /**
     * If a value is present, returns an {@code Optional} describing (as if by
     * {@link #ofNullable}) the result of applying the given mapping function to
     * the value, otherwise returns an empty {@code Optional}.
     *
     * <p>If the mapping function returns a {@code null} result then this method
     * returns an empty {@code Optional}.
     *
     * @param mapper the mapping function to apply to a value, if present
     * @param <U>    The type of the value returned from the mapping function
     *
     * @return an {@code Optional} describing the result of applying a mapping
     * function to the value of this {@code Optional}, if a value is
     * present, otherwise an empty {@code Optional}
     *
     * @throws NullPointerException if the mapping function is {@code null}
     * @apiNote This method supports post-processing on {@code Optional} values, without
     * the need to explicitly check for a return status.  For example, the
     * following code traverses a stream of URIs, selects one that has not
     * yet been processed, and creates a path from that URI, returning
     * an {@code Optional<Path>}:
     *
     * <pre>{@code
     *     Optional<Path> p =
     *         uris.stream().filter(uri -> !isProcessedYet(uri))
     *                       .findFirst()
     *                       .map(Paths::get);
     * }</pre>
     * <p>
     * Here, {@code findFirst} returns an {@code Optional<URI>}, and then
     * {@code map} returns an {@code Optional<Path>} for the desired
     * URI if one exists.
     */
    public <U> Optional<U> map(final Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) return empty();
        return Optional.ofNullable(mapper.apply(value));
    }

    /**
     * If a value is present, returns the result of applying the given
     * {@code Optional}-bearing mapping function to the value, otherwise returns
     * an empty {@code Optional}.
     *
     * <p>This method is similar to {@link #map(Function)}, but the mapping
     * function is one whose result is already an {@code Optional}, and if
     * invoked, {@code flatMap} does not wrap it within an additional
     * {@code Optional}.
     *
     * @param <U>    The type of value of the {@code Optional} returned by the
     *               mapping function
     * @param mapper the mapping function to apply to a value, if present
     *
     * @return the result of applying an {@code Optional}-bearing mapping
     * function to the value of this {@code Optional}, if a value is
     * present, otherwise an empty {@code Optional}
     *
     * @throws NullPointerException if the mapping function is {@code null} or
     *                              returns a {@code null} result
     */
    public <U> Optional<U> flatMap(final Function<? super T, ? extends Optional<? extends U>> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) return empty();
        @SuppressWarnings("unchecked") final Optional<U> r = (Optional<U>) mapper.apply(value);
        return Objects.requireNonNull(r);
    }

    /**
     * If a value is present, returns an {@code Optional} describing the value,
     * otherwise returns an {@code Optional} produced by the supplying function.
     *
     * @param supplier the supplying function that produces an {@code Optional}
     *                 to be returned
     *
     * @return returns an {@code Optional} describing the value of this
     * {@code Optional}, if a value is present, otherwise an
     * {@code Optional} produced by the supplying function.
     *
     * @throws NullPointerException if the supplying function is {@code null} or
     *                              produces a {@code null} result
     * @since 9
     */
    public Optional<T> or(final Supplier<? extends Optional<? extends T>> supplier) {
        Objects.requireNonNull(supplier);
        if (isPresent()) return this;
        @SuppressWarnings("unchecked") final Optional<T> r = (Optional<T>) supplier.get();
        return Objects.requireNonNull(r);
    }

    /**
     * If a value is present, returns a sequential {@link Stream} containing
     * only that value, otherwise returns an empty {@code Stream}.
     *
     * @return the optional value as a {@code Stream}
     *
     * @apiNote This method can be used to transform a {@code Stream} of optional
     * elements to a {@code Stream} of present value elements:
     * <pre>{@code
     *     Stream<Optional<T>> os = ..
     *     Stream<T> s = os.flatMap(Optional::stream)
     * }</pre>
     * @since 9
     */
    public Stream<T> stream() {
        if (!isPresent()) return Stream.empty();
        else return Stream.of(value);
    }

    /**
     * If a value is present, returns the value, otherwise returns
     * {@code other}.
     *
     * @param other the value to be returned, if no value is present.
     *              May be {@code null}.
     *
     * @return the value, if present, otherwise {@code other}
     */
    public T orElse(final T other) {
        return value != null ? value : other;
    }

    /**
     * If a value is present, returns the value, otherwise returns the result
     * produced by the supplying function.
     *
     * @param supplier the supplying function that produces a value to be returned
     *
     * @return the value, if present, otherwise the result produced by the
     * supplying function
     *
     * @throws NullPointerException if no value is present and the supplying
     *                              function is {@code null}
     */
    public T orElseGet(final Supplier<? extends T> supplier) {
        return value != null ? value : supplier.get();
    }

    /**
     * If a value is present, returns the value, otherwise throws
     * {@code NoSuchElementException}.
     *
     * @return the non-{@code null} value described by this {@code Optional}
     *
     * @throws NoSuchElementException if no value is present
     * @since 10
     */
    public T orElseThrow() {
        if (value == null) throw new NoSuchElementException("No value present");
        return value;
    }

    /**
     * If a value is present, returns the value, otherwise throws an exception
     * produced by the exception supplying function.
     *
     * @param <X>               Type of the exception to be thrown
     * @param exceptionSupplier the supplying function that produces an
     *                          exception to be thrown
     *
     * @return the value, if present
     *
     * @throws X                    if no value is present
     * @throws NullPointerException if no value is present and the exception
     *                              supplying function is {@code null}
     * @apiNote A method reference to the exception constructor with an empty argument
     * list can be used as the supplier. For example,
     * {@code IllegalStateException::new}
     */
    public <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier) throws X {
        if (value == null) throw exceptionSupplier.get();
        return value;
    }

    /**
     * Indicates whether some other object is "equal to" this {@code Optional}.
     * The other object is considered equal if:
     * <ul>
     * <li>it is also an {@code Optional} and;
     * <li>both instances have no value present or;
     * <li>the present values are "equal to" each other via {@code equals()}.
     * </ul>
     *
     * @param obj an object to be tested for equality
     *
     * @return {@code true} if the other object is "equal to" this object
     * otherwise {@code false}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Optional)) return false;
        final Optional<?> other = (Optional<?>) obj;
        return Objects.equals(value, other.value);
    }

    /**
     * Returns the hash code of the value, if present, otherwise {@code 0}
     * (zero) if no value is present.
     *
     * @return hash code value of the present value or {@code 0} if no value is
     * present
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    /**
     * Returns a non-empty string representation of this {@code Optional}
     * suitable for debugging.  The exact presentation format is unspecified and
     * may vary between implementations and versions.
     *
     * @return the string representation of this instance
     *
     * @implSpec If a value is present the result must include its string representation
     * in the result.  Empty and present {@code Optional}s must be unambiguously
     * differentiable.
     */
    @NonNull
    @Override
    public String toString() {
        return value == null ? "Optional.empty" : "Optional[" + value + "]";
    }
}
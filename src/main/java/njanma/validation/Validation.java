package njanma.validation;

import io.vavr.Value;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.concurrent.Future;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Validation with monadic combinators.
 *
 * @param <Err> - error type
 */
public interface Validation<Err> extends Value<Err> {

    /**
     * Create validation result from varargs of errors.
     *
     * @param <E> - error type
     * @return result of validation
     * @see SuccessValidation
     * @see ErrorValidation
     */
    static <E> Validation<E> of(E result) {
        return ofAll(Stream.of(result));
    }

    /**
     * Create validation result from varargs of errors.
     *
     * @param <E> - error type
     * @return result of validation
     * @see SuccessValidation
     * @see ErrorValidation
     */
    @SafeVarargs
    static <E> Validation<E> of(E first, E... others) {
        return ofAll(Stream.of(others).prepend(first));
    }

    /**
     * Create validation result from list of errors.
     *
     * @param <E> - error type
     * @return result of validation
     * @see SuccessValidation
     * @see ErrorValidation
     */
    static <E> Validation<E> ofAll(Iterable<E> errors) {
        List<E> errorList = List.ofAll(errors);
        if (errorList.nonEmpty()) {
            return failure(errorList);
        } else
            return success();
    }

    /**
     * Create success validation result.
     *
     * @param <E> - error type
     * @return {@link SuccessValidation}
     * @see SuccessValidation
     */
    static <E> Validation<E> success() {
        return SuccessValidation.instance();
    }

    /**
     * Create failure validation result.
     *
     * @param <E> - error type
     * @return {@link ErrorValidation}
     * @see ErrorValidation
     */
    @SafeVarargs
    static <E> Validation<E> failure(E first, E... errors) {
        return new ErrorValidation<>(List.of(errors).prepend(first));
    }

    static <E> Validation<E> failure(Iterable<E> errors) {
        return new ErrorValidation<>(List.ofAll(errors));
    }

    /**
     * Create chain of validators which will execute with dependent of result.
     *
     * @param validators - list of errors
     * @param <E>        - error type
     * @return result of validation
     * @see SuccessValidation
     * @see ErrorValidation
     */
    @SafeVarargs
    static <E> Validation<E> chain(Supplier<Validation<E>>... validators) {
        return chain(Stream.of(validators));
    }

    /**
     * Create chain of validators which will execute with dependent of result.
     *
     * @param validators - list of errors
     * @param <E>        - error type
     * @return result of validation
     * @see SuccessValidation
     * @see ErrorValidation
     */
    static <E> Validation<E> chain(Stream<Supplier<Validation<E>>> validators) {
        Validation<E> result = validators.head().get();
        if (validators.tail().nonEmpty()) {
            return result.andThen(() -> chain(validators.tail()));
        } else
            return result;
    }

    /**
     * Create sequence of validators which will execute without dependent of result.
     *
     * @param validators - list of errors
     * @param <E>        - error type
     * @return result of validation
     * @see SuccessValidation
     * @see ErrorValidation
     */
    @SafeVarargs
    static <E> Validation<E> sequence(Validation<E> firstValidation, Validation<E>... validators) {
        return sequence(Stream.of(validators).prepend(firstValidation));
    }

    /**
     * Create sequence of validators which will execute without dependent of result.
     *
     * @param validators - list of errors
     * @param <E>        - error type
     * @return result of validation
     * @see SuccessValidation
     * @see ErrorValidation
     */
    static <E> Validation<E> sequence(Iterable<Validation<E>> validators) {
        return Stream.ofAll(validators).foldLeft(Validation.success(), Validation::combine);
    }

    static <E> Validation<E> sequence(java.util.stream.Stream<Validation<E>> validators) {
        return Objects.requireNonNull(validators, "Stream should be not null!")
                .reduce(Validation.success(), Validation::combine);
    }

    /**
     * Create parallel sequence of validators which will execute without dependent of result.
     *
     * @param validators - list of errors
     * @param <E>        - error type
     * @return result of validation
     * @see SuccessValidation
     * @see ErrorValidation
     */
    @SafeVarargs
    static <E> Validation<E> parSequence(Supplier<Validation<E>>... validators) {
        return sequence(Stream.of(validators).flatMap(Future::ofSupplier));
    }

    /**
     * Combine errors in {@link Validation}'s
     *
     * @param other - {@link Validation} for combine contained errors.
     * @return combined {@link Validation}
     */
    default Validation<Err> combine(Validation<? extends Err> other) {
        Objects.requireNonNull(other, "Other validation should be not null!");
        return flatMap(v -> other.flatMap(o -> Validation.of(v, o)).orElse(() -> this)).orElse(() -> other);
    }

    /**
     * Returns this {@code Validation} if it is nonempty, otherwise return the result of evaluating supplier.
     *
     * @param supplier An alternative {@code Validation} supplier
     * @return this {@code Validation} if it is nonempty, otherwise return the result of evaluating supplier.
     */
    @SuppressWarnings("unchecked")
    default Validation<Err> orElse(Supplier<? extends Validation<? extends Err>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isEmpty() ? (Validation<Err>) supplier.get() : this;
    }

    default boolean nonEmpty() {
        return !isEmpty();
    }

    default Validation<Err> filter(Predicate<? super Err> action) {
        return Validation.ofAll(List.ofAll(this).filter(action));
    }

    default void ifPresent(Consumer<? super List<Err>> action) {
        if (nonEmpty()) {
            action.accept(List.ofAll(this));
        }
    }

    default Validation<Err> andThen(Supplier<Validation<Err>> nextValidator) {
        if (isEmpty()) {
            return sequence(this, nextValidator.get());
        } else
            return this;
    }

    default <X extends Throwable> void ifPresentThrow(Function<? super List<Err>, X> exceptionSupplier) throws X {
        if (nonEmpty()) {
            throw exceptionSupplier.apply(List.ofAll(this));
        }
    }

    default <U> Validation<U> flatMap(Function<? super Err, ? extends Validation<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        List<U> list = List.empty();
        for (Err t : this) {
            for (U u : mapper.apply(t)) {
                list = list.prepend(u);
            }
        }
        return Validation.ofAll(list.reverse());
    }

    @Override
    default boolean isAsync() {
        return false;
    }

    @Override
    default boolean isLazy() {
        return false;
    }

    @Override
    default Iterator<Err> iterator() {
        return isEmpty() ? Iterator.empty() : Iterator.of(get());
    }

    @Override
    default <U> Validation<U> map(Function<? super Err, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        List<U> list = List.empty();
        for (Err t : this) {
            list = list.prepend(mapper.apply(t));
        }
        return Validation.ofAll(list.reverse());
    }

    @Override
    default Validation<Err> peek(Consumer<? super Err> action) {
        Objects.requireNonNull(action, "action is null");
        if (!isEmpty()) {
            action.accept(get());
        }
        return this;
    }

    final class SuccessValidation<E> implements Validation<E> {

        private static final SuccessValidation<?> INSTANCE = new SuccessValidation<>();

        private SuccessValidation() {
        }

        @SuppressWarnings("unchecked")
        public static <T> SuccessValidation<T> instance() {
            return (SuccessValidation<T>) INSTANCE;
        }

        @Override
        public E get() {
            throw new NoSuchElementException("Haven't any errors!");
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean isSingleValued() {
            return false;
        }

        @Override
        public String stringPrefix() {
            return "Success!";
        }
    }

    final class ErrorValidation<E> implements Validation<E> {

        private final List<E> errors;

        private ErrorValidation(List<E> errors) {
            this.errors = errors;
        }

        public static <E> ErrorValidation<E> of(List<E> errors) {
            return new ErrorValidation<>(errors);
        }

        @Override
        public Iterator<E> iterator() {
            return errors.iterator();
        }

        @Override
        public E get() {
            return errors.get();
        }

        @Override
        public boolean isEmpty() {
            return errors.isEmpty();
        }

        @Override
        public boolean isSingleValued() {
            return false;
        }

        @Override
        public String stringPrefix() {
            return "List of errors: " + errors.stringPrefix();
        }
    }
}

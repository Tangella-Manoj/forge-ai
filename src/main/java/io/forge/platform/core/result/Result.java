package io.forge.platform.core.result;

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents the outcome of an operation that can either succeed with a value or fail with a typed
 * error.
 *
 * <p>This is the core platform contract for explicit, exception-free control flow.
 *
 * @param <T> the success value type
 * @param <E> the failure value type
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
  /**
   * Creates a successful result.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Result<String, String> result = Result.success("ok");
   * }</pre>
   *
   * @param value the success value
   * @param <T> the success value type
   * @param <E> the failure value type
   * @return a successful result
   */
  static <T, E> Result<T, E> success(T value) {
    return new Success<>(value);
  }

  /**
   * Creates a failed result.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Result<String, String> result = Result.failure("bad");
   * }</pre>
   *
   * @param error the failure value
   * @param <T> the success value type
   * @param <E> the failure value type
   * @return a failed result
   */
  static <T, E> Result<T, E> failure(E error) {
    return new Failure<>(error);
  }

  /**
   * Returns {@code true} when this result is successful.
   *
   * @return whether the result is successful
   */
  default boolean isSuccess() {
    return this instanceof Success<?, ?>;
  }

  /**
   * Returns {@code true} when this result is failed.
   *
   * @return whether the result is failed
   */
  default boolean isFailure() {
    return this instanceof Failure<?, ?>;
  }

  /**
   * Maps the success value when present.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Result<Integer, String> doubled = Result.success(2).map(value -> value * 2);
   * }</pre>
   *
   * @param mapper success value mapper
   * @param <U> the mapped success type
   * @return a mapped result
   */
  default <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
    Objects.requireNonNull(mapper, "mapper must not be null");
    return fold(value -> Result.success(mapper.apply(value)), Result::failure);
  }

  /**
   * Maps the failure value when present.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Result<String, Integer> mapped = Result.failure("bad").mapError(String::length);
   * }</pre>
   *
   * @param mapper failure value mapper
   * @param <F> the mapped failure type
   * @return a mapped result
   */
  default <F> Result<T, F> mapError(Function<? super E, ? extends F> mapper) {
    Objects.requireNonNull(mapper, "mapper must not be null");
    return fold(Result::success, error -> Result.failure(mapper.apply(error)));
  }

  /**
   * Flat-maps the success value when present.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Result<Integer, String> result =
   *     Result.success(2).flatMap(value -> Result.success(value * 3));
   * }</pre>
   *
   * @param mapper success value mapper
   * @param <U> the mapped success type
   * @return a mapped result
   */
  default <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
    Objects.requireNonNull(mapper, "mapper must not be null");
    return fold(mapper, Result::failure);
  }

  /**
   * Folds the result into a single value.
   *
   * <p>Example:
   *
   * <pre>{@code
   * String value = result.fold(success -> success, failure -> failure);
   * }</pre>
   *
   * @param onSuccess success mapper
   * @param onFailure failure mapper
   * @param <R> folded result type
   * @return a single folded value
   */
  <R> R fold(
      Function<? super T, ? extends R> onSuccess, Function<? super E, ? extends R> onFailure);

  /**
   * Successful result variant.
   *
   * @param value the success value
   * @param <T> the success value type
   * @param <E> the failure value type
   */
  record Success<T, E>(T value) implements Result<T, E> {
    public Success {
      Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    public <R> R fold(
        Function<? super T, ? extends R> onSuccess, Function<? super E, ? extends R> onFailure) {
      Objects.requireNonNull(onSuccess, "onSuccess must not be null");
      Objects.requireNonNull(onFailure, "onFailure must not be null");
      return onSuccess.apply(value);
    }
  }

  /**
   * Failed result variant.
   *
   * @param error the failure value
   * @param <T> the success value type
   * @param <E> the failure value type
   */
  record Failure<T, E>(E error) implements Result<T, E> {
    public Failure {
      Objects.requireNonNull(error, "error must not be null");
    }

    @Override
    public <R> R fold(
        Function<? super T, ? extends R> onSuccess, Function<? super E, ? extends R> onFailure) {
      Objects.requireNonNull(onSuccess, "onSuccess must not be null");
      Objects.requireNonNull(onFailure, "onFailure must not be null");
      return onFailure.apply(error);
    }
  }
}

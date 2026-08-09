package io.forge.platform.core.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResultTest {
  @Test
  void createsAndReadsSuccess() {
    Result<String, String> result = Result.success("ok");

    assertTrue(result.isSuccess());
    assertFalse(result.isFailure());
    assertEquals("ok", result.fold(value -> value, error -> error));
  }

  @Test
  void createsAndReadsFailure() {
    Result<String, String> result = Result.failure("bad");

    assertFalse(result.isSuccess());
    assertTrue(result.isFailure());
    assertEquals("bad", result.fold(value -> value, error -> error));
  }

  @Test
  void mapsSuccessValues() {
    Result<Integer, String> result = Result.<Integer, String>success(2).map(value -> value * 2);

    assertEquals(Result.<Integer, String>success(4), result);
    assertEquals(Integer.valueOf(4), result.fold(value -> value, error -> -1));
  }

  @Test
  void mapsFailureValues() {
    Result<String, Integer> result = Result.<String, String>failure("bad").mapError(String::length);

    assertEquals(Result.<String, Integer>failure(3), result);
    assertEquals(Integer.valueOf(3), result.fold(value -> -1, error -> error));
  }

  @Test
  void flatMapsSuccessValues() {
    Result<Integer, String> result =
        Result.<Integer, String>success(2)
            .flatMap(value -> Result.<Integer, String>success(value * 3));

    assertEquals(Result.<Integer, String>success(6), result);
    assertEquals(Integer.valueOf(6), result.fold(value -> value, error -> -1));
  }

  @Test
  void flatMapDoesNotTouchFailureValues() {
    Result<Integer, String> result =
        Result.<Integer, String>failure("bad")
            .flatMap(value -> Result.<Integer, String>success(value * 3));

    assertEquals(Result.<Integer, String>failure("bad"), result);
    assertEquals("bad", result.fold(value -> "ok", error -> error));
  }

  @Test
  void foldsBothBranches() {
    Result<String, String> success = Result.success("ok");
    Result<String, String> failure = Result.failure("bad");

    assertEquals(
        "success:ok", success.fold(value -> "success:" + value, error -> "failure:" + error));
    assertEquals(
        "failure:bad", failure.fold(value -> "success:" + value, error -> "failure:" + error));
  }

  @Test
  void rejectsNullSuccessValue() {
    assertThrows(NullPointerException.class, () -> Result.success(null));
  }

  @Test
  void rejectsNullFailureValue() {
    assertThrows(NullPointerException.class, () -> Result.failure(null));
  }

  @Test
  void rejectsNullMapFunction() {
    Result<String, String> result = Result.success("ok");

    assertThrows(NullPointerException.class, () -> result.map(null));
  }

  @Test
  void rejectsNullMapErrorFunction() {
    Result<String, String> result = Result.failure("bad");

    assertThrows(NullPointerException.class, () -> result.mapError(null));
  }

  @Test
  void rejectsNullFlatMapFunction() {
    Result<String, String> result = Result.success("ok");

    assertThrows(NullPointerException.class, () -> result.flatMap(null));
  }

  @Test
  void rejectsNullFoldFunctions() {
    Result<String, String> result = Result.success("ok");

    assertThrows(NullPointerException.class, () -> result.fold(null, error -> error));
    assertThrows(NullPointerException.class, () -> result.fold(value -> value, null));
  }

  @Test
  void supportsEqualsHashCodeAndToString() {
    Result<String, String> successA = Result.success("ok");
    Result<String, String> successB = Result.success("ok");
    Result<String, String> failure = Result.failure("bad");

    assertEquals(successA, successB);
    assertEquals(successA.hashCode(), successB.hashCode());
    assertNotEquals(successA, failure);
    assertEquals("Success[value=ok]", successA.toString());
    assertEquals("Failure[error=bad]", failure.toString());
  }

  @Test
  void supportsNestedResults() {
    Result<Result<String, String>, String> nested =
        Result.<Result<String, String>, String>success(Result.<String, String>success("ok"));

    assertEquals("ok", nested.fold(value -> value.fold(v -> v, error -> error), error -> error));
  }
}

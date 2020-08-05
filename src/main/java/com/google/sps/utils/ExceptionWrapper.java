package com.google.sps.utils;

import java.util.function.Function;

public class ExceptionWrapper {
  private ExceptionWrapper() {}

  public static <T, R> Function<T, R> wrap(ExceptionFunction<T, R> exceptionFunc) {
    return t -> {
      try {
        return exceptionFunc.apply(t);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

  public interface ExceptionFunction<T, R> {
    R apply(T param) throws Exception;
  }
}

package com.google.sps.utils;

public class StringUtils {
  public static String slice(String str, int start, int length) {
    if (str.length() > start + length) {
      return str.substring(0, (int) start) + str.substring((int) (start + length));
    } else {
      return str.substring(0, (int) start);
    }
  }

  public static String insert(String orig, String insert, int idx) {
    if (orig.length() > idx) {
      return orig.substring(0, (int) idx) + insert + orig.substring((int) idx);
    } else {
      return orig + insert;
    }
  }
}
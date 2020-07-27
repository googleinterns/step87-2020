package com.google.sps.models;

public class UserData {
  private String name;
  private String code;
  private String type;
  private boolean inQueue;

  public UserData(String code, String name, String type) {
    this(code, name, type, false);
  }

  public UserData(String code, String name, String type, boolean inQueue) {
    this.name = name;
    this.code = code;
    this.type = type;
    this.inQueue = inQueue;
  }
}

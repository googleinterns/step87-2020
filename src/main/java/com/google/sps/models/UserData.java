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

  /** @return the name */
  public String getName() {
    return name;
  }

  /** @return the code */
  public String getCode() {
    return code;
  }

  /** @return the type */
  public String getType() {
    return type;
  }

  /** @return whether the user is in the queue */
  public boolean getInQueue() {
    return inQueue;
  }
}

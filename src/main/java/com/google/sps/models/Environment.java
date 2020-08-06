package com.google.sps.models;

public class Environment {
  private String name;
  private String status;
  private String error;
  private String id;

  public Environment(String name, String status, String error, String id) {
    this.name = name;
    this.status = status;
    this.error = error;
    this.id = id;
  }

  /** @return the name */
  public String getName() {
    return name;
  }

  /** @return the status */
  public String getStatus() {
    return status;
  }

  /** @return the error */
  public String getError() {
    return error;
  }

  /** @return the id */
  public String getId() {
    return id;
  }
}

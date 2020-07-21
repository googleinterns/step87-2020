package com.google.sps.servlets;

public class UserData {
  private String name;
  private String code;
  private String type;

  public UserData(String code, String name, String type) {
    this.name = name;
    this.code = code;
    this.type = type;
  }
}

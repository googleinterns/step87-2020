package com.google.sps.queue;

public class StudentStatus {
  private int location;
  private String workspace;
  private static final String WORKSPACE = "/workspace/?workspaceID=";

  public StudentStatus(int location, String workspaceID) {
    this.location = location;
    this.workspace = WORKSPACE + workspaceID;
  }
}

package com.google.sps.queue;

public class StudentStatus {
  private int position;
  private String workspace;
  private static final String WORKSPACE = "/workspace/?workspaceID=";

  public StudentStatus(int position, String workspaceID) {
    this.position = position;
    this.workspace = WORKSPACE + workspaceID;
  }
}

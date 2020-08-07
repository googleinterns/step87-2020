package com.google.sps.queue;

public class StudentStatus {
  private int position;
  private String workspace;
  private String ta;
  private static final String WORKSPACE = "/workspace/?workspaceID=";

  public StudentStatus(int position, String workspaceID) {
    this(position, workspaceID, null);
  }

  public StudentStatus(int position, String workspaceID, String ta) {
    this.position = position;
    this.workspace = WORKSPACE + workspaceID;
    this.ta = ta;
  }

  /** @return the position */
  public int getPosition() {
    return position;
  }

  /** @return the workspace */
  public String getWorkspace() {
    return workspace;
  }

  /** @return the ta */
  public String getTa() {
    return ta;
  }
}

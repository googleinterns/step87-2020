package com.google.sps.queue;

import java.util.List;
import java.util.Objects;

public class Queue {
  private List<String> queue;
  private Helping helping;

  public Queue(List<String> queue, Helping helping) {
    this.queue = Objects.requireNonNull(queue);
    this.helping = helping;
  }

  /** @return the queue */
  public List<String> getQueue() {
    return queue;
  }

  /** @return the helping */
  public Helping getHelping() {
    return helping;
  }

  public static class Helping {
    private String email;
    private String workspace;
    private static final String WORKSPACE = "/workspace/?workspaceID=";

    public Helping(String email, String workspaceID) {
      this.email = email;
      this.workspace = WORKSPACE + workspaceID;
    }

    /** @return the email */
    public String getEmail() {
      return email;
    }

    /** @return the workspace */
    public String getWorkspace() {
      return workspace;
    }
  }
}

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

  public static class Helping {
    private String email;
    private String workspace;
    private static final String WORKSPACE = "/workspace/?workspaceID=";

    public Helping(String email, String workspaceID) {
      this.email = email;
      this.workspace = WORKSPACE + workspaceID;
    }
  }
}

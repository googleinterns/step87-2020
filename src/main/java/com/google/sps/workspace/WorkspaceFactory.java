package com.google.sps.workspace;

import com.google.api.core.ApiFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class WorkspaceFactory {
  private static WorkspaceFactory FACTORY = new WorkspaceFactory();

  private WorkspaceFactory() {}

  public static WorkspaceFactory getInstance() {
    return FACTORY;
  }

  public Workspace fromWorkspaceID(String workspaceID) throws IOException {
    return new Workspace(
        FirebaseDatabase.getInstance(FirebaseAppManager.getApp())
            .getReference()
            .child(workspaceID));
  }

  public Workspace create(String classID)
      throws InterruptedException, ExecutionException, IOException {
    return create(
        classID, FirebaseDatabase.getInstance(FirebaseAppManager.getApp()).getReference().push());
  }

  public Workspace create(String classID, DatabaseReference reference)
      throws InterruptedException, ExecutionException, IOException {
    ApiFuture<Void> classFuture = reference.child("classID").setValueAsync(classID);

    classFuture.get();

    return new Workspace(reference);
  }
}

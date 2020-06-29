package com.google.sps.workspace;

import com.google.api.core.ApiFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.Objects;
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

  public Workspace fromStudentAndTA(String studentUID, String TaUID)
      throws InterruptedException, ExecutionException, IOException {
    return fromStudentAndTA(
        studentUID,
        TaUID,
        FirebaseDatabase.getInstance(FirebaseAppManager.getApp()).getReference().push());
  }

  public Workspace fromStudentAndTA(String studentUID, String TaUID, DatabaseReference reference)
      throws InterruptedException, ExecutionException, IOException {
    ApiFuture<Void> studentFuture =
        reference.child("student").setValueAsync(Objects.requireNonNull(studentUID));
    ApiFuture<Void> taFuture = reference.child("ta").setValueAsync(Objects.requireNonNull(TaUID));

    studentFuture.get();
    taFuture.get();

    return new Workspace(reference);
  }
}

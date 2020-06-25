package com.google.sps.workspace;

import com.google.api.core.ApiFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 * Provides an interface to access and modify workspaces in the datastore. The contents of this
 * object will only reflect the current state of the entities. It will not update with the
 * datastore.
 */
public class Workspace {
  private final DatabaseReference reference;

  public Workspace(String studentUID, String TaUID)
      throws IOException, InterruptedException, ExecutionException {
    reference = FirebaseDatabase.getInstance(FirebaseAppManager.getApp()).getReference().push();

    ApiFuture<Void> studentFuture = reference.child("student").setValueAsync(studentUID);
    ApiFuture<Void> taFuture = reference.child("ta").setValueAsync(TaUID);

    studentFuture.get();
    taFuture.get();
  }

  public Workspace(String workspaceID) throws IOException {
    reference =
        FirebaseDatabase.getInstance(FirebaseAppManager.getApp()).getReference().child(workspaceID);
  }

  /** @return the studentUID */
  public Future<String> getStudentUID() {
    CompletableFuture<String> future = new CompletableFuture<>();

    reference
        .child("student")
        .addListenerForSingleValueEvent(
            new ValueEventListener() {

              @Override
              public void onDataChange(DataSnapshot snapshot) {
                future.complete((String) snapshot.getValue());
              }

              @Override
              public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
              }
            });

    return future;
  }

  /** @return the taUID */
  public Future<String> getTaUID() {
    CompletableFuture<String> future = new CompletableFuture<>();

    reference
        .child("ta")
        .addListenerForSingleValueEvent(
            new ValueEventListener() {

              @Override
              public void onDataChange(DataSnapshot snapshot) {
                future.complete((String) snapshot.getValue());
              }

              @Override
              public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
              }
            });

    return future;
  }

  public Future<List<WorkspaceFile>> getFiles() {
    CompletableFuture<List<WorkspaceFile>> future = new CompletableFuture<>();

    reference
        .child("files")
        .addListenerForSingleValueEvent(
            new ValueEventListener() {

              @Override
              public void onDataChange(DataSnapshot snapshot) {
                try {
                  ArrayList<WorkspaceFile> children = new ArrayList<>();
                  for (DataSnapshot child : snapshot.getChildren()) {
                    children.add(new WorkspaceFile(child));
                  }

                  future.complete(children);
                } catch (Exception e) {
                  future.completeExceptionally(e);
                }
              }

              @Override
              public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
              }
            });

    return future;
  }

  public String getArchive(OutputStream out)
      throws InterruptedException, ExecutionException, IOException {
    List<WorkspaceFile> files = getFiles().get();

    TarArchiveOutputStream tarOut =
        new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(out)));

    for (WorkspaceFile file : files) {
      byte contents[] = file.getContents().get().getBytes();

      TarArchiveEntry entry = new TarArchiveEntry(file.getFilename());
      entry.setSize(contents.length);

      tarOut.putArchiveEntry(entry);
      tarOut.write(contents);

      tarOut.closeArchiveEntry();
    }

    tarOut.close();

    return "";
  }

  /** @return the workspaceID */
  public String getWorkspaceID() {
    return reference.getKey();
  }
}

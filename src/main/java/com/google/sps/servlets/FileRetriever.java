package com.google.sps.servlets;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/retrieveFile")
public class FileRetriever extends HttpServlet {

  public FileRetriever() throws IOException {}

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    FirebaseApp app = FirebaseAppManager.getApp();
    FirebaseDatabase db = FirebaseDatabase.getInstance(app);
    db.goOnline();
    String refPath = RequestUtils.getParameter(req, "path", "");

    DatabaseReference ref = db.getReference(refPath);

    ref.child("t").setValueAsync("t");

    CompletableFuture<String> future = new CompletableFuture<>();
    ref.addListenerForSingleValueEvent(
        new ValueEventListener() {

          @Override
          public void onDataChange(DataSnapshot snapshot) {
            ref.removeEventListener(this);

            // WorkspaceFile file = new WorkspaceFile(snapshot);
            future.complete(Thread.currentThread().getName());

            // try {
            // future.complete(file.getContents().get());
            // } catch (InterruptedException | ExecutionException e) {
            // future.completeExceptionally(e);
            // }
          }

          @Override
          public void onCancelled(DatabaseError error) {
            // TODO Auto-generated method stub
            future.completeExceptionally(error.toException());
          }
        });

    try {
      resp.getWriter().println(future.get());
      resp.getWriter().println(Thread.currentThread().getName());
    } catch (InterruptedException | ExecutionException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getStackTrace().toString());
    }
  }
}

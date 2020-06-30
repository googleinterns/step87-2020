package com.google.sps.servlets;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.common.annotations.VisibleForTesting;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.protobuf.ByteString;
import com.google.sps.firebase.FirebaseAppManager;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/workspace/queueDownload")
public class QueueDownload extends HttpServlet {
  private WorkspaceFactory workspaceFactory;
  private FirebaseAuth auth;

  public QueueDownload() {}

  protected QueueDownload(WorkspaceFactory workspaceFactory, FirebaseAuth auth) {
    this.workspaceFactory = workspaceFactory;
    this.auth = auth;
  }

  @Override
  public void init() throws ServletException {
    super.init();
    workspaceFactory = WorkspaceFactory.getInstance();
    try {
      auth = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @VisibleForTesting
  protected CloudTasksClient getClient() throws IOException {
    return CloudTasksClient.create();
  }

  @VisibleForTesting
  protected String getProjectID() {
    return System.getenv("GOOGLE_CLOUD_PROJECT");
  }

  @VisibleForTesting
  protected String getQueueName() {
    return System.getenv("DOWNLOAD_QUEUE_ID");
  }

  @VisibleForTesting
  protected String getLocation() {
    return System.getenv("LOCATION_ID");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String idToken = req.getParameter("idToken");
    try {
      FirebaseToken tok = auth.verifyIdToken(idToken);

      Workspace w = workspaceFactory.fromWorkspaceID(req.getParameter("workspaceID"));

      if (w.getStudentUID().get().equals(tok.getUid()) || w.getTaUID().get().equals(tok.getUid())) {

        String downloadID = w.newDownloadID();

        try (CloudTasksClient client = getClient()) {
          String queuePath = QueueName.of(getProjectID(), getLocation(), getQueueName()).toString();

          Task.Builder taskBuilder =
              Task.newBuilder()
                  .setAppEngineHttpRequest(
                      AppEngineHttpRequest.newBuilder()
                          .setBody(
                              ByteString.copyFrom(
                                  String.join(",", w.getWorkspaceID(), downloadID),
                                  Charset.defaultCharset()))
                          .setRelativeUri("/tasks/prepareDownload")
                          .setHttpMethod(HttpMethod.POST)
                          .build());

          client.createTask(queuePath, taskBuilder.build());

          resp.getWriter().print(downloadID);
        }
      }
    } catch (IllegalArgumentException | FirebaseAuthException e) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
    } catch (InterruptedException | ExecutionException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}

package com.google.sps.servlets;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/workspace/queueDownload")
public class QueueDownload extends HttpServlet {
  WorkspaceFactory workspaceFactory = WorkspaceFactory.getInstance();

  public QueueDownload() {}

  protected QueueDownload(WorkspaceFactory workspaceFactory) {
    this.workspaceFactory = workspaceFactory;
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
    Workspace w = workspaceFactory.fromWorkspaceID(req.getParameter("workspaceID"));

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
}

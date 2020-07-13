package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.sps.environment.Environment;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/environment")
public class GetEnvironment extends HttpServlet {
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
    return System.getenv("EXECUTION_QUEUE_ID");
  }

  @VisibleForTesting
  protected String getLocation() {
    return System.getenv("LOCATION_ID");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String envID = req.getParameter("envID");

    try {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Entity e = datastore.get(KeyFactory.stringToKey(envID));

      resp.getWriter()
          .print(
              new Gson()
                  .toJson(
                      new Environment(
                          (String) e.getProperty("name"),
                          (String) e.getProperty("status"),
                          KeyFactory.keyToString(e.getKey()))));
    } catch (EntityNotFoundException | IllegalArgumentException e) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String envID = req.getParameter("envID");

    try (CloudTasksClient client = getClient()) {
      String queuePath = QueueName.of(getProjectID(), getLocation(), getQueueName()).toString();

      Task.Builder taskBuilder =
          Task.newBuilder()
              .setAppEngineHttpRequest(
                  AppEngineHttpRequest.newBuilder()
                      .setBody(ByteString.copyFrom(envID, Charset.defaultCharset()))
                      .setRelativeUri("/tasks/deleteEnv")
                      .setHttpMethod(HttpMethod.POST)
                      .build());

      client.createTask(queuePath, taskBuilder.build());

      resp.getWriter().print(envID);
    }
  }
}

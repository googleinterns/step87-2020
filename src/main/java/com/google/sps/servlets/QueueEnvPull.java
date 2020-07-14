package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.sps.tasks.PullNewEnvironment;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/queueEnvPull")
public class QueueEnvPull extends HttpServlet {

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
    String classID = req.getParameter("classID");
    String image = req.getParameter("image");
    String tag = req.getParameter("tag");
    String name = req.getParameter("name");
    Key classKey = KeyFactory.stringToKey(classID);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    PreparedQuery q =
        datastore.prepare(
            new Query("Environment")
                .setFilter(
                    CompositeFilterOperator.and(
                        new FilterPredicate(
                            "image",
                            FilterOperator.EQUAL,
                            PullNewEnvironment.getImageName(classID, image)),
                        new FilterPredicate("class", FilterOperator.EQUAL, classKey))));

    if (q.countEntities(FetchOptions.Builder.withLimit(1)) == 0) {
      Entity e = new Entity("Environment");
      e.setProperty("status", "pulling");
      e.setProperty("class", classKey);
      e.setProperty("name", name);
      datastore.put(e);
      String envID = KeyFactory.keyToString(e.getKey());

      try (CloudTasksClient client = getClient()) {
        String queuePath = QueueName.of(getProjectID(), getLocation(), getQueueName()).toString();

        Task.Builder taskBuilder =
            Task.newBuilder()
                .setAppEngineHttpRequest(
                    AppEngineHttpRequest.newBuilder()
                        .setBody(
                            ByteString.copyFrom(
                                String.join(",", envID, classID, image, tag),
                                Charset.defaultCharset()))
                        .setRelativeUri("/tasks/pullEnv")
                        .setHttpMethod(HttpMethod.POST)
                        .build());

        client.createTask(queuePath, taskBuilder.build());

        resp.getWriter().print(envID);
      }
    } else {
      resp.sendError(HttpServletResponse.SC_CONFLICT);
    }
  }
}
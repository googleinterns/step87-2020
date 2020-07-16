package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.sps.environment.Environment;
import com.google.sps.tasks.TaskSchedulerFactory;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/environment")
public class EnvironmentServlet extends HttpServlet {
  private TaskSchedulerFactory taskSchedulerFactory;

  @Override
  public void init() throws ServletException {
    taskSchedulerFactory = TaskSchedulerFactory.getInstance();
  }

  @VisibleForTesting
  protected String getQueueName() {
    return System.getenv("EXECUTION_QUEUE_ID");
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

    try {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Entity e = datastore.get(KeyFactory.stringToKey(envID));
      e.setProperty("status", "deleting");
      datastore.put(e);

      taskSchedulerFactory.create(getQueueName(), "/tasks/deleteEnv")
          .schedule(envID);

      resp.getWriter().print(envID);
    } catch (EntityNotFoundException e) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }
}

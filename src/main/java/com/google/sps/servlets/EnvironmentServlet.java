package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.sps.authentication.Authenticator;
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
  private Authenticator auth;
  @VisibleForTesting protected String QUEUE_NAME;

  @Override
  public void init() throws ServletException {
    taskSchedulerFactory = TaskSchedulerFactory.getInstance();
    QUEUE_NAME = System.getenv("EXECUTION_QUEUE_ID");
    try {
      auth = new Authenticator();
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String envID = req.getParameter("envID");
    String idToken = req.getParameter("idToken");

    try {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Entity e = datastore.get(KeyFactory.stringToKey(envID));

      if (auth.verifyInClass(idToken, (Key) e.getProperty("class"))) {
        resp.getWriter()
            .print(
                new Gson()
                    .toJson(
                        new Environment(
                            (String) e.getProperty("name"),
                            (String) e.getProperty("status"),
                            KeyFactory.keyToString(e.getKey()))));
      } else {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      }
    } catch (EntityNotFoundException | IllegalArgumentException e) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String envID = req.getParameter("envID");

    String idToken = req.getParameter("idToken");

    try {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Entity e = datastore.get(KeyFactory.stringToKey(envID));
      if (auth.verifyTaOrOwner(idToken, (Key) e.getProperty("class"))) {
        e.setProperty("status", "deleting");
        datastore.put(e);

        taskSchedulerFactory.create(QUEUE_NAME, "/tasks/deleteEnv").schedule(envID);

        resp.getWriter().print(envID);
      } else {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      }
    } catch (EntityNotFoundException e) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }
}

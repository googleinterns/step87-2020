package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that creates a new class Datastore. */
@WebServlet("/newclass")
public final class NewClass extends HttpServlet {
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the input from the form.
    // navigate to /_ah/admin to view Datastore

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    System.setProperty(
        DatastoreServiceConfig.DATASTORE_EMPTY_LIST_SUPPORT, Boolean.TRUE.toString());

    String className = request.getParameter("className");

    Entity classEntity = new Entity("Class");
    classEntity.setProperty("owner", "");
    classEntity.setProperty("name", className);
    classEntity.setProperty("beingHelped", "");
    classEntity.setProperty("studentQueue", Collections.emptyList());

    datastore.put(classEntity);
  }
}
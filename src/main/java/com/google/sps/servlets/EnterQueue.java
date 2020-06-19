package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that creates a new class Datastore. */
@WebServlet("/enterqueue")
public final class EnterQueue extends HttpServlet {
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the input from the form.
    // navigate to /_ah/admin to view Datastore

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    System.setProperty(
        DatastoreServiceConfig.DATASTORE_EMPTY_LIST_SUPPORT, Boolean.TRUE.toString());

    try {
      Key classCode = KeyFactory.stringToKey(request.getParameter("classCode"));
      Entity classEntity = datastore.get(classCode);
      ArrayList<String> updatedQueue = (ArrayList) classEntity.getProperty("studentQueue");
      updatedQueue.add("student"); // temporary until authentification is implemented
      classEntity.setProperty("studentQueue", updatedQueue);

      datastore.put(classEntity);
    } catch (EntityNotFoundException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}

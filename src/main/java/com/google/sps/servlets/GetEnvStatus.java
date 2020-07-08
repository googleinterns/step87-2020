package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/envStatus")
public class GetEnvStatus extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String envID = req.getParameter("envID");

    try {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Entity e = datastore.get(KeyFactory.stringToKey(envID));

      resp.getWriter().print(e.getProperty("status"));
    } catch (EntityNotFoundException e) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }
}

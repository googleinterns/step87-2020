package com.google.sps.servlets.course;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.gson.Gson;
import com.google.sps.authentication.Authenticator;
import com.google.sps.models.Environment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/getEnvironments")
public class GetEnvironments extends HttpServlet {
  private Authenticator auth;

  @Override
  public void init() throws ServletException {
    try {
      auth = new Authenticator();
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String classID = req.getParameter("classID");
    String idToken = req.getParameter("idToken");
    String status = req.getParameter("status");

    if (auth.verifyInClass(idToken, classID)) {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

      Filter filter;
      if (status != null) {
        filter =
            CompositeFilterOperator.and(
                new FilterPredicate("class", FilterOperator.EQUAL, KeyFactory.stringToKey(classID)),
                new FilterPredicate("status", FilterOperator.EQUAL, status));
      } else {
        filter =
            new FilterPredicate("class", FilterOperator.EQUAL, KeyFactory.stringToKey(classID));
      }

      PreparedQuery q = datastore.prepare(new Query("Environment").setFilter(filter));

      List<Environment> envs = new ArrayList<>();

      for (Entity e : q.asIterable()) {
        envs.add(
            new Environment(
                (String) e.getProperty("name"),
                (String) e.getProperty("status"),
                KeyFactory.keyToString(e.getKey())));
      }

      resp.getWriter().print(new Gson().toJson(envs));
    } else {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.gson.Gson;
import com.google.sps.environment.Environment;
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
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String classID = req.getParameter("classID");
    String idToken = req.getParameter("idToken");

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    PreparedQuery q =
        datastore.prepare(
            new Query("Environment")
                .setFilter(
                    new FilterPredicate(
                        "class", FilterOperator.EQUAL, KeyFactory.stringToKey(classID))));

    List<Environment> envs = new ArrayList<>();

    for (Entity e : q.asIterable()) {
      envs.add(
          new Environment(
              (String) e.getProperty("name"),
              (String) e.getProperty("status"),
              KeyFactory.keyToString(e.getKey())));
    }

    resp.getWriter().print(new Gson().toJson(envs));
  }
}

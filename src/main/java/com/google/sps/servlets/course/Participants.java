package com.google.sps.servlets.course;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/participants")
public class Participants extends HttpServlet {
  private FirebaseAuth authInstance;
  private DatastoreService datastore;

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    datastore = DatastoreServiceFactory.getDatastoreService();

    try {
      ArrayList<String> classParticipants = new ArrayList<String>();

      // Retrieve class entity
      String classCode = request.getParameter("classCode").trim();
      Key classKey = KeyFactory.stringToKey(classCode);

      String type = request.getParameter("type"); // Students or TAs

      PreparedQuery results;

      if (!type.equals("student")) {
        // Filter for TAs that teach this class
        Query query =
            new Query("User")
                .setFilter(new FilterPredicate("taClasses", FilterOperator.EQUAL, classKey));
        results = datastore.prepare(query);
      } else {
        // Filter for students that are in this class
        Query query2 =
            new Query("User")
                .setFilter(
                    new FilterPredicate("registeredClasses", FilterOperator.EQUAL, classKey));
        results = datastore.prepare(query2);
      }

      // Store the emails
      for (Entity entity : results.asIterable()) {
        String email = (String) entity.getProperty("userEmail");
        classParticipants.add(email);
      }

      response.setContentType("application/json;");
      Gson gson = new Gson();
      String json = gson.toJson(classParticipants);
      response.getWriter().println(json);

    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    // try {
    //   ArrayList<String> classTAs = new ArrayList<String>();

    //   // Retrieve class entity
    //   String classCode = request.getParameter("classCode").trim();
    //   Key classKey = KeyFactory.stringToKey(classCode);

    //   // Filter for TAs that teach this class
    //   Query query =
    //       new Query("User")
    //           .setFilter(new FilterPredicate("taClasses", FilterOperator.EQUAL, classKey));
    //   PreparedQuery results = datastore.prepare(query);

    //   // Store the TA emails
    //   for (Entity entity : results.asIterable()) {
    //     String email = (String) entity.getProperty("userEmail");
    //     classTAs.add(email);
    //   }

    //   response.setContentType("application/json;");
    //   Gson gson = new Gson();
    //   String json = gson.toJson(classTAs);
    //   response.getWriter().println(json);

    // } catch (IllegalArgumentException e) {
    //   response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    // }
  }
}

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Retrieve class visit data by date
@WebServlet("/visit-date")
public class VisitsByDate extends HttpServlet {

  FirebaseAuth authInstance;

  // Get the current session
  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  // Obtain a query of class visits, filter by unique class, and store the visits by date
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    try {
      ArrayList<Date> dates = new ArrayList<Date>();
      ArrayList<Long> classVisits = new ArrayList<Long>();

      // The class filter will be the unique class's key
      String classCode = request.getParameter("classCode").trim(); // Hidden parameter
      Key classKey = KeyFactory.stringToKey(classCode);

      Filter classFilter = new FilterPredicate("classKey", FilterOperator.EQUAL, classKey);

      // Obtain visits from datastore and filter them into results query;
      // Sort by most recent date
      Query query =
          new Query("Visit").addSort("date", SortDirection.DESCENDING).setFilter(classFilter);
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      PreparedQuery results = datastore.prepare(query);

      // Store the date and number of visits into two separate lists
      for (Entity entity : results.asIterable()) {
        Date date = (Date) entity.getProperty("date");
        long visitsForThisDate = (long) entity.getProperty("numVisits");

        dates.add(date);
        classVisits.add(visitsForThisDate);
      }

      // Send both class dates list and visits to line chart function
      VisitParentDates parent = new VisitParentDates(dates, classVisits);
      Gson gson = new Gson();
      String json = gson.toJson(parent);
      response.getWriter().println(json);

    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}

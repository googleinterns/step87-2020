package com.google.sps.servlets.course;

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
import com.google.sps.models.WaitData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Retrieve class queue wait time data by date
@WebServlet("/wait-time")
public class WaitTime extends HttpServlet {

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

  // Helper function to calculate average wait time for each date
  private long calculateAverage(List<Long> times) {
    long sum = 0;
    if (!times.isEmpty()) {
      for (long time : times) {
        sum += time;
      }
      return sum / times.size();
    }
    return sum;
  }

  // Obtain a query of class visits, filter by unique class, and store the visits by date
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    try {
      ArrayList<Date> dates = new ArrayList<Date>();
      ArrayList<ArrayList<Long>> waitTimes = new ArrayList<ArrayList<Long>>();

      // The class filter will be the unique class's key
      String classCode = request.getParameter("classCode").trim(); // Hidden parameter
      Key classKey = KeyFactory.stringToKey(classCode);

      Filter classFilter = new FilterPredicate("classKey", FilterOperator.EQUAL, classKey);

      // Obtain waits from datastore and filter them into results query;
      // Sort by most recent date
      Query query =
          new Query("Wait").addSort("date", SortDirection.DESCENDING).setFilter(classFilter);
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      PreparedQuery results = datastore.prepare(query);

      // Store the date and wait time lists into two separate lists
      for (Entity entity : results.asIterable()) {
        Date date = (Date) entity.getProperty("date");
        ArrayList<Long> waitTimeList = (ArrayList<Long>) entity.getProperty("waitDurations");

        dates.add(date);
        waitTimes.add(waitTimeList);
      }

      // List that holds average wait time
      ArrayList<Long> finalWaitAverage = new ArrayList<Long>();

      // Calculate average wait times for each date
      for (ArrayList<Long> list : waitTimes) {
        long average = calculateAverage(list);
        finalWaitAverage.add(average);
      }

      // Send both class dates list and wait-times to line chart function
      WaitData parent = new WaitData(dates, finalWaitAverage);
      Gson gson = new Gson();
      String json = gson.toJson(parent);
      response.getWriter().println(json);

    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}

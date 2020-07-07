package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Once class owner submits a TA email, retrieve that user and add them as a TA to the class
@WebServlet("/add-ta")
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
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    ArrayList<Date> dates = new ArrayList<Date>();
    ArrayList<Long> classVisits = new ArrayList<Long>();

    // The class filter will be the unique class's key
    String classCode = request.getParameter("classCode").trim();
    Key classKey = KeyFactory.stringToKey(classCode);

    // Obtain visits from datastore and filter them into results query
    Query query = new Query("Visit")
        .addSort("date", SortDirection.DESCENDING)
        .setFilter(classKey);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Store the date and number of visits into two separate lists
    for (Entity entity : results.asIterable()) {
      Date date = (Date) entity.getProperty("date");
      long visitsForThisDate = (long) entity.getProperty("numVisits");

      dates.add(date);
      classVisits.add(visitsForThisDate);
    }

    
  }
}
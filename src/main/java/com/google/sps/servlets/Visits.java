package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Retrieve the number of visits per class from Visit entity and send to chart
@WebServlet("/visits")
public class Visits extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    ArrayList<String> listOfClassNames = new ArrayList<String>();
    ArrayList<Integer> visitsPerClass = new ArrayList<Integer>();

    // Obtain visits from datastore and filter them into results query
    Query query = new Query("Visit");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Store the class name and number of visits into two separate lists
    for (Entity entity : results.asIterable()) {
      String className = entity.getProperty("???");
      Integer classVisits = entity.getProperty("numVisits");

      listOfClassNames.add(className);
      visitsPerClass.add(classVisits);
    }
  }
}
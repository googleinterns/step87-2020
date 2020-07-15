package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Once class owner submits a TA email, retrieve that user and add them as a TA to the class
@WebServlet("/submit-roster")
public class SubmitRoster extends HttpServlet {

  private FirebaseAuth authInstance;

  // Get the current session
  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  // Add a user to the datastore
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    String rosterNames = request.getParameter("roster").trim();

    // Split the emails and collapse whitespaces
    List<String> allClassEmails = Arrays.asList(rosterNames.split("\\s*,\\s*"));

    // Find the corresponding class Key
    String classCode = request.getParameter("classCode").trim();
    Key classKey = KeyFactory.stringToKey(classCode);
    ArrayList<Key> registered = new ArrayList<Key>();
    registered.add(classKey);

    for (String email : allClassEmails) {
      // Prevent creating duplicate users
      Query query =
          new Query("User")
              .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, email));

      if (datastore.prepare(query).countEntities() == 0) {

        Entity user = new Entity("User");
        user.setProperty("userEmail", email);
        user.setProperty("registeredClasses", registered);
        user.setProperty("ownedClasses", Collections.emptyList());
        user.setProperty("taClasses", Collections.emptyList());

        datastore.put(user);
      } else {
        // Entity user = query.asSingleEntity();
      }
    }

    // Redirect to the class dashboard page
    response.sendRedirect("/dashboard.html?classCode=" + classCode);
  }
}

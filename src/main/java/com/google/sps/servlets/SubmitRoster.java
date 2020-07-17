package com.google.sps.servlets;

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
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Once class owner submits a roster, add or update those student users class registration
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

  // Add a student user to the datastore
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    String rosterNames = request.getParameter("roster").trim();

    // Split the emails and collapse whitespaces
    List<String> allClassEmails = Arrays.asList(rosterNames.split("\\s*,\\s*"));

    // Find the corresponding class Key
    String classCode = request.getParameter("classCode").trim();
    Key classKey = KeyFactory.stringToKey(classCode);

    for (String email : allClassEmails) {
      // Look for the student in the user datastore
      PreparedQuery queryUser =
          datastore.prepare(
              new Query("User")
                  .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, email)));

      Entity user;

      // If the student user entity doesnt exist yet, create one
      if (queryUser.countEntities() == 0) {

        List<Key> regClassesList = Arrays.asList(classKey);

        user = new Entity("User");
        user.setProperty("userEmail", email);
        user.setProperty("registeredClasses", regClassesList);
        user.setProperty("ownedClasses", Collections.emptyList());
        user.setProperty("taClasses", Collections.emptyList());

        datastore.put(user);
      } else {
        // If student already exists, update their registered class list
        user = queryUser.asSingleEntity();
        List<Key> regClassesList = (List<Key>) user.getProperty("registeredClasses");

        // Do not add a class that is already in the registered list
        if (regClassesList.contains(classKey)) {
          response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        regClassesList.add(classKey);
        user.setProperty("registeredClasses", regClassesList);

        datastore.put(user);
      }
    }

    // Redirect to the class dashboard page
    response.sendRedirect("/dashboard.html?classCode=" + classCode);
  }
}

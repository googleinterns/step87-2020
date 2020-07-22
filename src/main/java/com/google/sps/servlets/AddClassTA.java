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
import com.google.appengine.api.datastore.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.sps.ApplicationDefaults;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Once class owner submits a TA email, retrieve that user and add them as a TA to the class
@WebServlet("/add-class-ta")
public class AddClassTA extends HttpServlet {

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

  // Add a TA to the user datastore
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    int retries = 10;

    try {
      while (true) {
        Transaction txn = datastore.beginTransaction();

        try {

          // Obtain the teaching assistant email and search for the user
          String teachingAssistantEmail = request.getParameter("taEmail").trim();

          // Find the corresponding class Key
          String classCode = request.getParameter("classCode").trim();
          Key classKey = KeyFactory.stringToKey(classCode);

          // Look for the TA in the user datastore
          PreparedQuery queryUser =
              datastore.prepare(
                  new Query("User")
                      .setFilter(
                          new FilterPredicate(
                              "userEmail", FilterOperator.EQUAL, teachingAssistantEmail)));

          Entity user;

          // If the TA user entity doesnt exist yet, create one
          if (queryUser.countEntities() == 0) {

            List<Key> taClassesList = Arrays.asList(classKey);

            user = new Entity("User");
            user.setProperty("userEmail", teachingAssistantEmail);
            user.setProperty("registeredClasses", Collections.emptyList());
            user.setProperty("ownedClasses", Collections.emptyList());
            user.setProperty("taClasses", taClassesList);

            datastore.put(txn, user);
          } else {
            // If TA user already exists, update their ta class list
            user = queryUser.asSingleEntity();
            List<Key> taClassesList = (List<Key>) user.getProperty("taClasses");

            // Do not add a class that is already in the TA list
            if (!taClassesList.contains(classKey)) {
              taClassesList.add(classKey);
              user.setProperty("taClasses", taClassesList);

              datastore.put(txn, user);
            }
          }

          // Redirect to the class dashboard page
          response.sendRedirect(ApplicationDefaults.DASHBOARD + classCode);

          txn.commit();
          break;

        } catch (ConcurrentModificationException e) {
          if (retries == 0) {
            throw e;
          }

          // Allow retry to occur
          --retries;
        } finally {
          if (txn.isActive()) {
            txn.rollback();
          }
        }
      }

    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}

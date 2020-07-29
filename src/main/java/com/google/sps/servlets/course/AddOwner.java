package com.google.sps.servlets.course;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.sps.firebase.FirebaseAppManager;
import com.google.sps.tasks.TaskSchedulerFactory;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/add-owner")
public class AddOwner extends HttpServlet {

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

  // Add an owner to the class
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    int retries = 10;

    try {
      while (true) {
        Transaction txn = datastore.beginTransaction();

        try {

          // Obtain the owner email and search for the user
          String ownerEmail = request.getParameter("ownerEmail").trim();

          // Find the corresponding class Key
          String classCode = request.getParameter("classCode").trim();
          Key classKey = KeyFactory.stringToKey(classCode);

          // Look for the owner in the user datastore
          PreparedQuery queryUser =
              datastore.prepare(
                  new Query("User")
                      .setFilter(
                          new FilterPredicate(
                              "userEmail", FilterOperator.EQUAL, ownerEmail)));

          Entity user;

          // If the owner user entity doesnt exist yet, create one
          if (queryUser.countEntities() == 0) {

            List<Key> taClassesList = Arrays.asList(classKey);

            user = new Entity("User");
            user.setProperty("userEmail", ownerEmail);
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
    
    try {
      String classCode = request.getParameter("classCode");
      Key classKey = KeyFactory.stringToKey(classCode);

      String idToken = request.getParameter("idToken");
      FirebaseToken decodedToken = authInstance.verifyIdToken(idToken);

      Entity ownerCheck = datastore.get(classKey);
      if (ownerCheck.getProperty("owner").equals(decodedToken.getUid())) {
        
      }
    }
  }
}
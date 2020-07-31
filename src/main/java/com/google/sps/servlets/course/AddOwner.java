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
import com.google.appengine.api.datastore.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.sps.ApplicationDefaults;
import com.google.sps.authentication.Authenticator;
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

@WebServlet("/add-owner")
public class AddOwner extends HttpServlet {

  private FirebaseAuth authInstance;
  private Authenticator auth;

  // Get the current session
  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
      auth = new Authenticator();
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  // Add an owner to the class
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    int retries = 5;

    // Obtain the owner email and id token and search for the user
    String ownerEmail = request.getParameter("ownerEmail").trim();
    String idToken = request.getParameter("idTokenOwner");

    // Find the corresponding class Key
    String classCode = request.getParameter("classCode").trim();
    Key classKey = KeyFactory.stringToKey(classCode);

    if (auth.verifyOwner(idToken, classKey)) {
      try {
        while (true) {
          Transaction txn = datastore.beginTransaction();

          try {
            // Look for the owner in the user datastore
            PreparedQuery queryUser =
                datastore.prepare(
                    new Query("User")
                        .setFilter(
                            new FilterPredicate("userEmail", FilterOperator.EQUAL, ownerEmail)));

            Entity user;

            // If the owner user entity doesnt exist yet, create one
            if (queryUser.countEntities() == 0) {

              List<Key> ownedClassesList = Arrays.asList(classKey);

              user = new Entity("User");
              user.setProperty("userEmail", ownerEmail);
              user.setProperty("registeredClasses", Collections.emptyList());
              user.setProperty("ownedClasses", ownedClassesList);
              user.setProperty("taClasses", Collections.emptyList());

              datastore.put(txn, user);
            } else {
              // If owner user already exists, update their owner class list
              user = queryUser.asSingleEntity();
              List<Key> ownedClassesList = (List<Key>) user.getProperty("ownedClasses");

              // Do not add a class that is already in the owner list
              if (!ownedClassesList.contains(classKey)) {
                ownedClassesList.add(classKey);
                user.setProperty("ownedClasses", ownedClassesList);

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
    } else {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}

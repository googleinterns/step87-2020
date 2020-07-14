package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Once class owner submits a TA email, retrieve that user and add them as a TA to the class
@WebServlet("/create-user")
public class CreateUser extends HttpServlet {

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
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    try {
      // Check if user entity already exists
      String userToken = request.getParameter("userToken");
      FirebaseToken decodedToken = authInstance.verifyIdToken(userToken);
      String userID = decodedToken.getUid(); // PROBLEM AT THIS LINE - NULL POINTER EXCEPTION

      UserRecord userRecord = authInstance.getUser(userID);
      String userEmail = userRecord.getEmail();

      // Prevent creating duplicate users
      Query query =
          new Query("User")
              .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, userEmail));

      if (datastore.prepare(query).countEntities() == 0) {

        Entity user = new Entity("User");
        user.setProperty("userEmail", userEmail);
        user.setProperty("registeredClasses", Collections.emptyList());
        user.setProperty("ownedClasses", Collections.emptyList());
        user.setProperty("taClasses", Collections.emptyList());

        datastore.put(user);
      } else {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
      }
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
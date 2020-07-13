package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
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
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    try {
      // Check if user entity already exists
      String userToken = request.getParameter("userToken");
      FirebaseToken decodedToken = authInstance.verifyIdToken(userToken);
      String userID = decodedToken.getUid();

      UserRecord userRecord = authInstance.getUser(userID);
      String userEmail = userRecord.getEmail();

      // Obtain users from datastore and filter them into results query
      Query query = new Query("User");
      PreparedQuery results = datastore.prepare(query);

      boolean userExists = false; // Assume user does not exist
      for (Entity entity : results.asIterable()) {
        if (entity.getProperty("userEmail") == userEmail) {
          userExists = true;
        }
      }

      // Create a new user entity if there is no existing one
      if (!userExists) {
        Entity user = new Entity("User");
        user.setProperty("userEmail", userEmail);
        user.setProperty("registeredClasses", Collections.emptyList());
        user.setProperty("ownedClasses", Collections.emptyList());
        user.setProperty("taClasses", Collections.emptyList());

        datastore.put(user);
      }
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}

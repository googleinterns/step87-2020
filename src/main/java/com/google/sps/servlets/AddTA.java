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

// Once class owner submits a TA email, retrieve that user and add them as a TA to the class
@WebServlet("/add-ta")
public class AddTA extends HttpServlet {
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

  // Add a TA to the datastore
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    try {
      // Obtain the teaching assistant email and search for the user
      String teachingAssistantEmail = request.getParameter("taEmail").trim();
      UserRecord userRecord = authInstance.getUserByEmail(teachingAssistantEmail);

      String classCode = request.getParameter("classCode").trim();

      // Create a TA entity with both a user ID and a class ID
      Entity taEntity = new Entity("TA");
      taEntity.setProperty("userKey", userRecord.getUid());
      taEntity.setProperty("classKey", classCode);

      datastore.put(taEntity); // Store the new TA with their class code

    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}

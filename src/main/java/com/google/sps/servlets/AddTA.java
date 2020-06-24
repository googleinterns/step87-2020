package com.google.sps.servlets;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Once class owner submits a TA email, retrieve that user and add them as a TA to the class
@WebServlet("/addta")
public class AddTA extends HttpServlet {
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    // Obtain the teaching assistant email and search for the user
    String teachingAssistantEmail = getParameter("taEmail").trim();
    UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(teachingAssistantEmail);
    System.out.println("Successfully fetched user data: " + userRecord.getUid());

    // Create a TA entity with both a user ID and a class ID
    // Entity taEntity = new Entity("TA");
    // taEntity.setProperty("userKey", userRecord.getUid());
    // taEntity.setProperty("classKey", "DEFAULT"); // Need to get class key from URL
  }
}

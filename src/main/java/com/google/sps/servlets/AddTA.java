// package com.google.sps.servlets;

// import com.google.appengine.api.datastore.DatastoreService;
// import com.google.appengine.api.datastore.DatastoreServiceFactory;
// import com.google.appengine.api.datastore.Entity;
// import com.google.appengine.api.datastore.EntityNotFoundException;
// import com.google.appengine.api.datastore.Key;
// import com.google.appengine.api.datastore.KeyFactory;
// import com.google.appengine.api.datastore.Transaction;
// import com.google.firebase.auth.FirebaseAuth;
// import com.google.firebase.auth.FirebaseAuthException;
// import com.google.firebase.auth.UserRecord;
// import com.google.sps.firebase.FirebaseAppManager;
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.ConcurrentModificationException;
// import javax.servlet.ServletConfig;
// import javax.servlet.ServletException;
// import javax.servlet.annotation.WebServlet;
// import javax.servlet.http.HttpServlet;
// import javax.servlet.http.HttpServletRequest;
// import javax.servlet.http.HttpServletResponse;

// // Once class owner submits a TA email, retrieve that user and add them as a TA to the class
// @WebServlet("/add-ta")
// public class AddTA extends HttpServlet {

//   private FirebaseAuth authInstance;

//   // Get the current session
//   @Override
//   public void init(ServletConfig config) throws ServletException {
//     try {
//       authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
//     } catch (IOException e) {
//       throw new ServletException(e);
//     }
//   }

//   // Add a TA to the class datastore
//   @Override
//   public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
// {

//     DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

//     int retries = 10;

//     try {
//       while (true) {
//         Transaction txn = datastore.beginTransaction();

//         try {

//           // Obtain the teaching assistant email and search for the user
//           String teachingAssistantEmail = request.getParameter("taEmail").trim();
//           UserRecord userRecord = authInstance.getUserByEmail(teachingAssistantEmail);

//           // Find the corresponding Class Entity
//           String classCode = request.getParameter("classCode").trim();
//           Key classKey = KeyFactory.stringToKey(classCode);
//           Entity classEntity = datastore.get(txn, classKey);

//           // Get the list of TAs
//           ArrayList<String> listOfClassTAs = (ArrayList) classEntity.getProperty("taList");

//           // Update the class's TA list to include the new TA
//           listOfClassTAs.add(teachingAssistantEmail);
//           classEntity.setProperty("taList", listOfClassTAs);
//           datastore.put(txn, classEntity);

//           // Redirect to the class dashboard page
//           response.sendRedirect("/dashboard.html?classCode=" + classCode);

//           txn.commit();
//           break;

//         } catch (ConcurrentModificationException e) {
//           if (retries == 0) {
//             throw e;
//           }

//           // Allow retry to occur
//           --retries;
//         } finally {
//           if (txn.isActive()) {
//             txn.rollback();
//           }
//         }
//       }

//     } catch (FirebaseAuthException e) {
//       response.sendError(HttpServletResponse.SC_NOT_FOUND);
//     } catch (IllegalArgumentException e) {
//       response.sendError(HttpServletResponse.SC_BAD_REQUEST);
//     } catch (EntityNotFoundException e) {
//       response.sendError(HttpServletResponse.SC_NOT_FOUND);
//     }
//   }
// }

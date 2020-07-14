package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.sps.firebase.FirebaseAppManager;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/notify-student")
public class NotifyStudent extends HttpServlet {
  private FirebaseAuth authInstance;
  private DatastoreService datastore;
  private WorkspaceFactory factory;

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
      factory = WorkspaceFactory.getInstance();
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the input from the form.
    // navigate to /_ah/admin to view Datastore

    datastore = DatastoreServiceFactory.getDatastoreService();
    System.setProperty(
        DatastoreServiceConfig.DATASTORE_EMPTY_LIST_SUPPORT, Boolean.TRUE.toString());

    try {
      String classCode = request.getParameter("classCode").trim();
      String taToken = request.getParameter("taToken");
      FirebaseToken decodedToken = authInstance.verifyIdToken(taToken);
      String taID = decodedToken.getUid();

      int retries = 10;
      while (true) {
        Transaction txn = datastore.beginTransaction();
        try {
          // Retrive class entity
          Key classKey = KeyFactory.stringToKey(classCode);
          Entity classEntity = datastore.get(txn, classKey);

          // Get studentID from studentEmail
          String studentEmail = request.getParameter("studentEmail");
          UserRecord userRecord = authInstance.getUserByEmail(studentEmail);
          String studentID = userRecord.getUid();

          // Update queue
          ArrayList<EmbeddedEntity> updatedQueue =
              (ArrayList<EmbeddedEntity>) classEntity.getProperty("studentQueue");
          EmbeddedEntity delEntity =
              updatedQueue.stream().filter(elem -> elem.hasProperty(studentID)).findFirst().get();
          updatedQueue.remove(delEntity);

          // Get workspace ID
          String workspaceID =
              factory.fromStudentAndTA(classCode, studentID, taID).getWorkspaceID();

          // Update beingHelped
          EmbeddedEntity beingHelped = (EmbeddedEntity) classEntity.getProperty("beingHelped");

          EmbeddedEntity queueInfo = new EmbeddedEntity();
          queueInfo.setProperty("taID", taID);
          queueInfo.setProperty("workspaceID", workspaceID);

          beingHelped.setProperty(studentID, queueInfo);

          classEntity.setProperty("studentQueue", updatedQueue);
          classEntity.setProperty("beingHelped", beingHelped);
          datastore.put(txn, classEntity);

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
    } catch (EntityNotFoundException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    } catch (InterruptedException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    } catch (ExecutionException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}

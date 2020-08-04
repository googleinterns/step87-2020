package com.google.sps.servlets.queue;

import com.google.appengine.api.datastore.DatastoreService;
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
import com.google.sps.authentication.Authenticator;
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

@WebServlet("/remove-from-queue")
public class RemoveFromQueue extends HttpServlet {
  private FirebaseAuth authInstance;
  private DatastoreService datastore;
  private WorkspaceFactory factory;
  private Authenticator auth;

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
      factory = WorkspaceFactory.getInstance();
      auth = new Authenticator();
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    datastore = DatastoreServiceFactory.getDatastoreService();

    try {
      String classCode = request.getParameter("classCode").trim();
      String idToken = request.getParameter("idToken");

      if (auth.verifyInClass(idToken, classCode)) {

        FirebaseToken decodedToken = authInstance.verifyIdToken(idToken);
        String studentID = decodedToken.getUid();

        int retries = 10;
        while (true) {
          Transaction txn = datastore.beginTransaction();
          try {
            // Retrive class entity
            Key classKey = KeyFactory.stringToKey(classCode);
            Entity classEntity = datastore.get(txn, classKey);

            // Get queue
            ArrayList<EmbeddedEntity> queue =
                (ArrayList<EmbeddedEntity>) classEntity.getProperty("studentQueue");
            EmbeddedEntity delEntity =
                queue.stream()
                    .filter(elem -> (((String) elem.getProperty("uID")).equals(studentID)))
                    .findFirst()
                    .orElse(null);

            // Delete workspace
            String workspaceID = (String) delEntity.getProperty("workspaceID");
            factory.fromWorkspaceID(workspaceID).delete();

            // Update queue
            queue.remove(delEntity);

            // Update entities
            classEntity.setProperty("studentQueue", queue);
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

      } else {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
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

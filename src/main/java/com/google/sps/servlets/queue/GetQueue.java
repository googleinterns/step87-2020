package com.google.sps.servlets.queue;

import static com.google.sps.utils.ExceptionWrapper.wrap;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;
import com.google.sps.authentication.Authenticator;
import com.google.sps.firebase.FirebaseAppManager;
import com.google.sps.queue.Queue;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Optional;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/get-queue")
public class GetQueue extends HttpServlet {
  private FirebaseAuth authInstance;
  private DatastoreService datastore;
  private Authenticator auth;

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
      auth = new Authenticator();
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    datastore = DatastoreServiceFactory.getDatastoreService();

    try {
      // Retrieve class entity
      String classCode = request.getParameter("classCode").trim();
      String idToken = request.getParameter("idToken");

      if (auth.verifyTaOrOwner(idToken, classCode)) {

        Key classKey = KeyFactory.stringToKey(classCode);
        Entity classEntity = datastore.get(classKey);

        String TaID = authInstance.verifyIdToken(idToken).getUid();

        // Get queue
        ArrayList<EmbeddedEntity> entityQueue =
            (ArrayList<EmbeddedEntity>) classEntity.getProperty("studentQueue");

        // Reconstruct queue using names
        ArrayList<String> queue = new ArrayList<String>();
        for (EmbeddedEntity elem : entityQueue) {
          String uid = (String) elem.getProperty("uID");
          UserRecord userRecord = authInstance.getUser(uid);
          String studentName = userRecord.getEmail();
          queue.add(studentName);
        }

        Optional<Queue.Helping> beingHelpedEntity =
            ((EmbeddedEntity) classEntity.getProperty("beingHelped"))
                .getProperties().entrySet().stream()
                    .map(
                        entry ->
                            new SimpleEntry<String, EmbeddedEntity>(
                                entry.getKey(), (EmbeddedEntity) entry.getValue()))
                    .filter(entry -> entry.getValue().getProperty("taID").equals(TaID))
                    .map(
                        wrap(
                            entry ->
                                new Queue.Helping(
                                    authInstance.getUser(entry.getKey()).getEmail(),
                                    (String) entry.getValue().getProperty("workspaceID"))))
                    .findFirst();

        response.setContentType("application/json;");

        Gson gson = new Gson();
        response.getWriter().print(gson.toJson(new Queue(queue, beingHelpedEntity.orElse(null))));

      } else {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
      }

    } catch (EntityNotFoundException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.repackaged.com.google.gson.Gson;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.sps.firebase.FirebaseAppManager;
import com.google.sps.queue.StudentStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/check-student")
public class CheckStudentStatus extends HttpServlet {
  private FirebaseAuth authInstance;
  private DatastoreService datastore;

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    datastore = DatastoreServiceFactory.getDatastoreService();
    System.setProperty(
        DatastoreServiceConfig.DATASTORE_EMPTY_LIST_SUPPORT, Boolean.TRUE.toString());

    try {
      // Find user ID
      String studentToken = request.getParameter("studentToken");
      FirebaseToken decodedToken = authInstance.verifyIdToken(studentToken);
      String studentID = decodedToken.getUid();

      // Retrive entity
      String classCode = request.getParameter("classCode").trim();
      Key classKey = KeyFactory.stringToKey(classCode);
      Entity classEntity = datastore.get(classKey);

      // Find position in queue
      ArrayList<EmbeddedEntity> queue =
          (ArrayList<EmbeddedEntity>) classEntity.getProperty("studentQueue");
      Optional<EmbeddedEntity> studentEntity =
          queue.stream().filter(elem -> elem.hasProperty(studentID)).findFirst();

      response.setContentType("application/json;");

      Gson gson = new Gson();
      if (studentEntity.isPresent()) {
        EmbeddedEntity embeddedEntity = (EmbeddedEntity) studentEntity.get().getProperty(studentID);
        response
            .getWriter()
            .print(
                gson.toJson(
                    new StudentStatus(
                        queue.indexOf(studentEntity.get()) + 1,
                        (String) embeddedEntity.getProperty("workspaceID"))));
      } else {
        EmbeddedEntity beingHelped = (EmbeddedEntity) classEntity.getProperty("beingHelped");
        if (beingHelped.hasProperty(studentID)) {
          EmbeddedEntity queueInfo = (EmbeddedEntity) beingHelped.getProperty(studentID);

          // Get workspace id
          String workspaceID = (String) queueInfo.getProperty("workspaceID");
          response.getWriter().print(gson.toJson(new StudentStatus(0, workspaceID)));
        } else {
          response.getWriter().print(gson.toJson(new StudentStatus(0, "")));
        }
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

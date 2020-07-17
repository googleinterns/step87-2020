package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/get-workspace")
public class GetWorkspace extends HttpServlet {
  private FirebaseAuth authInstance;
  private DatastoreService datastore;
  private Gson gson;
  private static final String WORKSPACE = "/workspace/?workspaceID=";

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
      gson = new Gson();
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
      // Retrive class entity
      String classCode = request.getParameter("classCode").trim();
      Key classKey = KeyFactory.stringToKey(classCode);
      Entity classEntity = datastore.get(classKey);

      String studentID;
      if (request.getParameter("studentToken") == null) {
        // Get studentID from studentEmail
        String studentEmail = request.getParameter("studentEmail");
        UserRecord userRecord = authInstance.getUserByEmail(studentEmail);
        studentID = userRecord.getUid();
      } else {
        // Get studentID from studentToken
        String studentToken = request.getParameter("studentToken");
        FirebaseToken decodedToken = authInstance.verifyIdToken(studentToken);
        studentID = decodedToken.getUid();
      }

      List<EmbeddedEntity> queue = (List<EmbeddedEntity>) classEntity.getProperty("studentQueue");

      Optional<EmbeddedEntity> studentEntity =
          queue.stream().filter(elem -> elem.hasProperty(studentID)).findFirst();

      String workspaceID;
      if (studentEntity.isPresent()) {
        workspaceID =
            (String)
                ((EmbeddedEntity) studentEntity.get().getProperty(studentID))
                    .getProperty("workspaceID");
      } else {
        EmbeddedEntity beingHelped = (EmbeddedEntity) classEntity.getProperty("beingHelped");
        EmbeddedEntity queueInfo = (EmbeddedEntity) beingHelped.getProperty(studentID);

        // Get workspace id
        workspaceID = (String) queueInfo.getProperty("workspaceID");
      }

      // Build workspace link
      String workspaceLink = WORKSPACE + workspaceID;

      response.setContentType("application/json;");
      response.getWriter().print(gson.toJson(workspaceLink));

    } catch (EntityNotFoundException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that creates a new class Datastore. */
@WebServlet("/newclass")
public class NewClass extends HttpServlet {
  private FirebaseAuth authInstance;
  private DatastoreService datastore;
  public static final String DASHBOARD = "/dashboard.html?classCode=";

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
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
      String className = request.getParameter("className").trim();

      // prevents creating duplicate classes
      Query query =
          new Query("Class")
              .setFilter(new FilterPredicate("name", FilterOperator.EQUAL, className));

      if (datastore.prepare(query).countEntities() == 0) {
        String idToken = request.getParameter("idToken");
        FirebaseToken decodedToken = authInstance.verifyIdToken(idToken);

        Entity classEntity = new Entity("Class");
        classEntity.setProperty("owner", decodedToken.getUid());
        classEntity.setProperty("name", className);

        EmbeddedEntity beingHelped = new EmbeddedEntity();
        classEntity.setProperty("beingHelped", beingHelped);

        classEntity.setProperty("studentQueue", Collections.emptyList());
        classEntity.setProperty("taList", Collections.emptyList());

        datastore.put(classEntity);

        response.sendRedirect(DASHBOARD + KeyFactory.keyToString(classEntity.getKey()));

      } else {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
      }

    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}

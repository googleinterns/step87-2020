package com.google.sps.servlets.user;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.gson.Gson;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/get-role")
public class GetUserRole extends HttpServlet {
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

    try {
      // Retrieve class entity
      String classCode = request.getParameter("classCode").trim();
      Key classKey = KeyFactory.stringToKey(classCode);

      // Find user entity
      String idToken = request.getParameter("idToken");
      FirebaseToken decodedToken = authInstance.verifyIdToken(idToken);
      String userID = decodedToken.getUid();
      String userEmail = decodedToken.getEmail();

      PreparedQuery queryUser =
          datastore.prepare(
              new Query("User")
                  .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, userEmail)));

      Entity userEntity = queryUser.asSingleEntity();

      List<Key> registeredClassesList = (List<Key>) userEntity.getProperty("registeredClasses");
      List<Key> ownedClassesList = (List<Key>) userEntity.getProperty("ownedClasses");
      List<Key> taClassesList = (List<Key>) userEntity.getProperty("taClasses");

      if (ownedClassesList.contains(classKey)) {
        response.setContentType("application/json;");

        Gson gson = new Gson();
        response.getWriter().print(gson.toJson("owner"));

      } else if (taClassesList.contains(classKey)) {
        response.setContentType("application/json;");

        Gson gson = new Gson();
        response.getWriter().print(gson.toJson("TA"));

      } else {
        response.setContentType("application/json;");

        Gson gson = new Gson();
        response.getWriter().print(gson.toJson("student"));
      }

    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    } catch (FirebaseAuthException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}

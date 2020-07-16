package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

@WebServlet("/get-user")
public class GetUserData extends HttpServlet {
  private FirebaseAuth authInstance;
  private DatastoreService datastore;
  private Gson gson;

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
      // Find user ID
      String idToken = request.getParameter("idToken");
      FirebaseToken decodedToken = authInstance.verifyIdToken(idToken);
      String userID = decodedToken.getUid();
      UserRecord userRecord = authInstance.getUser(userID);
      String userEmail = userRecord.getEmail();

      PreparedQuery queryUser =
          datastore.prepare(
              new Query("User")
                  .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, userEmail)));

      Entity userEntity;
      // If the student user entity doesnt exist yet, create one
      if (queryUser.countEntities() == 0) {
        userEntity = new Entity("User");
        userEntity.setProperty("userEmail", userEmail);
        userEntity.setProperty("registeredClasses", Collections.emptyList());
        userEntity.setProperty("ownedClasses", Collections.emptyList());
        userEntity.setProperty("taClasses", Collections.emptyList());

        datastore.put(userEntity);
      } else {
        userEntity = queryUser.asSingleEntity();
      }

      List<Key> classList = new ArrayList<Key>();
      if (request.getParameter("registeredClasses") != null) {
        classList = (List<Key>) userEntity.getProperty("registeredClasses");

      } else if (request.getParameter("ownedClasses") != null) {
        classList = (List<Key>) userEntity.getProperty("ownedClasses");

      } else if (request.getParameter("taClasses") != null) {
        classList = (List<Key>) userEntity.getProperty("taClasses");
      }

      Map<String, String> classMap = new HashMap<>();
      for (Key classKey : classList) {
        String classCode = KeyFactory.keyToString(classKey);
        String className = (String) datastore.get(classKey).getProperty("name");
        System.out.println(classCode);

        classMap.put(classCode, className);
      }

      if (classMap.isEmpty()) {
        response.setContentType("application/json;");
        response.getWriter().print("null");

      } else {
        response.setContentType("application/json;");
        response.getWriter().print(new JSONObject(classMap).toString());
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

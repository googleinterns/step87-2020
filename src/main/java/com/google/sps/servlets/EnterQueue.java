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
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.sps.firebase.FirebaseAppManager;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/enterqueue")
public final class EnterQueue extends HttpServlet {
  private FirebaseAuth authInstance;
  private DatastoreService datastore;
  private Clock clock;
  private static final String TA_QUEUE = "/queue/ta.html?classCode=";
  private static final String STUDENT_QUEUE = "/queue/student.html?classCode=";

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
      clock = Clock.systemUTC();
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the input from the form.

    datastore = DatastoreServiceFactory.getDatastoreService();
    System.setProperty(
        DatastoreServiceConfig.DATASTORE_EMPTY_LIST_SUPPORT, Boolean.TRUE.toString());

    try {
      String classCode = request.getParameter("classCode").trim();

      String idToken = request.getParameter("idToken");
      FirebaseToken decodedToken = authInstance.verifyIdToken(idToken);
      String userID = decodedToken.getUid();

      if (request.getParameter("enterTA") == null) {
        int retries = 10;
        while (true) {
          TransactionOptions options = TransactionOptions.Builder.withXG(true);
          Transaction txn = datastore.beginTransaction(options);
          try {
            Key classKey = KeyFactory.stringToKey(classCode);
            Entity classEntity = datastore.get(txn, classKey);

            // Get date
            LocalDate localDate = LocalDate.now(clock);
            ZoneId defaultZoneId = ZoneId.systemDefault();
            Date currDate = Date.from(localDate.atStartOfDay(defaultZoneId).toInstant());

            // Query visit entity for particular day
            Filter classVisitFilter =
                new FilterPredicate("classKey", FilterOperator.EQUAL, classKey);
            Filter dateVisitFilter = new FilterPredicate("date", FilterOperator.EQUAL, currDate);
            CompositeFilter visitFilter =
                CompositeFilterOperator.and(dateVisitFilter, classVisitFilter);
            PreparedQuery query = datastore.prepare(new Query("Visit").setFilter(visitFilter));

            // Get visit entity for particular day
            Entity visitEntity;
            if (query.countEntities() == 0) {
              visitEntity = new Entity("Visit");
              visitEntity.setProperty("classKey", classKey);
              visitEntity.setProperty("date", currDate);
              visitEntity.setProperty("numVisits", (long) 0);
            } else {
              visitEntity = query.asSingleEntity();
            }

            long numVisits = (long) visitEntity.getProperty("numVisits");
            ArrayList<String> updatedQueue = (ArrayList) classEntity.getProperty("studentQueue");

            // Update studentQueue and numVisit properties
            if (!updatedQueue.contains(userID)) {
              updatedQueue.add(userID);
              numVisits++;
            }

            visitEntity.setProperty("numVisits", numVisits);
            datastore.put(txn, visitEntity);

            classEntity.setProperty("studentQueue", updatedQueue);
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
        response.sendRedirect(STUDENT_QUEUE + classCode);
      } else {
        Key classKey = KeyFactory.stringToKey(classCode);
        Entity classEntity = datastore.get(classKey);

        List<String> taList = (List<String>) classEntity.getProperty("taList");

        // Get user email
        UserRecord userRecord = authInstance.getUser(userID);
        String userEmail = userRecord.getEmail();

        if (taList.contains(userEmail)) {
          response.sendRedirect(TA_QUEUE + classCode);
        } else {
          response.sendError(HttpServletResponse.SC_FORBIDDEN);
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

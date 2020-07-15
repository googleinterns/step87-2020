package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
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
import com.google.sps.workspace.WorkspaceFactory;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
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
  private Clock clock;

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      authInstance = FirebaseAuth.getInstance(FirebaseAppManager.getApp());
      factory = WorkspaceFactory.getInstance();
      clock = Clock.systemUTC();
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

      // Get studentID from studentEmail
      String studentEmail = request.getParameter("studentEmail");
      UserRecord userRecord = authInstance.getUserByEmail(studentEmail);
      String studentID = userRecord.getUid();

      // Get date
      LocalDate localDate = LocalDate.now(clock);
      ZoneId defaultZoneId = ZoneId.systemDefault();
      Date currDate = Date.from(localDate.atStartOfDay(defaultZoneId).toInstant());

      int retries = 10;
      while (true) {
        TransactionOptions options = TransactionOptions.Builder.withXG(true);
        Transaction txn = datastore.beginTransaction(options);
        try {
          // Retrive class entity
          Key classKey = KeyFactory.stringToKey(classCode);
          Entity classEntity = datastore.get(txn, classKey);

          // Query wait entity for particular day
          Filter classWaitFilter = new FilterPredicate("classKey", FilterOperator.EQUAL, classKey);
          Filter dateWaitFilter = new FilterPredicate("date", FilterOperator.EQUAL, currDate);
          CompositeFilter waitFilter = CompositeFilterOperator.and(dateWaitFilter, classWaitFilter);
          PreparedQuery query = datastore.prepare(new Query("Wait").setFilter(waitFilter));

          // Get wait entity for particular day
          Entity waitEntity;
          if (query.countEntities() == 0) {
            waitEntity = new Entity("Wait");
            waitEntity.setProperty("classKey", classKey);
            waitEntity.setProperty("date", currDate);
            waitEntity.setProperty("waitDurations", new ArrayList<Long>());
          } else {
            waitEntity = query.asSingleEntity();
          }

          // Get student info and queue
          ArrayList<EmbeddedEntity> queue =
              (ArrayList<EmbeddedEntity>) classEntity.getProperty("studentQueue");
          EmbeddedEntity delEntity =
              queue.stream().filter(elem -> elem.hasProperty(studentID)).findFirst().orElse(null);
          ArrayList<Long> waitTimes = (ArrayList<Long>) waitEntity.getProperty("waitDurations");

          // Update wait entity
          EmbeddedEntity studentEntity = (EmbeddedEntity) delEntity.getProperty(studentID);
          Date timeEntered = (Date) studentEntity.getProperty("timeEntered");

          LocalDateTime currTime = LocalDateTime.now(clock);
          LocalDateTime enteredTime =
              timeEntered.toInstant().atZone(defaultZoneId).toLocalDateTime();
          waitTimes.add(Duration.between(enteredTime, currTime).getSeconds());

          // Update queue
          queue.remove(delEntity);

          // Get workspace ID
          String workspaceID = (String) delEntity.getProperty("workspaceID");
          factory.fromWorkspaceID(workspaceID).setTaUID(taID);

          // Update beingHelped
          EmbeddedEntity beingHelped = (EmbeddedEntity) classEntity.getProperty("beingHelped");
          EmbeddedEntity queueInfo = new EmbeddedEntity();
          queueInfo.setProperty("taID", taID);
          queueInfo.setProperty("workspaceID", workspaceID);
          beingHelped.setProperty(studentID, queueInfo);

          // Update entities
          waitEntity.setProperty("waitDurations", waitTimes);
          datastore.put(txn, waitEntity);

          classEntity.setProperty("studentQueue", queue);
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

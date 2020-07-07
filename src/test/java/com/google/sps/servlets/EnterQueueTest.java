package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnterQueueTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(
          // Use High Rep job policy to allow cross group transactions in tests.
          new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @Mock Clock clock;

  @InjectMocks EnterQueue addFirst;

  private Clock fixedClock;
  private static final LocalDate LOCAL_DATE = LocalDate.of(2020, 07, 06);
  private static final Date DATE =
      Date.from(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());

  private Filter classVisitFilter;
  private Filter dateVisitFilter;
  private CompositeFilter visitFilter;
  private Query visitQuery;
  private Entity init;

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();
    System.setProperty(
        DatastoreServiceConfig.DATASTORE_EMPTY_LIST_SUPPORT, Boolean.TRUE.toString());

    fixedClock =
        Clock.fixed(
            LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    doReturn(fixedClock.instant()).when(clock).instant();
    doReturn(fixedClock.getZone()).when(clock).getZone();

    init = new Entity("Class");

    classVisitFilter = new FilterPredicate("classKey", FilterOperator.EQUAL, init.getKey());
    dateVisitFilter = new FilterPredicate("date", FilterOperator.EQUAL, DATE);
    visitFilter = CompositeFilterOperator.and(dateVisitFilter, classVisitFilter);

    visitQuery = new Query("Visit").setFilter(visitFilter);
  }

  @After
  public void tearDown() {
    // Clean up any dangling transactions.
    Transaction txn = datastore.getCurrentTransaction(null);
    if (txn != null && txn.isActive()) {
      txn.rollback();
    }

    helper.tearDown();
  }

  @Test
  public void addFirstStudentToQueue() throws Exception {
    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());
    init.setProperty("taList", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn("token");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("token")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("studentID");

    addFirst.doPost(httpRequest, httpResponse);

    Entity testClassEntity = datastore.prepare(new Query("Class")).asSingleEntity();
    ArrayList<String> testQueue = (ArrayList<String>) testClassEntity.getProperty("studentQueue");
    assertEquals(1, testQueue.size());
    assertEquals("studentID", testQueue.get(0));

    Entity testVisitEntity = datastore.prepare(visitQuery).asSingleEntity();
    assertEquals(1, (long) testVisitEntity.getProperty("numVisits"));
    assertEquals(DATE, (Date) testVisitEntity.getProperty("date"));
  }

  @Test
  public void addUniqueStudentToQueue() throws Exception {
    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", Collections.emptyList());
    init.setProperty("studentQueue", Arrays.asList("test1"));
    init.setProperty("taList", Collections.emptyList());

    datastore.put(init);

    Entity visitInit = new Entity("Visit");

    visitInit.setProperty("classKey", init.getKey());
    visitInit.setProperty("date", DATE);
    visitInit.setProperty("numVisits", 1);

    datastore.put(visitInit);

    when(httpRequest.getParameter("enterTA")).thenReturn(null);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("uID");

    addFirst.doPost(httpRequest, httpResponse);

    Entity testClassEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    ArrayList<String> testQueue = (ArrayList<String>) testClassEntity.getProperty("studentQueue");
    assertEquals(
        KeyFactory.keyToString(init.getKey()), KeyFactory.keyToString(testClassEntity.getKey()));
    assertEquals(2, testQueue.size());
    assertEquals("uID", testQueue.get(1));

    Entity testVisitEntity = datastore.prepare(visitQuery).asSingleEntity();
    assertEquals(2, (long) testVisitEntity.getProperty("numVisits"));
    assertEquals(DATE, (Date) testVisitEntity.getProperty("date"));
  }

  @Test
  public void addDuplicateStudentToQueue() throws Exception {
    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", Collections.emptyList());
    init.setProperty("studentQueue", Arrays.asList("uID"));
    init.setProperty("taList", Collections.emptyList());

    datastore.put(init);

    Entity visitInit = new Entity("Visit");

    visitInit.setProperty("classKey", init.getKey());
    visitInit.setProperty("date", DATE);
    visitInit.setProperty("numVisits", 1);

    datastore.put(visitInit);

    when(httpRequest.getParameter("enterTA")).thenReturn(null);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("uID");

    addFirst.doPost(httpRequest, httpResponse);

    Entity testClassEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    ArrayList<String> testQueue = (ArrayList<String>) testClassEntity.getProperty("studentQueue");
    assertEquals(
        KeyFactory.keyToString(init.getKey()), KeyFactory.keyToString(testClassEntity.getKey()));
    assertEquals(1, testQueue.size());
    assertEquals("uID", testQueue.get(0));

    Filter classVisitFilter = new FilterPredicate("classKey", FilterOperator.EQUAL, init.getKey());

    Filter dateVisitFilter = new FilterPredicate("date", FilterOperator.EQUAL, DATE);

    CompositeFilter visitFilter = CompositeFilterOperator.and(dateVisitFilter, classVisitFilter);

    Entity testVisitEntity = datastore.prepare(visitQuery).asSingleEntity();
    assertEquals(1, (long) testVisitEntity.getProperty("numVisits"));
    assertEquals(DATE, (Date) testVisitEntity.getProperty("date"));
  }

  @Test
  public void redirectVerifiedTA() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", Collections.emptyList());
    init.setProperty("studentQueue", Collections.emptyList());
    init.setProperty("taList", Arrays.asList("uID"));

    datastore.put(init);

    when(httpRequest.getParameter("enterTA")).thenReturn("isTA");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("uID");

    addFirst.doPost(httpRequest, httpResponse);

    verify(httpResponse)
        .sendRedirect("/queue/ta.html?classCode=" + KeyFactory.keyToString(init.getKey()));
  }

  @Test
  public void redirectUnverifiedTA() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", Collections.emptyList());
    init.setProperty("studentQueue", Collections.emptyList());
    init.setProperty("taList", Arrays.asList("taID"));

    datastore.put(init);

    when(httpRequest.getParameter("enterTA")).thenReturn("isTA");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("uID");

    addFirst.doPost(httpRequest, httpResponse);

    verify(httpResponse).sendError(403);
  }
}

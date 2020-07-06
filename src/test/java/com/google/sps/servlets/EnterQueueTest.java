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
    Entity init = new Entity("Class");

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

    Filter classVisitFilter =
        new FilterPredicate(
            "classCode", FilterOperator.EQUAL, KeyFactory.keyToString(init.getKey()));

    Filter dateVisitFilter = new FilterPredicate("date", FilterOperator.EQUAL, "07/06/2020");

    CompositeFilter visitFilter = CompositeFilterOperator.and(dateVisitFilter, classVisitFilter);

    Entity testVisitEntity =
        datastore.prepare(new Query("Visit").setFilter(visitFilter)).asSingleEntity();
    assertEquals(1, (long) testVisitEntity.getProperty("numVisits"));
    assertEquals("07/06/2020", (String) testVisitEntity.getProperty("date"));
  }

  @Test
  public void addUniqueStudentToQueue() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", Collections.emptyList());
    init.setProperty("studentQueue", new ArrayList(Arrays.asList("test1")));

    datastore.put(init);

    Entity visitInit = new Entity("Visit");

    visitInit.setProperty("classCode", KeyFactory.keyToString(init.getKey()));
    visitInit.setProperty("date", "07/06/2020");
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

    Filter classVisitFilter =
        new FilterPredicate(
            "classCode", FilterOperator.EQUAL, KeyFactory.keyToString(init.getKey()));

    Filter dateVisitFilter = new FilterPredicate("date", FilterOperator.EQUAL, "07/06/2020");

    CompositeFilter visitFilter = CompositeFilterOperator.and(dateVisitFilter, classVisitFilter);

    Entity testVisitEntity =
        datastore.prepare(new Query("Visit").setFilter(visitFilter)).asSingleEntity();
    assertEquals(2, (long) testVisitEntity.getProperty("numVisits"));
    assertEquals("07/06/2020", (String) testVisitEntity.getProperty("date"));
  }

  @Test
  public void addDuplicateStudentToQueue() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", Collections.emptyList());
    init.setProperty("studentQueue", new ArrayList(Arrays.asList("uID")));
    init.setProperty("visitKey", "visitKey");

    datastore.put(init);

    Entity visitInit = new Entity("Visit");

    visitInit.setProperty("classCode", KeyFactory.keyToString(init.getKey()));
    visitInit.setProperty("date", "07/06/2020");
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

    Filter classVisitFilter =
        new FilterPredicate(
            "classCode", FilterOperator.EQUAL, KeyFactory.keyToString(init.getKey()));

    Filter dateVisitFilter = new FilterPredicate("date", FilterOperator.EQUAL, "07/06/2020");

    CompositeFilter visitFilter = CompositeFilterOperator.and(dateVisitFilter, classVisitFilter);

    Entity testVisitEntity =
        datastore.prepare(new Query("Visit").setFilter(visitFilter)).asSingleEntity();
    assertEquals(1, (long) testVisitEntity.getProperty("numVisits"));
    assertEquals("07/06/2020", (String) testVisitEntity.getProperty("date"));
  }

  @Test
  public void redirectTA() throws Exception {
    when(httpRequest.getParameter("enterTA")).thenReturn("isTA");
    when(httpRequest.getParameter("classCode")).thenReturn("code");
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("uID");

    addFirst.doPost(httpRequest, httpResponse);

    verify(httpResponse).sendRedirect("/queue/ta.html?classCode=code");
  }
}

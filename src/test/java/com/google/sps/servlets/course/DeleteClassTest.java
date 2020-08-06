package com.google.sps.servlets.course;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.sps.authentication.Authenticator;
import com.google.sps.tasks.TaskScheduler;
import com.google.sps.tasks.TaskSchedulerFactory;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
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
public class DeleteClassTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(
          // Use High Rep job policy to allow cross group transactions in tests.
          new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;
  @Mock HttpServletResponse httpResponse;
  @Mock FirebaseAuth authInstance;
  @Mock Authenticator auth;
  @Mock Clock clock;
  @Mock WorkspaceFactory workspaceFactory;
  @Mock Workspace workspace;
  @Mock TaskSchedulerFactory taskSchedulerFactory;
  @Mock TaskScheduler scheduler;

  @InjectMocks DeleteClass deleteClass;

  private static final LocalDate LOCAL_DATE = LocalDate.of(2020, 07, 06);
  private static final Date DATE =
      Date.from(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());

  private static final LocalDate LOCAL_DATE2 = LocalDate.of(2020, 07, 03);
  private static final Date DATE2 =
      Date.from(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();
    System.setProperty(
        DatastoreServiceConfig.DATASTORE_EMPTY_LIST_SUPPORT, Boolean.TRUE.toString());
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void delClass() throws Exception {
    Entity init = new Entity("Class");
    init.setProperty("name", "testClass");

    EmbeddedEntity queueInfo = new EmbeddedEntity();
    queueInfo.setProperty("taID", "taID");
    queueInfo.setProperty("workspaceID", "WORKSPACE_ID");

    EmbeddedEntity queueInfo2 = new EmbeddedEntity();
    queueInfo2.setProperty("taID", "ta3ID");
    queueInfo2.setProperty("workspaceID", "WORKSPACE_ID");

    EmbeddedEntity beingHelped = new EmbeddedEntity();
    beingHelped.setProperty("test1", queueInfo);
    beingHelped.setProperty("test3", queueInfo2);

    init.setProperty("beingHelped", beingHelped);

    EmbeddedEntity addQueue = new EmbeddedEntity();
    addQueue.setProperty("timeEntered", DATE);
    addQueue.setProperty("workspaceID", "WORKSPACE_ID");
    addQueue.setProperty("uID", "studentID");
    init.setProperty("studentQueue", Arrays.asList(addQueue));

    datastore.put(init);

    Entity visitInit = new Entity("Visit");
    visitInit.setProperty("classKey", init.getKey());
    visitInit.setProperty("date", DATE);
    visitInit.setProperty("numVisits", 1);
    datastore.put(visitInit);

    Entity visitInit2 = new Entity("Visit");
    visitInit2.setProperty("classKey", init.getKey());
    visitInit2.setProperty("date", DATE2);
    visitInit2.setProperty("numVisits", 4);
    datastore.put(visitInit2);

    Entity initUser = new Entity("User");
    initUser.setProperty("userEmail", "user@google.com");
    initUser.setProperty("registeredClasses", Arrays.asList(init.getKey(), "dummyKey1"));
    initUser.setProperty("ownedClasses", Collections.emptyList());
    initUser.setProperty("taClasses", Collections.emptyList());
    datastore.put(initUser);

    Entity initUser2 = new Entity("User");
    initUser2.setProperty("userEmail", "user2@google.com");
    initUser2.setProperty("registeredClasses", Arrays.asList("dummyKey1", "dummyKey2"));
    initUser2.setProperty("ownedClasses", Collections.emptyList());
    initUser2.setProperty("taClasses", Arrays.asList(init.getKey()));
    datastore.put(initUser2);

    Entity initUser3 = new Entity("User");
    initUser3.setProperty("userEmail", "user3@google.com");
    initUser3.setProperty("registeredClasses", Collections.emptyList());
    initUser3.setProperty("ownedClasses", Collections.emptyList());
    initUser3.setProperty("taClasses", Collections.emptyList());
    datastore.put(initUser3);

    Entity waitInit = new Entity("Wait");
    waitInit.setProperty("classKey", init.getKey());
    waitInit.setProperty("date", DATE);
    waitInit.setProperty("numVisits", Collections.emptyList());
    datastore.put(waitInit);

    Entity waitInit2 = new Entity("Wait");
    waitInit2.setProperty("classKey", init.getKey());
    waitInit2.setProperty("date", DATE2);
    waitInit2.setProperty("waitDurations", Collections.emptyList());
    datastore.put(waitInit2);

    String QUEUE_NAME = "QUEUE_NAME";

    Entity environInit = new Entity("Environment");
    environInit.setProperty("class", init.getKey());
    environInit.setProperty("status", "status");
    environInit.setProperty("name", "name1");
    datastore.put(environInit);

    Entity environInit2 = new Entity("Environment");
    environInit2.setProperty("class", init.getKey());
    environInit.setProperty("status", "status");
    environInit.setProperty("name", "name2");
    datastore.put(environInit2);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    when(taskSchedulerFactory.create(anyString(), anyString())).thenReturn(scheduler);
    deleteClass.QUEUE_NAME = QUEUE_NAME;
    when(workspaceFactory.fromWorkspaceID("WORKSPACE_ID")).thenReturn(workspace);
    when(auth.verifyOwner("testID", init.getKey())).thenReturn(true);

    assertEquals(2, datastore.prepare(new Query("Visit")).countEntities());
    assertEquals(2, datastore.prepare(new Query("Wait")).countEntities());
    assertEquals(3, datastore.prepare(new Query("User")).countEntities());
    assertEquals(2, datastore.prepare(new Query("Environment")).countEntities());

    Filter registeredClassesFilter =
        new FilterPredicate("registeredClasses", FilterOperator.EQUAL, init.getKey());
    Filter ownedClassesFilter =
        new FilterPredicate("ownedClasses", FilterOperator.EQUAL, init.getKey());
    Filter taClassesFilter = new FilterPredicate("taClasses", FilterOperator.EQUAL, init.getKey());

    assertEquals(
        1, datastore.prepare(new Query("User").setFilter(registeredClassesFilter)).countEntities());
    assertEquals(
        0, datastore.prepare(new Query("User").setFilter(ownedClassesFilter)).countEntities());
    assertEquals(
        1, datastore.prepare(new Query("User").setFilter(taClassesFilter)).countEntities());

    deleteClass.doPost(httpRequest, httpResponse);

    assertEquals(0, datastore.prepare(new Query("Class")).countEntities());
    assertEquals(0, datastore.prepare(new Query("Visit")).countEntities());
    assertEquals(0, datastore.prepare(new Query("Wait")).countEntities());
    assertEquals(
        0, datastore.prepare(new Query("User").setFilter(registeredClassesFilter)).countEntities());
    assertEquals(
        0, datastore.prepare(new Query("User").setFilter(ownedClassesFilter)).countEntities());
    assertEquals(
        0, datastore.prepare(new Query("User").setFilter(taClassesFilter)).countEntities());

    verify(taskSchedulerFactory, times(2)).create(eq(QUEUE_NAME), eq("/tasks/deleteEnv"));
    verify(scheduler, times(1)).schedule(eq(KeyFactory.keyToString(environInit.getKey())));
    verify(scheduler, times(1)).schedule(eq(KeyFactory.keyToString(environInit2.getKey())));
  }

  @Test
  public void forbiddenDelete() throws Exception {
    Entity init = new Entity("Class");
    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    when(auth.verifyOwner("testID", init.getKey())).thenReturn(false);

    deleteClass.doPost(httpRequest, httpResponse);

    verify(httpResponse, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

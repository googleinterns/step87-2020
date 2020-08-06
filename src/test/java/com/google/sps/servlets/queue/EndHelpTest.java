package com.google.sps.servlets.queue;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.sps.authentication.Authenticator;
import com.google.sps.tasks.TaskScheduler;
import com.google.sps.tasks.TaskSchedulerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
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
public class EndHelpTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(
          // Use High Rep job policy to allow cross group transactions in tests.
          new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

  private DatastoreService datastore;
  private String QUEUE_NAME = "QUEUE_NAME";

  private Entity init;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;
  @Mock TaskSchedulerFactory taskSchedulerFactory;
  @Mock TaskScheduler scheduler;
  @Mock Authenticator auth;

  @InjectMocks EndHelp finishStudent;

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();

    init = new Entity("Class");
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
  public void doneHelping() throws Exception {
    ArrayList<String> setQueue = new ArrayList<String>(Arrays.asList("uID1", "uID2"));

    init.setProperty("name", "testClass");
    init.setProperty("studentQueue", setQueue);

    EmbeddedEntity queueInfo = new EmbeddedEntity();
    queueInfo.setProperty("taID", "taID");
    queueInfo.setProperty("workspaceID", "workspaceID");

    EmbeddedEntity queueInfo2 = new EmbeddedEntity();
    queueInfo2.setProperty("taID", "ta3ID");
    queueInfo2.setProperty("workspaceID", "workspaceID");

    EmbeddedEntity beingHelped = new EmbeddedEntity();
    beingHelped.setProperty("test1", queueInfo);
    beingHelped.setProperty("test3", queueInfo2);

    init.setProperty("beingHelped", beingHelped);

    datastore.put(init);

    when(httpRequest.getParameter("taToken")).thenReturn("testID");
    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(auth.verifyTaOrOwner("testID", KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    when(httpRequest.getParameter("studentEmail")).thenReturn("test@google.com");

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    UserRecord mockUser = mock(UserRecord.class);
    when(authInstance.getUserByEmail("test@google.com")).thenReturn(mockUser);
    when(mockUser.getUid()).thenReturn("test1");

    when(taskSchedulerFactory.create(anyString(), anyString())).thenReturn(scheduler);
    finishStudent.QUEUE_NAME = QUEUE_NAME;

    finishStudent.doPost(httpRequest, httpResponse);

    Entity testClassEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    ArrayList<String> testQueue = (ArrayList<String>) testClassEntity.getProperty("studentQueue");
    assertEquals(
        KeyFactory.keyToString(init.getKey()), KeyFactory.keyToString(testClassEntity.getKey()));
    assertEquals(2, testQueue.size());

    EmbeddedEntity got = (EmbeddedEntity) testClassEntity.getProperty("beingHelped");
    assertThat((EmbeddedEntity) got.getProperty("test1")).isNull();
    assertThat((EmbeddedEntity) got.getProperty("test3")).isNotNull();

    verify(taskSchedulerFactory, times(1)).create(eq(QUEUE_NAME), eq("/tasks/deleteWorkspace"));
    verify(scheduler, times(1)).schedule(eq("workspaceID"), eq(TimeUnit.HOURS.toSeconds(1)));
  }

  @Test
  public void notTA() throws Exception {
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("taToken")).thenReturn("testID");

    when(auth.verifyTaOrOwner("testID", KeyFactory.keyToString(init.getKey()))).thenReturn(false);

    finishStudent.doPost(httpRequest, httpResponse);

    verify(httpResponse, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

package com.google.sps.servlets.queue;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
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
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceFactory;
import java.time.Clock;
import java.time.Duration;
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
public class NotifyStudentTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(
          // Use High Rep job policy to allow cross group transactions in tests.
          new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;
  @Mock HttpServletResponse httpResponse;
  @Mock FirebaseAuth authInstance;
  @Mock WorkspaceFactory factory;
  @Mock Workspace workspace;
  @Mock Clock clock;
  @Mock Authenticator auth;

  @InjectMocks NotifyStudent alertStudent;

  private Clock fixedClock;
  private static final LocalDate LOCAL_DATE = LocalDate.of(2020, 07, 07);
  private static final Date DATE =
      Date.from(LocalDate.of(2020, 07, 06).atStartOfDay(ZoneId.systemDefault()).toInstant());

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();

    fixedClock =
        Clock.fixed(
            LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
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
  public void takeOff() throws Exception {
    String WORKSPACE_ID = "WORKSPACE_ID";

    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");

    EmbeddedEntity addQueue1 = new EmbeddedEntity();
    addQueue1.setProperty("timeEntered", DATE);
    addQueue1.setProperty("workspaceID", WORKSPACE_ID);
    addQueue1.setProperty("uID", "studentID");

    EmbeddedEntity addQueue2 = new EmbeddedEntity();
    addQueue2.setProperty("timeEntered", DATE);
    addQueue2.setProperty("workspaceID", WORKSPACE_ID);
    addQueue2.setProperty("uID", "uID2");

    init.setProperty("studentQueue", Arrays.asList(addQueue1, addQueue2));

    EmbeddedEntity beingHelped = new EmbeddedEntity();
    init.setProperty("beingHelped", beingHelped);

    datastore.put(init);

    when(httpRequest.getParameter("taToken")).thenReturn("testID");
    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("taID");

    when(httpRequest.getParameter("studentEmail")).thenReturn("test@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(auth.verifyTaOrOwner("testID", KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    UserRecord mockUser = mock(UserRecord.class);
    when(authInstance.getUserByEmail("test@google.com")).thenReturn(mockUser);
    when(mockUser.getUid()).thenReturn("studentID");
    doReturn(fixedClock.instant()).when(clock).instant();

    when(factory.fromWorkspaceID(WORKSPACE_ID)).thenReturn(workspace);

    alertStudent.doPost(httpRequest, httpResponse);

    Entity testClassEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    EmbeddedEntity got = (EmbeddedEntity) testClassEntity.getProperty("beingHelped");
    EmbeddedEntity gotQueue = (EmbeddedEntity) got.getProperty("studentID");

    assertThat((String) gotQueue.getProperty("taID")).named("got.taID").isEqualTo("taID");
    assertThat((String) gotQueue.getProperty("workspaceID"))
        .named("got.workspaceID")
        .isEqualTo(WORKSPACE_ID);

    verify(workspace, times(1)).setTaUID("taID");

    ArrayList<EmbeddedEntity> testQueue =
        (ArrayList<EmbeddedEntity>) testClassEntity.getProperty("studentQueue");
    assertEquals(
        KeyFactory.keyToString(init.getKey()), KeyFactory.keyToString(testClassEntity.getKey()));
    assertEquals(1, testQueue.size());
    assertTrue(((String) testQueue.get(0).getProperty("uID")).equals("uID2"));
    Entity testWaitEntity = datastore.prepare(new Query("Wait")).asSingleEntity();
    ArrayList<Long> waitDurations = (ArrayList<Long>) testWaitEntity.getProperty("waitDurations");
    assertEquals((long) Duration.ofHours(24).getSeconds(), (long) waitDurations.get(0));
  }

  @Test
  public void isStudent() throws Exception {
    Entity init = new Entity("Class");
    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("taToken")).thenReturn("testID");
    when(auth.verifyTaOrOwner("testID", KeyFactory.keyToString(init.getKey()))).thenReturn(false);

    alertStudent.doPost(httpRequest, httpResponse);

    verify(httpResponse, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

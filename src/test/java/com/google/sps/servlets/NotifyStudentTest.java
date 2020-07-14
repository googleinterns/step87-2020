package com.google.sps.servlets;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
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
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
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

  @InjectMocks NotifyStudent alertStudent;

  private static final LocalDate LOCAL_DATE = LocalDate.of(2020, 07, 06);
  private static final Date DATE =
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
    // Clean up any dangling transactions.
    Transaction txn = datastore.getCurrentTransaction(null);
    if (txn != null && txn.isActive()) {
      txn.rollback();
    }

    helper.tearDown();
  }

  @Test
  public void takeOff() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("visitKey", "visitKey");

    EmbeddedEntity addQueue1 = new EmbeddedEntity();
    EmbeddedEntity studentInfo1 = new EmbeddedEntity();
    studentInfo1.setProperty("timeEntered", DATE);
    addQueue1.setProperty("studentID", studentInfo1);

    EmbeddedEntity addQueue2 = new EmbeddedEntity();
    EmbeddedEntity studentInfo2 = new EmbeddedEntity();
    studentInfo2.setProperty("timeEntered", DATE);
    addQueue2.setProperty("test2", studentInfo2);

    init.setProperty("studentQueue", Arrays.asList(addQueue1, addQueue2));

    EmbeddedEntity queueInfo = new EmbeddedEntity();
    queueInfo.setProperty("taID", "taID");
    queueInfo.setProperty("workspaceID", "workspaceID");

    EmbeddedEntity beingHelped = new EmbeddedEntity();
    beingHelped.setProperty("studentID", queueInfo);

    init.setProperty("beingHelped", beingHelped);

    datastore.put(init);

    when(httpRequest.getParameter("taToken")).thenReturn("testID");
    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("taID");

    when(httpRequest.getParameter("studentEmail")).thenReturn("test@google.com");

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    UserRecord mockUser = mock(UserRecord.class);
    when(authInstance.getUserByEmail("test@google.com")).thenReturn(mockUser);
    when(mockUser.getUid()).thenReturn("studentID");

    when(factory.fromStudentAndTA(KeyFactory.keyToString(init.getKey()), "studentID", "taID"))
        .thenReturn(workspace);
    when(workspace.getWorkspaceID()).thenReturn("workspaceID");

    alertStudent.doPost(httpRequest, httpResponse);

    Entity testClassEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    EmbeddedEntity got = (EmbeddedEntity) testClassEntity.getProperty("beingHelped");
    EmbeddedEntity gotQueue = (EmbeddedEntity) got.getProperty("studentID");

    assertThat((String) gotQueue.getProperty("taID")).named("got.taID").isEqualTo("taID");
    assertThat((String) gotQueue.getProperty("workspaceID"))
        .named("got.workspaceID")
        .isEqualTo("workspaceID");

    ArrayList<EmbeddedEntity> testQueue =
        (ArrayList<EmbeddedEntity>) testClassEntity.getProperty("studentQueue");
    assertEquals(
        KeyFactory.keyToString(init.getKey()), KeyFactory.keyToString(testClassEntity.getKey()));
    assertEquals(1, testQueue.size());
    assertTrue(testQueue.get(0).hasProperty("test2"));
  }
}

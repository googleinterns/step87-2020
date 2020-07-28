package com.google.sps.servlets.queue;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
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
public class RemoveFromQueueTest {

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

  @InjectMocks RemoveFromQueue removeFromQueue;

  private static final Date START_DATE =
      Date.from(LocalDate.of(2020, 07, 06).atStartOfDay(ZoneId.systemDefault()).toInstant());

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();
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
  public void takeSelfOff() throws Exception {
    String WORKSPACE_ID1 = "WORKSPACE_ID";
    String WORKSPACE_ID2 = "WORKSPACE_ID2";

    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");

    EmbeddedEntity addQueue1 = new EmbeddedEntity();
    EmbeddedEntity studentInfo1 = new EmbeddedEntity();
    studentInfo1.setProperty("timeEntered", START_DATE);
    studentInfo1.setProperty("workspaceID", WORKSPACE_ID1);
    addQueue1.setProperty("studentID", studentInfo1);

    EmbeddedEntity addQueue2 = new EmbeddedEntity();
    EmbeddedEntity studentInfo2 = new EmbeddedEntity();
    studentInfo2.setProperty("timeEntered", START_DATE);
    studentInfo2.setProperty("workspaceID", WORKSPACE_ID2);
    addQueue2.setProperty("ID2", studentInfo2);

    init.setProperty("studentQueue", Arrays.asList(addQueue1, addQueue2));

    EmbeddedEntity beingHelped = new EmbeddedEntity();
    init.setProperty("beingHelped", beingHelped);

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn("token");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("token")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("studentID");

    when(factory.fromWorkspaceID(WORKSPACE_ID1)).thenReturn(workspace);

    removeFromQueue.doPost(httpRequest, httpResponse);

    Entity testClassEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    ArrayList<EmbeddedEntity> testQueue =
        (ArrayList<EmbeddedEntity>) testClassEntity.getProperty("studentQueue");
    assertEquals(
        KeyFactory.keyToString(init.getKey()), KeyFactory.keyToString(testClassEntity.getKey()));
    assertEquals(1, testQueue.size());
  }
}

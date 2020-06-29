package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import java.util.ArrayList;
import java.util.Arrays;
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

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @InjectMocks EndHelp finishStudent;

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
  public void doneHelping() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new ArrayList(Arrays.asList("test1")));
    init.setProperty("studentQueue", new ArrayList(Arrays.asList("test2")));
    init.setProperty("visitKey", "visitKey");

    datastore.put(init);

    when(httpRequest.getParameter("uEmail")).thenReturn("test@google.com");

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    UserRecord mockUser = mock(UserRecord.class);
    when(authInstance.getUserByEmail("test@google.com")).thenReturn(mockUser);
    when(mockUser.getUid()).thenReturn("test1");

    finishStudent.doPost(httpRequest, httpResponse);

    Entity testClassEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    ArrayList<String> testQueue = (ArrayList<String>) testClassEntity.getProperty("studentQueue");
    ArrayList<String> helpQueue = (ArrayList<String>) testClassEntity.getProperty("beingHelped");
    assertEquals(
        KeyFactory.keyToString(init.getKey()), KeyFactory.keyToString(testClassEntity.getKey()));
    assertEquals(1, testQueue.size());
    assertEquals("test2", testQueue.get(0));
    assertEquals(0, helpQueue.size());
  }
}

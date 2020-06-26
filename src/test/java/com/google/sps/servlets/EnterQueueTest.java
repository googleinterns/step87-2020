package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
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
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @InjectMocks EnterQueue addFirst;

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
  public void addFirstStudentToQueue() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", "");
    init.setProperty("studentQueue", Collections.emptyList());
    init.setProperty("visitKey", "visitKey");

    datastore.put(init);

    Entity visitInit = new Entity("Visit", "visitKey");

    visitInit.setProperty("classKey", "ownerID");
    visitInit.setProperty("numVisits", 0);

    datastore.put(visitInit);

    when(httpRequest.getParameter("enterTA")).thenReturn(null);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("uID");

    addFirst.doPost(httpRequest, httpResponse);

    Entity testEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    ArrayList<String> testQueue = (ArrayList<String>) testEntity.getProperty("studentQueue");
    assertEquals(
        KeyFactory.keyToString(init.getKey()), KeyFactory.keyToString(testEntity.getKey()));
    assertEquals(1, testQueue.size());
    assertEquals("uID", testQueue.get(0));
  }

  @Test
  public void addUniqueStudentToQueue() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", "");
    init.setProperty("studentQueue", new ArrayList(Arrays.asList("test1")));
    init.setProperty("visitKey", "visitKey");

    datastore.put(init);

    Entity visitInit = new Entity("Visit", "visitKey");

    visitInit.setProperty("classKey", "ownerID");
    visitInit.setProperty("numVisits", 0);

    datastore.put(visitInit);

    when(httpRequest.getParameter("enterTA")).thenReturn(null);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("uID");

    addFirst.doPost(httpRequest, httpResponse);

    Entity testEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    ArrayList<String> testQueue = (ArrayList<String>) testEntity.getProperty("studentQueue");
    assertEquals(
        KeyFactory.keyToString(init.getKey()), KeyFactory.keyToString(testEntity.getKey()));
    assertEquals(2, testQueue.size());
    assertEquals("uID", testQueue.get(1));
  }

  @Test
  public void addDuplicateStudentToQueue() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", "");
    init.setProperty("studentQueue", new ArrayList(Arrays.asList("uID")));
    init.setProperty("visitKey", "visitKey");

    datastore.put(init);

    Entity visitInit = new Entity("Visit", "visitKey");

    visitInit.setProperty("classKey", "ownerID");
    visitInit.setProperty("numVisits", 0);

    datastore.put(visitInit);

    when(httpRequest.getParameter("enterTA")).thenReturn(null);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("uID");

    addFirst.doPost(httpRequest, httpResponse);

    Entity testEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    ArrayList<String> testQueue = (ArrayList<String>) testEntity.getProperty("studentQueue");
    assertEquals(
        KeyFactory.keyToString(init.getKey()), KeyFactory.keyToString(testEntity.getKey()));
    assertEquals(1, testQueue.size());
    assertEquals("uID", testQueue.get(0));
  }

  @Test
  public void redirectTA() throws Exception {
    when(httpRequest.getParameter("enterTA")).thenReturn("isTA");
    when(httpRequest.getParameter("classCode")).thenReturn("code");

    addFirst.doPost(httpRequest, httpResponse);

    verify(httpResponse).sendRedirect("/queue/ta.html?classCode=code");
  }
}

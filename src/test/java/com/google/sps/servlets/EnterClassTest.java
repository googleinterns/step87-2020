package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public class EnterClassTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @InjectMocks NewClass addNew;

  @InjectMocks EnterQueue addFirst;

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void addNewClass() throws Exception {
    when(httpRequest.getParameter("className")).thenReturn("testClass");
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("ownerID");

    addNew.doPost(httpRequest, httpResponse);

    Entity testEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    assertEquals(testEntity.getProperty("owner"), "ownerID");
    assertEquals(testEntity.getProperty("name"), "testClass");
    assertEquals(testEntity.getProperty("beingHelped"), "");

    ArrayList<String> testQueue = (ArrayList<String>) testEntity.getProperty("studentQueue");
    assertTrue(testQueue.isEmpty());
  }

  @Test
  public void addFirstToQueue() throws Exception {
    Entity init = new Entity("Class");
    List<String> emptyQueue = Collections.emptyList();

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", "");
    init.setProperty("studentQueue", emptyQueue);
    init.setProperty("visitKey", "visitKey");

    datastore.put(init);

    Entity visitInit = new Entity("Visit", "visitKey");

    visitInit.setProperty("classKey", "ownerID");
    visitInit.setProperty("numVisits", 0);

    datastore.put(visitInit);

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
}

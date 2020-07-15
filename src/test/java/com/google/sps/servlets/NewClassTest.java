package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import java.util.ArrayList;
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
public class NewClassTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @InjectMocks NewClass addNew;

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
  public void addUniqueClass() throws Exception {

    when(httpRequest.getParameter("className")).thenReturn("testClass");
    when(httpRequest.getParameter("idToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("ownerID");

    // Get owner info
    UserRecord mockUser = mock(UserRecord.class);
    when(authInstance.getUser("ownerID")).thenReturn(mockUser);
    when(mockUser.getEmail()).thenReturn("ownerEmail@google.com");

    addNew.doPost(httpRequest, httpResponse);

    Query query = new Query("User");
    PreparedQuery results = datastore.prepare(query);

    // Search for owner user entity in datastore
    for (Entity user : results.asIterable()) {
      if (user.getProperty("userEmail") == "ownerEmail@google.com") {
        ArrayList<Key> testOwned = (ArrayList<Key>) user.getProperty("ownedClasses");
        assertTrue(!testOwned.isEmpty());
        assertTrue(testOwned.size() == 1);
      }
    }

    Entity testClassEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    assertEquals(testClassEntity.getProperty("owner"), "ownerID");
    assertEquals(testClassEntity.getProperty("name"), "testClass");
    assertEquals(testClassEntity.getProperty("beingHelped"), new EmbeddedEntity());

    ArrayList<String> testQueue = (ArrayList<String>) testClassEntity.getProperty("studentQueue");
    assertTrue(testQueue.isEmpty());

    ArrayList<String> taList = (ArrayList<String>) testClassEntity.getProperty("taList");
    assertTrue(taList.isEmpty());
  }

  @Test
  public void addDuplicateClass() throws Exception {

    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());
    init.setProperty("taList", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("className")).thenReturn("testClass");

    addNew.doPost(httpRequest, httpResponse);

    verify(httpResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

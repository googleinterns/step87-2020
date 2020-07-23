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
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import java.util.ArrayList;
import java.util.Arrays;
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

    // Look for the owner in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(
                    new FilterPredicate(
                        "userEmail", FilterOperator.EQUAL, "ownerEmail@google.com")));

    Entity userOwner = queryUser.asSingleEntity();

    ArrayList<Key> testOwned = (ArrayList<Key>) userOwner.getProperty("ownedClasses");
    assertTrue(testOwned.size() == 1);

    Entity testClassEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    assertEquals(testClassEntity.getProperty("owner"), "ownerID");
    assertEquals(testClassEntity.getProperty("name"), "testClass");
    assertEquals(testClassEntity.getProperty("beingHelped"), new EmbeddedEntity());

    ArrayList<String> testQueue = (ArrayList<String>) testClassEntity.getProperty("studentQueue");
    assertTrue(testQueue.isEmpty());
  }

  @Test
  // User is already a TA, student, and owner; add another owned class
  public void existingOwner() throws Exception {

    Entity init1 = new Entity("Class");

    init1.setProperty("owner", "ownerID1");
    init1.setProperty("name", "testClass1");
    init1.setProperty("beingHelped", new EmbeddedEntity());
    init1.setProperty("studentQueue", Collections.emptyList());

    Entity init2 = new Entity("Class");

    init2.setProperty("owner", "ownerID2");
    init2.setProperty("name", "testClass2");
    init2.setProperty("beingHelped", new EmbeddedEntity());
    init2.setProperty("studentQueue", Collections.emptyList());

    Entity init3 = new Entity("Class");

    init3.setProperty("owner", "ownerID");
    init3.setProperty("name", "testClass3");
    init3.setProperty("beingHelped", new EmbeddedEntity());
    init3.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init1);
    datastore.put(init2);
    datastore.put(init3);

    // Example owner
    Entity owner = new Entity("User");

    List<Key> reg1 = Arrays.asList(init1.getKey());
    List<Key> ta1 = Arrays.asList(init2.getKey());
    List<Key> own1 = Arrays.asList(init3.getKey());

    owner.setProperty("userEmail", "ownerEmail@google.com");
    owner.setProperty("registeredClasses", reg1);
    owner.setProperty("taClasses", ta1);
    owner.setProperty("ownedClasses", own1);

    datastore.put(owner);

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

    // Look for the owner in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(
                    new FilterPredicate(
                        "userEmail", FilterOperator.EQUAL, "ownerEmail@google.com")));

    Entity userOwner = queryUser.asSingleEntity();

    List<Key> testOwned = (List<Key>) userOwner.getProperty("ownedClasses");
    List<Key> testRegistered = (List<Key>) userOwner.getProperty("registeredClasses");
    List<Key> testTA = (List<Key>) userOwner.getProperty("taClasses");

    assertTrue(testOwned.contains(init3.getKey()));
    assertEquals(2, testOwned.size());
    assertTrue(testRegistered.contains(init1.getKey()));
    assertEquals(1, testRegistered.size());
    assertTrue(testTA.contains(init2.getKey()));
    assertEquals(1, testTA.size());

    Query query2 = new Query("Class");
    PreparedQuery results2 = datastore.prepare(query2);

    // Verify class was created properly
    for (Entity testClassEntity : results2.asIterable()) {
      if (testClassEntity.getProperty("className") == "testClass") {
        assertEquals(testClassEntity.getProperty("owner"), "ownerID");
        assertEquals(testClassEntity.getProperty("name"), "testClass");
        assertEquals(testClassEntity.getProperty("beingHelped"), new EmbeddedEntity());

        ArrayList<String> testQueue =
            (ArrayList<String>) testClassEntity.getProperty("studentQueue");
        assertTrue(testQueue.isEmpty());
      }
    }
  }

  @Test
  public void addDuplicateClass() throws Exception {

    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("className")).thenReturn("testClass");

    addNew.doPost(httpRequest, httpResponse);

    verify(httpResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

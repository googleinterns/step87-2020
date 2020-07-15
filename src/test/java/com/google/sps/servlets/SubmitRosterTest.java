package com.google.sps.servlets;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
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
public class SubmitRosterTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @InjectMocks SubmitRoster submitRoster;

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
  public void addRosterNewStudents() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());
    init.setProperty("taList", Collections.emptyList());

    datastore.put(init);

    // Submit a roster of 2 students
    when(httpRequest.getParameter("roster")).thenReturn("first@google.com, second@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    submitRoster.doPost(httpRequest, httpResponse);

    Query query = new Query("User");
    PreparedQuery results = datastore.prepare(query);

    for (Entity user : results.asIterable()) {
      if (user.getProperty("userEmail") == "first@google.com") {
        List<Key> testRegistered = (List<Key>) user.getProperty("registeredClasses");
        assertTrue(testRegistered.contains(init.getKey()));
        assertTrue(testRegistered.size() == 1);
      } else if (user.getProperty("userEmail") == "second@google.com") {
        List<Key> testRegistered = (List<Key>) user.getProperty("registeredClasses");
        assertTrue(testRegistered.contains(init.getKey()));
        assertTrue(testRegistered.size() == 1);
      }
    }
  }

  @Test
  // For existing users, just update registration list
  public void existingStudents() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());
    init.setProperty("taList", Collections.emptyList());

    datastore.put(init);

    // Example student 1
    Entity user1 = new Entity("User");

    user1.setProperty("userEmail", "test1@google.com");
    user1.setProperty("registeredClasses", Collections.emptyList());
    user1.setProperty("taClasses", Collections.emptyList());
    user1.setProperty("ownedClasses", Collections.emptyList());

    // Example student 2
    Entity user2 = new Entity("User");

    user2.setProperty("userEmail", "test2@google.com");
    user2.setProperty("registeredClasses", Collections.emptyList());
    user2.setProperty("taClasses", Collections.emptyList());
    user2.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user1);
    datastore.put(user2);

    // Submit a roster of 2 students
    when(httpRequest.getParameter("roster")).thenReturn("test1@google.com, test2@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    submitRoster.doPost(httpRequest, httpResponse);

    Query query = new Query("User");
    PreparedQuery results = datastore.prepare(query);

    // Verify size and contents of registeredClass list for each student
    for (Entity user : results.asIterable()) {
      if (user.getProperty("userEmail") == "test1@google.com") {
        List<Key> testRegistered = (List<Key>) user.getProperty("registeredClasses");
        assertTrue(testRegistered.contains(init.getKey()));
        assertTrue(testRegistered.size() == 1);
      } else if (user.getProperty("userEmail") == "test2@google.com") {
        List<Key> testRegistered = (List<Key>) user.getProperty("registeredClasses");
        assertTrue(testRegistered.contains(init.getKey()));
        assertTrue(testRegistered.size() == 1);
      }
    }
  }
}

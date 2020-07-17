package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
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
  // Create new student users when a roster is submitted
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
      if ((user.getProperty("userEmail") == "first@google.com")
          || (user.getProperty("userEmail") == "second@google.com")) {
        List<Key> testRegistered = (List<Key>) user.getProperty("registeredClasses");
        assertTrue(testRegistered.contains(init.getKey()));
        assertTrue(testRegistered.size() == 1);
      }
    }
  }

  @Test
  // User is already registered for one class, add another
  public void existingUserAddClass() throws Exception {

    // Create 2 classes
    Entity class1 = new Entity("Class");

    class1.setProperty("owner", "ownerID");
    class1.setProperty("name", "testClass");
    class1.setProperty("beingHelped", new EmbeddedEntity());
    class1.setProperty("studentQueue", Collections.emptyList());
    class1.setProperty("taList", Collections.emptyList());

    datastore.put(class1);

    Entity class2 = new Entity("Class");

    class2.setProperty("owner", "ownerID2");
    class2.setProperty("name", "testClass2");
    class2.setProperty("beingHelped", new EmbeddedEntity());
    class2.setProperty("studentQueue", Collections.emptyList());
    class2.setProperty("taList", Collections.emptyList());

    datastore.put(class2);

    // Add one student user with one registered class
    Entity user1 = new Entity("User");

    List<Key> reg1 = Arrays.asList(class1.getKey());

    user1.setProperty("userEmail", "test1@google.com");
    user1.setProperty("registeredClasses", reg1);
    user1.setProperty("taClasses", Collections.emptyList());
    user1.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user1);

    // Submit a roster of 1 student
    when(httpRequest.getParameter("roster")).thenReturn("test1@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(class2.getKey()));

    submitRoster.doPost(httpRequest, httpResponse);

    Query query = new Query("User");
    PreparedQuery results = datastore.prepare(query);

    for (Entity user : results.asIterable()) {
      if ((user.getProperty("userEmail") == "test1@google.com")) {
        List<Key> testRegistered = (List<Key>) user.getProperty("registeredClasses");
        assertTrue(testRegistered.contains(class1.getKey()));
        assertTrue(testRegistered.contains(class2.getKey()));
        assertTrue(testRegistered.size() == 2);
      }
    }
  }

  @Test
  // If owner adds the same class to a user's registered list, verify error
  public void duplicates() throws Exception {

    // Create a class
    Entity class1 = new Entity("Class");

    class1.setProperty("owner", "ownerID");
    class1.setProperty("name", "testClass");
    class1.setProperty("beingHelped", new EmbeddedEntity());
    class1.setProperty("studentQueue", Collections.emptyList());
    class1.setProperty("taList", Collections.emptyList());

    datastore.put(class1);

    // Add one student user with one registered class
    Entity user1 = new Entity("User");

    List<Key> reg1 = Arrays.asList(class1.getKey());

    user1.setProperty("userEmail", "test1@google.com");
    user1.setProperty("registeredClasses", reg1);
    user1.setProperty("taClasses", Collections.emptyList());
    user1.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user1);

    // Attempt to add the same class to user's registered list
    when(httpRequest.getParameter("roster")).thenReturn("test1@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(class1.getKey()));

    submitRoster.doPost(httpRequest, httpResponse);

    verify(httpResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  @Test
  // For existing users, just update registration list
  public void preventDuplicateStudentRoster() throws Exception {

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

    List<Key> reg1 = Arrays.asList(init.getKey());

    user1.setProperty("userEmail", "test1@google.com");
    user1.setProperty("registeredClasses", reg1);
    user1.setProperty("taClasses", Collections.emptyList());
    user1.setProperty("ownedClasses", Collections.emptyList());

    // Example student 2
    Entity user2 = new Entity("User");

    List<Key> reg2 = Arrays.asList(init.getKey());

    user2.setProperty("userEmail", "test2@google.com");
    user2.setProperty("registeredClasses", reg2);
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
      if ((user.getProperty("userEmail") == "test1@google.com")
          || (user.getProperty("userEmail") == "test2@google.com")) {
        List<Key> testRegistered = (List<Key>) user.getProperty("registeredClasses");
        assertTrue(testRegistered.contains(init.getKey()));
        assertEquals(1, testRegistered.size());
      }
    }
  }

  @Test
  // If owner submits a roster with the same student twice, only add the student once
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
      if ((user.getProperty("userEmail") == "test1@google.com")
          || (user.getProperty("userEmail") == "test2@google.com")) {
        List<Key> testRegistered = (List<Key>) user.getProperty("registeredClasses");
        assertTrue(testRegistered.contains(init.getKey()));
        assertTrue(testRegistered.size() == 1);
      }
    }
  }
}

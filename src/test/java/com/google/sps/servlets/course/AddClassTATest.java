package com.google.sps.servlets.course;

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
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
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
public class AddClassTATest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock FirebaseAuth authInstance;

  @InjectMocks AddClassTA addTA;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

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
  // For a user that doesn't TA for any class, add a class
  public void addOneTAEmptyList() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    // Create a user
    Entity user = new Entity("User");

    user.setProperty("userEmail", "test@google.com");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    addTA.doPost(httpRequest, httpResponse);

    // Look for the TA in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(
                    new FilterPredicate("userEmail", FilterOperator.EQUAL, "test@google.com")));

    Entity userTA = queryUser.asSingleEntity();

    List<Key> taClasses = (List<Key>) userTA.getProperty("taClasses");
    assertTrue(taClasses.contains(init.getKey()));
    assertTrue(taClasses.size() == 1);
  }

  @Test
  // For a user that already TAs for one class, add another class
  public void addOneTANonEmptyList() throws Exception {

    // Create a ta and non-ta class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    Entity init2 = new Entity("Class");

    init2.setProperty("owner", "ownerID2");
    init2.setProperty("name", "testClass2");
    init2.setProperty("beingHelped", new EmbeddedEntity());
    init2.setProperty("studentQueue", Collections.emptyList());
    init2.setProperty("taList", Collections.emptyList());

    datastore.put(init);
    datastore.put(init2);

    // Initialize a user
    Entity user = new Entity("User");

    List<Key> taClassList = Arrays.asList(init.getKey());

    user.setProperty("userEmail", "test@google.com");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", taClassList);
    user.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init2.getKey()));

    addTA.doPost(httpRequest, httpResponse);

    // Look for the TA in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(
                    new FilterPredicate("userEmail", FilterOperator.EQUAL, "test@google.com")));

    Entity userTA = queryUser.asSingleEntity();

    List<Key> taClasses = (List<Key>) userTA.getProperty("taClasses");
    assertTrue(taClasses.contains(init.getKey()));
    assertTrue(taClasses.contains(init2.getKey()));
    assertTrue(taClasses.size() == 2);
  }

  @Test
  // Verify that duplicate classes don't get added
  public void preventDuplicates() throws Exception {

    // Create some classes
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    Entity init2 = new Entity("Class");

    init2.setProperty("owner", "ownerID2");
    init2.setProperty("name", "testClass2");
    init2.setProperty("beingHelped", new EmbeddedEntity());
    init2.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);
    datastore.put(init2);

    // Initialize a TA user
    Entity user = new Entity("User");

    List<Key> taClassList = Arrays.asList(init.getKey(), init2.getKey());

    user.setProperty("userEmail", "testTA@google.com");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", taClassList);
    user.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn("testTA@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    addTA.doPost(httpRequest, httpResponse);

    // Look for the TA in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(
                    new FilterPredicate("userEmail", FilterOperator.EQUAL, "testTA@google.com")));

    Entity userTA = queryUser.asSingleEntity();

    // Verify the ta class list stayed the same
    List<Key> taClasses = (List<Key>) userTA.getProperty("taClasses");
    assertTrue(taClasses.contains(init.getKey()));
    assertTrue(taClasses.contains(init2.getKey()));
    assertTrue(taClasses.size() == 2);
  }

  @Test
  // Add multiple classes for a TA user
  public void addMultipleClassKeys() throws Exception {

    // Create multiple classes
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    Entity init2 = new Entity("Class");

    init2.setProperty("owner", "ownerID2");
    init2.setProperty("name", "testClass2");
    init2.setProperty("beingHelped", new EmbeddedEntity());
    init2.setProperty("studentQueue", Collections.emptyList());
    Entity init3 = new Entity("Class");

    init3.setProperty("owner", "ownerID3");
    init3.setProperty("name", "testClass3");
    init3.setProperty("beingHelped", new EmbeddedEntity());
    init3.setProperty("studentQueue", Collections.emptyList());

    Entity init4 = new Entity("Class");

    init4.setProperty("owner", "ownerID4");
    init4.setProperty("name", "testClass4");
    init4.setProperty("beingHelped", new EmbeddedEntity());
    init4.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);
    datastore.put(init2);
    datastore.put(init3);
    datastore.put(init4);

    // Create a user
    Entity user = new Entity("User");

    List<Key> taClassList = Arrays.asList(init.getKey(), init2.getKey(), init3.getKey());

    user.setProperty("userEmail", "test@google.com");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", taClassList);
    user.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init4.getKey()));

    addTA.doPost(httpRequest, httpResponse);

    // Look for the TA in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(
                    new FilterPredicate("userEmail", FilterOperator.EQUAL, "test@google.com")));

    Entity userTA = queryUser.asSingleEntity();

    List<Key> taClasses = (List<Key>) userTA.getProperty("taClasses");

    // Verify that all the classes were stored
    assertTrue(taClasses.contains(init.getKey()));
    assertTrue(taClasses.contains(init2.getKey()));
    assertTrue(taClasses.contains(init3.getKey()));
    assertTrue(taClasses.contains(init4.getKey()));
    assertTrue(taClasses.size() == 4);
  }

  @Test
  // Throw an exception if class key isn't correct
  public void keyUnavailable() throws Exception {

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn("testClassCode");

    addTA.doPost(httpRequest, httpResponse);

    verify(httpResponse).sendError(HttpServletResponse.SC_BAD_REQUEST);
  }
}

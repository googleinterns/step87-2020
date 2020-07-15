package com.google.sps.servlets;

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

    // Create a user
    Entity user = new Entity("User");

    user.setProperty("userEmail", "test@google.com");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn("exampleClassCode");

    addTA.doPost(httpRequest, httpResponse);

    Query query = new Query("User");
    PreparedQuery results = datastore.prepare(query);

    // Add the class key to the user's TA classes list
    for (Entity entity : results.asIterable()) {
      if (entity.getProperty("userEmail") == "test@google.com") {
        List<Key> taClasses = (List<Key>) entity.getProperty("taClasses");
        assertTrue(taClasses.contains(KeyFactory.stringToKey("exampleClassCode")));
      }
    }
  }

  @Test
  // For a user that already TAs for one class, add more classes
  public void addOneTANonEmptyList() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());
    init.setProperty("taList", Collections.emptyList());

    datastore.put(init);

    // Create a user
    Entity user = new Entity("User");

    List<Key> taClassList = Arrays.asList(init.getKey());

    user.setProperty("userEmail", "test@google.com");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", taClassList);
    user.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn("exampleClassCode");

    addTA.doPost(httpRequest, httpResponse);

    Query query = new Query("User");
    PreparedQuery results = datastore.prepare(query);

    // Add the class key to the user's TA classes list
    for (Entity entity : results.asIterable()) {
      if (entity.getProperty("userEmail") == "test@google.com") {
        List<Key> taClasses = (List<Key>) entity.getProperty("taClasses");
        assertTrue(taClasses.contains(KeyFactory.stringToKey("exampleClassCode")));
      }
    }
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

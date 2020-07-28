package com.google.sps.servlets.course;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import java.io.PrintWriter;
import java.io.StringWriter;
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
public class StudentParticipantTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock FirebaseAuth authInstance;

  @InjectMocks StudentParticipants displayStudent;

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
  // Check if a single participant is retrieved properly
  public void basicCheck() throws Exception {

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
    user.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayStudent.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("test@google.com"));
  }

  @Test
  // Check if multiple participants are retrieved properly
  public void multipleTAs() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    // Create users
    Entity user = new Entity("User");

    user.setProperty("userEmail", "test@google.com");
    user.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());

    Entity user2 = new Entity("User");

    user2.setProperty("userEmail", "test2@google.com");
    user2.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user2.setProperty("taClasses", Collections.emptyList());
    user2.setProperty("ownedClasses", Collections.emptyList());

    Entity user3 = new Entity("User");

    user3.setProperty("userEmail", "test3@google.com");
    user3.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user3.setProperty("taClasses", Collections.emptyList());
    user3.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);
    datastore.put(user2);
    datastore.put(user3);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayStudent.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("test@google.com"));
    assertTrue(stringWriter.toString().contains("test2@google.com"));
    assertTrue(stringWriter.toString().contains("test3@google.com"));
  }

  @Test
  // Verify that a class with no students returns nothing
  public void empty() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayStudent.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("[]"));
  }
}

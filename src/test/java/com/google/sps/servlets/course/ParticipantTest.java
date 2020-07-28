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
public class ParticipantTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock FirebaseAuth authInstance;

  @InjectMocks Participants displayParticipants;

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
  // Check if a single TA participant is retrieved properly
  public void basicCheckTA() throws Exception {

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
    user.setProperty("taClasses", Arrays.asList(init.getKey()));
    user.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn("teach-staff");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("test@google.com"));
  }

  @Test
  // Check if a single student participant is retrieved properly
  public void basicCheckStudent() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    // Create a user
    Entity user = new Entity("User");

    user.setProperty("userEmail", "student1@google.com");
    user.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn("student");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("student1@google.com"));
  }

  @Test
  // Check if multiple TA participants are retrieved properly
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
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Arrays.asList(init.getKey()));
    user.setProperty("ownedClasses", Collections.emptyList());

    Entity user2 = new Entity("User");

    user2.setProperty("userEmail", "test2@google.com");
    user2.setProperty("registeredClasses", Collections.emptyList());
    user2.setProperty("taClasses", Arrays.asList(init.getKey()));
    user2.setProperty("ownedClasses", Collections.emptyList());

    Entity user3 = new Entity("User");

    user3.setProperty("userEmail", "test3@google.com");
    user3.setProperty("registeredClasses", Collections.emptyList());
    user3.setProperty("taClasses", Arrays.asList(init.getKey()));
    user3.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);
    datastore.put(user2);
    datastore.put(user3);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn("teach-staff");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("test@google.com"));
    assertTrue(stringWriter.toString().contains("test2@google.com"));
    assertTrue(stringWriter.toString().contains("test3@google.com"));
  }

  @Test
  // Check if multiple student participants are retrieved properly
  public void multipleStudents() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    // Create users
    Entity user = new Entity("User");

    user.setProperty("userEmail", "student1@google.com");
    user.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());

    Entity user2 = new Entity("User");

    user2.setProperty("userEmail", "student2@google.com");
    user2.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user2.setProperty("taClasses", Collections.emptyList());
    user2.setProperty("ownedClasses", Collections.emptyList());

    Entity user3 = new Entity("User");

    user3.setProperty("userEmail", "student3@google.com");
    user3.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user3.setProperty("taClasses", Collections.emptyList());
    user3.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);
    datastore.put(user2);
    datastore.put(user3);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn("student");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("student1@google.com"));
    assertTrue(stringWriter.toString().contains("student2@google.com"));
    assertTrue(stringWriter.toString().contains("student3@google.com"));
  }

  @Test
  // Verify that a class with no TAs returns nothing
  public void emptyTA() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn("teach-staff");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("[]"));
  }

  @Test
  // Verify that a class with no students returns nothing
  public void emptyStudent() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn("student");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("[]"));
  }

  @Test
  public void pickStudents() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    // Create users
    Entity user = new Entity("User");

    user.setProperty("userEmail", "student1@google.com");
    user.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());

    Entity user2 = new Entity("User");

    user2.setProperty("userEmail", "student2@google.com");
    user2.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user2.setProperty("taClasses", Collections.emptyList());
    user2.setProperty("ownedClasses", Collections.emptyList());

    Entity user3 = new Entity("User");

    user3.setProperty("userEmail", "ta1@google.com");
    user3.setProperty("registeredClasses", Collections.emptyList());
    user3.setProperty("taClasses", Arrays.asList(init.getKey()));
    user3.setProperty("ownedClasses", Collections.emptyList());

    Entity user4 = new Entity("User");

    user4.setProperty("userEmail", "ta2@google.com");
    user4.setProperty("registeredClasses", Collections.emptyList());
    user4.setProperty("taClasses", Arrays.asList(init.getKey()));
    user4.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);
    datastore.put(user2);
    datastore.put(user3);
    datastore.put(user4);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn("student");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("student1@google.com"));
    assertTrue(stringWriter.toString().contains("student2@google.com"));
  }

  @Test
  public void pickTAs() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    // Create users
    Entity user = new Entity("User");

    user.setProperty("userEmail", "student1@google.com");
    user.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());

    Entity user2 = new Entity("User");

    user2.setProperty("userEmail", "student2@google.com");
    user2.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    user2.setProperty("taClasses", Collections.emptyList());
    user2.setProperty("ownedClasses", Collections.emptyList());

    Entity user3 = new Entity("User");

    user3.setProperty("userEmail", "ta1@google.com");
    user3.setProperty("registeredClasses", Collections.emptyList());
    user3.setProperty("taClasses", Arrays.asList(init.getKey()));
    user3.setProperty("ownedClasses", Collections.emptyList());

    Entity user4 = new Entity("User");

    user4.setProperty("userEmail", "ta2@google.com");
    user4.setProperty("registeredClasses", Collections.emptyList());
    user4.setProperty("taClasses", Arrays.asList(init.getKey()));
    user4.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);
    datastore.put(user2);
    datastore.put(user3);
    datastore.put(user4);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn("teach-staff");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("ta1@google.com"));
    assertTrue(stringWriter.toString().contains("ta2@google.com"));
  }
}

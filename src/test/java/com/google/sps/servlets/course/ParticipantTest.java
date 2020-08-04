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
import com.google.sps.authentication.Authenticator;
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

  @Mock Authenticator auth;

  @InjectMocks Participants displayParticipants;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  private final String ID_TOKEN = "ID_TOKEN";
  private final String STAFF = "teach-staff";
  private final String STUDENT = "student";
  private final String STUDENT_1 = "student1@google.com";
  private final String STUDENT_2 = "student2@google.com";
  private final String STUDENT_3 = "student3@google.com";
  private final String TA_1 = "ta1@google.com";
  private final String TA_2 = "ta2@google.com";
  private final String TA_3 = "ta3@google.com";

  private Entity init;
  private Entity student1;
  private Entity student2;
  private Entity student3;
  private Entity ta1;
  private Entity ta2;
  private Entity ta3;

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();

    // Create a class
    init = new Entity("Class");

    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    //
    // Create student/TA users
    //
    student1 = new Entity("User");

    student1.setProperty("userEmail", STUDENT_1);
    student1.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    student1.setProperty("taClasses", Collections.emptyList());
    student1.setProperty("ownedClasses", Collections.emptyList());

    student2 = new Entity("User");

    student2.setProperty("userEmail", STUDENT_2);
    student2.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    student2.setProperty("taClasses", Collections.emptyList());
    student2.setProperty("ownedClasses", Collections.emptyList());

    student3 = new Entity("User");

    student3.setProperty("userEmail", STUDENT_3);
    student3.setProperty("registeredClasses", Arrays.asList(init.getKey()));
    student3.setProperty("taClasses", Collections.emptyList());
    student3.setProperty("ownedClasses", Collections.emptyList());

    ta1 = new Entity("User");

    ta1.setProperty("userEmail", TA_1);
    ta1.setProperty("registeredClasses", Collections.emptyList());
    ta1.setProperty("taClasses", Arrays.asList(init.getKey()));
    ta1.setProperty("ownedClasses", Collections.emptyList());

    ta2 = new Entity("User");

    ta2.setProperty("userEmail", TA_2);
    ta2.setProperty("registeredClasses", Collections.emptyList());
    ta2.setProperty("taClasses", Arrays.asList(init.getKey()));
    ta2.setProperty("ownedClasses", Collections.emptyList());

    ta2 = new Entity("User");

    ta2.setProperty("userEmail", TA_2);
    ta2.setProperty("registeredClasses", Collections.emptyList());
    ta2.setProperty("taClasses", Arrays.asList(init.getKey()));
    ta2.setProperty("ownedClasses", Collections.emptyList());

    ta3 = new Entity("User");

    ta3.setProperty("userEmail", TA_3);
    ta3.setProperty("registeredClasses", Collections.emptyList());
    ta3.setProperty("taClasses", Arrays.asList(init.getKey()));
    ta3.setProperty("ownedClasses", Collections.emptyList());
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  // Check if a single TA participant is retrieved properly
  public void basicCheckTA() throws Exception {

    datastore.put(init);
    datastore.put(ta1);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn(STAFF);
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains(TA_1));
  }

  @Test
  // Check if a single student participant is retrieved properly
  public void basicCheckStudent() throws Exception {

    datastore.put(init);
    datastore.put(student1);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn(STUDENT);
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains(STUDENT_1));
  }

  @Test
  // Check if multiple TA participants are retrieved properly
  public void multipleTAs() throws Exception {

    datastore.put(init);
    datastore.put(ta1);
    datastore.put(ta2);
    datastore.put(ta3);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn(STAFF);
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains(TA_1));
    assertTrue(stringWriter.toString().contains(TA_2));
    assertTrue(stringWriter.toString().contains(TA_3));
  }

  @Test
  // Check if multiple student participants are retrieved properly
  public void multipleStudents() throws Exception {

    datastore.put(init);
    datastore.put(student1);
    datastore.put(student2);
    datastore.put(student3);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn(STUDENT);
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains(STUDENT_1));
    assertTrue(stringWriter.toString().contains(STUDENT_2));
    assertTrue(stringWriter.toString().contains(STUDENT_3));
  }

  @Test
  // Verify that a class with no TAs returns nothing
  public void emptyTA() throws Exception {

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn(STAFF);
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("[]"));
  }

  @Test
  // Verify that a class with no students returns nothing
  public void emptyStudent() throws Exception {

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn(STUDENT);
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("[]"));
  }

  @Test
  public void pickStudents() throws Exception {

    datastore.put(init);
    datastore.put(student1);
    datastore.put(student2);
    datastore.put(ta1);
    datastore.put(ta2);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn(STUDENT);
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains(STUDENT_1));
    assertTrue(stringWriter.toString().contains(STUDENT_2));
  }

  @Test
  public void pickTAs() throws Exception {

    datastore.put(init);
    datastore.put(student1);
    datastore.put(student2);
    datastore.put(ta1);
    datastore.put(ta2);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("type")).thenReturn(STAFF);
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    displayParticipants.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains(TA_1));
    assertTrue(stringWriter.toString().contains(TA_2));
  }
}

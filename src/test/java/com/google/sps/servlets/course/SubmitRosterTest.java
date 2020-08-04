package com.google.sps.servlets.course;

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
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.sps.authentication.Authenticator;
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

  @Mock Authenticator auth;

  @Mock FirebaseAuth authInstance;

  @InjectMocks SubmitRoster submitRoster;

  private final String ID_TOKEN = "ID_TOKEN";
  private final String STUDENT_1 = "student1@google.com";
  private final String STUDENT_2 = "student2@google.com";
  private final String STUDENT_3 = "student3@google.com";
  private final String STUDENT_4 = "student4@google.com";

  private Entity init;
  private Entity init2;
  private Entity student1;
  private Entity student2;
  private Entity student3;
  private Entity student4;

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();

    //
    // Create classes
    //
    init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    init2 = new Entity("Class");

    init2.setProperty("owner", "ownerID2");
    init2.setProperty("name", "testClass2");
    init2.setProperty("beingHelped", new EmbeddedEntity());
    init2.setProperty("studentQueue", Collections.emptyList());

    //
    // Create student users
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

    List<Key> reg1 = Arrays.asList(init.getKey());
    student3.setProperty("userEmail", STUDENT_3);
    student3.setProperty("registeredClasses", reg1);
    student3.setProperty("taClasses", Collections.emptyList());
    student3.setProperty("ownedClasses", Collections.emptyList());

    student4 = new Entity("User");

    List<Key> reg2 = Arrays.asList(init.getKey());
    student4.setProperty("userEmail", STUDENT_4);
    student4.setProperty("registeredClasses", reg2);
    student4.setProperty("taClasses", Collections.emptyList());
    student4.setProperty("ownedClasses", Collections.emptyList());
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  // Create new student users when a roster is submitted
  public void addRosterNewStudents() throws Exception {

    datastore.put(init);

    // Submit a roster of 2 students
    when(httpRequest.getParameter("roster")).thenReturn("student1@google.com, student2@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, init.getKey())).thenReturn(true);

    submitRoster.doPost(httpRequest, httpResponse);

    // Look for the students in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, STUDENT_1)));
    PreparedQuery queryUser2 =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, STUDENT_2)));

    Entity userStudent = queryUser.asSingleEntity();
    Entity userStudent2 = queryUser2.asSingleEntity();

    List<Key> testRegistered = (List<Key>) userStudent.getProperty("registeredClasses");
    List<Key> testRegistered2 = (List<Key>) userStudent2.getProperty("registeredClasses");

    assertTrue(testRegistered.contains(init.getKey()));
    assertTrue(testRegistered.size() == 1);
    assertTrue(testRegistered2.contains(init.getKey()));
    assertTrue(testRegistered2.size() == 1);
  }

  @Test
  // User is already registered for one class, add another
  public void existingUserAddClass() throws Exception {

    datastore.put(init);
    datastore.put(init2);
    datastore.put(student3);

    // Submit a roster of 1 student
    when(httpRequest.getParameter("roster")).thenReturn(STUDENT_3);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init2.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, init2.getKey())).thenReturn(true);

    submitRoster.doPost(httpRequest, httpResponse);

    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, STUDENT_3)));

    Entity userStudent = queryUser.asSingleEntity();

    List<Key> testRegistered = (List<Key>) userStudent.getProperty("registeredClasses");
    assertTrue(testRegistered.contains(init.getKey()));
    assertTrue(testRegistered.contains(init2.getKey()));
    assertTrue(testRegistered.size() == 2);
  }

  @Test
  // If owner adds the same class to a user's registered list, verify that it doesn't get added
  public void duplicates() throws Exception {

    datastore.put(init);
    datastore.put(student3);

    // Attempt to add the same class to user's registered list
    when(httpRequest.getParameter("roster")).thenReturn(STUDENT_3);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, init.getKey())).thenReturn(true);

    submitRoster.doPost(httpRequest, httpResponse);

    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, STUDENT_3)));

    Entity userStudent = queryUser.asSingleEntity();

    List<Key> testRegistered = (List<Key>) userStudent.getProperty("registeredClasses");
    assertTrue(testRegistered.contains(init.getKey()));
    assertTrue(testRegistered.size() == 1);
  }

  @Test
  public void preventDuplicateMultipleStudents() throws Exception {

    datastore.put(init);
    datastore.put(student3);
    datastore.put(student4);

    when(httpRequest.getParameter("roster")).thenReturn("student3@google.com, student4@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, init.getKey())).thenReturn(true);

    submitRoster.doPost(httpRequest, httpResponse);

    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, STUDENT_3)));
    PreparedQuery queryUser2 =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, STUDENT_4)));

    Entity userStudent = queryUser.asSingleEntity();
    Entity userStudent2 = queryUser2.asSingleEntity();

    List<Key> testRegistered = (List<Key>) userStudent.getProperty("registeredClasses");
    List<Key> testRegistered2 = (List<Key>) userStudent2.getProperty("registeredClasses");

    assertTrue(testRegistered.contains(init.getKey()));
    assertTrue(testRegistered.size() == 1);
    assertTrue(testRegistered2.contains(init.getKey()));
    assertTrue(testRegistered2.size() == 1);
  }

  @Test
  public void existingStudents() throws Exception {

    datastore.put(init);
    datastore.put(student1);
    datastore.put(student2);

    when(httpRequest.getParameter("roster")).thenReturn("student1@google.com, student2@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, init.getKey())).thenReturn(true);

    submitRoster.doPost(httpRequest, httpResponse);

    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, STUDENT_1)));
    PreparedQuery queryUser2 =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, STUDENT_2)));

    Entity userStudent = queryUser.asSingleEntity();
    Entity userStudent2 = queryUser2.asSingleEntity();

    List<Key> testRegistered = (List<Key>) userStudent.getProperty("registeredClasses");
    List<Key> testRegistered2 = (List<Key>) userStudent2.getProperty("registeredClasses");

    assertTrue(testRegistered.contains(init.getKey()));
    assertTrue(testRegistered.size() == 1);
    assertTrue(testRegistered2.contains(init.getKey()));
    assertTrue(testRegistered2.size() == 1);
  }
}

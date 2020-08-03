package com.google.sps.servlets.course;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
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
public class AddClassTATest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock FirebaseAuth authInstance;

  @Mock Authenticator auth;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @InjectMocks AddClassTA addTA;

  private final String ID_TOKEN = "ID_TOKEN";
  private final String TA_EMAIL = "test@google.com";
  private final String EMAIL_2 = "test2@google.com";

  private Entity init;
  private Entity init2;
  private Entity init3;
  private Entity init4;
  private Entity user;
  private Entity user2;

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();

    // Create classes
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

    init3 = new Entity("Class");

    init3.setProperty("owner", "ownerID3");
    init3.setProperty("name", "testClass3");
    init3.setProperty("beingHelped", new EmbeddedEntity());
    init3.setProperty("studentQueue", Collections.emptyList());

    init4 = new Entity("Class");

    init4.setProperty("owner", "ownerID4");
    init4.setProperty("name", "testClass4");
    init4.setProperty("beingHelped", new EmbeddedEntity());
    init4.setProperty("studentQueue", Collections.emptyList());

    // Create users
    user = new Entity("User");

    user.setProperty("userEmail", TA_EMAIL);
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());

    user2 = new Entity("User");

    user2.setProperty("userEmail", EMAIL_2);
    user2.setProperty("registeredClasses", Collections.emptyList());
    user2.setProperty("taClasses", Collections.emptyList());
    user2.setProperty("ownedClasses", Collections.emptyList());
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  // For a user that doesn't TA for any class, add a class
  public void addOneTAEmptyList() throws Exception {

    datastore.put(init);
    datastore.put(user);

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn(TA_EMAIL);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    addTA.doPost(httpRequest, httpResponse);

    // Look for the TA in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, TA_EMAIL)));

    Entity userTA = queryUser.asSingleEntity();

    List<Key> taClasses = (List<Key>) userTA.getProperty("taClasses");
    assertTrue(taClasses.contains(init.getKey()));
    assertTrue(taClasses.size() == 1);
  }

  @Test
  // For a user that already TAs for one class, add another class
  public void addOneTANonEmptyList() throws Exception {

    datastore.put(init);
    datastore.put(init2);

    // Initialize a user
    Entity testUser = new Entity("User");

    List<Key> taClassList = Arrays.asList(init.getKey());

    testUser.setProperty("userEmail", TA_EMAIL);
    testUser.setProperty("registeredClasses", Collections.emptyList());
    testUser.setProperty("taClasses", taClassList);
    testUser.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(testUser);

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn(TA_EMAIL);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init2.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(eq(ID_TOKEN), eq(KeyFactory.keyToString(init2.getKey()))))
        .thenReturn(true);

    addTA.doPost(httpRequest, httpResponse);

    // Look for the TA in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, TA_EMAIL)));

    Entity userTA = queryUser.asSingleEntity();

    List<Key> taClasses = (List<Key>) userTA.getProperty("taClasses");
    assertTrue(taClasses.contains(init.getKey()));
    assertTrue(taClasses.contains(init2.getKey()));
    assertTrue(taClasses.size() == 2);
  }

  @Test
  // Verify that duplicate classes don't get added
  public void preventDuplicates() throws Exception {

    datastore.put(init);
    datastore.put(init2);

    // Initialize a TA user
    Entity testUser = new Entity("User");

    List<Key> taClassList = Arrays.asList(init.getKey(), init2.getKey());

    testUser.setProperty("userEmail", TA_EMAIL);
    testUser.setProperty("registeredClasses", Collections.emptyList());
    testUser.setProperty("taClasses", taClassList);
    testUser.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(testUser);

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn(TA_EMAIL);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    addTA.doPost(httpRequest, httpResponse);

    // Look for the TA in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, TA_EMAIL)));

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

    datastore.put(init);
    datastore.put(init2);
    datastore.put(init3);
    datastore.put(init4);

    // Create a user
    Entity testUser = new Entity("User");

    List<Key> taClassList = Arrays.asList(init.getKey(), init2.getKey(), init3.getKey());

    testUser.setProperty("userEmail", TA_EMAIL);
    testUser.setProperty("registeredClasses", Collections.emptyList());
    testUser.setProperty("taClasses", taClassList);
    testUser.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(testUser);

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn(TA_EMAIL);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init4.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init4.getKey()))).thenReturn(true);

    addTA.doPost(httpRequest, httpResponse);

    // Look for the TA in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, TA_EMAIL)));

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

    datastore.put(init);
    datastore.delete(init.getKey());

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn(TA_EMAIL);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    addTA.doPost(httpRequest, httpResponse);

    verify(httpResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  // Add multiple TA's at the same time
  public void addOneMultiple() throws Exception {

    datastore.put(init);
    datastore.put(user);
    datastore.put(user2);

    // Create examples for the TA email and class code
    when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com,test2@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    addTA.doPost(httpRequest, httpResponse);

    Entity userTA = datastore.get(user.getKey());
    Entity userTA2 = datastore.get(user2.getKey());

    List<Key> taClasses = (List<Key>) userTA.getProperty("taClasses");
    assertTrue(taClasses.contains(init.getKey()));
    assertTrue(taClasses.size() == 1);

    List<Key> taClasses2 = (List<Key>) userTA2.getProperty("taClasses");
    assertTrue(taClasses2.contains(init.getKey()));
    assertTrue(taClasses2.size() == 1);
  }

  @Test
  // Add multiple TA's at the same time with whitespace
  public void addOneMultipleWhitespace() throws Exception {

    datastore.put(init);
    datastore.put(user);
    datastore.put(user2);

    when(httpRequest.getParameter("taEmail")).thenReturn("test@google.com, \n\t test2@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    addTA.doPost(httpRequest, httpResponse);

    Entity userTA = datastore.get(user.getKey());
    Entity userTA2 = datastore.get(user2.getKey());

    List<Key> taClasses = (List<Key>) userTA.getProperty("taClasses");
    assertTrue(taClasses.contains(init.getKey()));
    assertTrue(taClasses.size() == 1);

    List<Key> taClasses2 = (List<Key>) userTA2.getProperty("taClasses");
    assertTrue(taClasses2.contains(init.getKey()));
    assertTrue(taClasses2.size() == 1);
  }
}

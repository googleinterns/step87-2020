package com.google.sps.servlets.course;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
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
public class AddOwnerTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock FirebaseAuth authInstance;

  @InjectMocks AddOwner addOwner;

  @Mock Authenticator auth;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  private final String ID_TOKEN = "ID_TOKEN";

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
  // For a user that doesn't own any class, add a class
  public void addOneOwnerEmptyList() throws Exception {

    // Create a class
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    // Create a user
    Entity user = new Entity("User");

    user.setProperty("userEmail", "testOwner@google.com");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", Collections.emptyList());

    datastore.put(user);

    when(httpRequest.getParameter("ownerEmail")).thenReturn("testOwner@google.com");
    when(httpRequest.getParameter("idTokenOwner")).thenReturn(ID_TOKEN);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(auth.verifyOwner(eq(ID_TOKEN), eq(init.getKey()))).thenReturn(true);

    System.out.println("Testing1");

    addOwner.doPost(httpRequest, httpResponse);

    System.out.println("Testing2");

    // Look for the owner in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(
                    new FilterPredicate(
                        "userEmail", FilterOperator.EQUAL, "testOwner@google.com")));

    Entity owner = queryUser.asSingleEntity();

    List<Key> ownedClasses = (List<Key>) owner.getProperty("ownedClasses");
    assertTrue(ownedClasses.contains(init.getKey()));
    assertTrue(ownedClasses.size() == 1);
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

    // Initialize an owner user
    Entity user = new Entity("User");

    List<Key> ownedClassList = Arrays.asList(init.getKey(), init2.getKey());

    user.setProperty("userEmail", "testOwner@google.com");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", ownedClassList);

    datastore.put(user);

    when(httpRequest.getParameter("ownerEmail")).thenReturn("testOwner@google.com");
    when(httpRequest.getParameter("idTokenOwner")).thenReturn(ID_TOKEN);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(auth.verifyOwner(eq(ID_TOKEN), eq(init.getKey()))).thenReturn(true);

    addOwner.doPost(httpRequest, httpResponse);

    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(
                    new FilterPredicate(
                        "userEmail", FilterOperator.EQUAL, "testOwner@google.com")));

    Entity owner = queryUser.asSingleEntity();

    // Verify the owner class list stayed the same
    List<Key> ownedClasses = (List<Key>) owner.getProperty("ownedClasses");
    assertTrue(ownedClasses.contains(init.getKey()));
    assertTrue(ownedClasses.contains(init2.getKey()));
    assertTrue(ownedClasses.size() == 2);
  }

  @Test
  // Add multiple classes for owner
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

    List<Key> ownedClassList = Arrays.asList(init.getKey(), init2.getKey(), init3.getKey());

    user.setProperty("userEmail", "test@google.com");
    user.setProperty("registeredClasses", Collections.emptyList());
    user.setProperty("taClasses", Collections.emptyList());
    user.setProperty("ownedClasses", ownedClassList);

    datastore.put(user);

    when(httpRequest.getParameter("ownerEmail")).thenReturn("test@google.com");
    when(httpRequest.getParameter("idTokenOwner")).thenReturn(ID_TOKEN);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init4.getKey()));
    when(auth.verifyOwner(eq(ID_TOKEN), eq(init4.getKey()))).thenReturn(true);

    addOwner.doPost(httpRequest, httpResponse);

    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(
                    new FilterPredicate("userEmail", FilterOperator.EQUAL, "test@google.com")));

    Entity owner = queryUser.asSingleEntity();

    List<Key> ownedClasses = (List<Key>) owner.getProperty("ownedClasses");

    // Verify that all the classes were stored
    assertTrue(ownedClasses.contains(init.getKey()));
    assertTrue(ownedClasses.contains(init2.getKey()));
    assertTrue(ownedClasses.contains(init3.getKey()));
    assertTrue(ownedClasses.contains(init4.getKey()));
    assertTrue(ownedClasses.size() == 4);
  }

  //   @Test
  //   // Throw an exception if class key isn't correct
  //   public void keyUnavailable() throws Exception {

  //     // Create examples for the TA email and class code
  //     when(httpRequest.getParameter("ownerEmail")).thenReturn("test@google.com");
  //     when(httpRequest.getParameter("classCode")).thenReturn("testClassCode");
  //     when(httpRequest.getParameter("idTokenOwner")).thenReturn("ownerID");

  //     addOwner.doPost(httpRequest, httpResponse);

  //     verify(httpResponse).sendError(HttpServletResponse.SC_BAD_REQUEST);
  //   }
}

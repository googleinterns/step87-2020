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
  private final String OWNER_EMAIL_1 = "testOwner@google.com";
  private final String OWNER_EMAIL_2 = "testOwner2@google.com";
  private final String OWNER_EMAIL_3 = "testOwner3@google.com";

  private Entity init;
  private Entity init2;
  private Entity init3;
  private Entity init4;
  private Entity userOwner1;
  private Entity userOwner2;
  private Entity userOwner3;

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

    //
    // Create users that are owners
    //
    userOwner1 = new Entity("User");

    userOwner1.setProperty("userEmail", OWNER_EMAIL_1);
    userOwner1.setProperty("registeredClasses", Collections.emptyList());
    userOwner1.setProperty("taClasses", Collections.emptyList());
    userOwner1.setProperty("ownedClasses", Collections.emptyList());

    userOwner2 = new Entity("User");

    List<Key> ownedClassList2 = Arrays.asList(init.getKey(), init2.getKey());
    userOwner2.setProperty("userEmail", OWNER_EMAIL_2);
    userOwner2.setProperty("registeredClasses", Collections.emptyList());
    userOwner2.setProperty("taClasses", Collections.emptyList());
    userOwner2.setProperty("ownedClasses", ownedClassList2);

    userOwner3 = new Entity("User");

    List<Key> ownedClassList3 = Arrays.asList(init.getKey(), init2.getKey(), init3.getKey());
    userOwner3.setProperty("userEmail", OWNER_EMAIL_3);
    userOwner3.setProperty("registeredClasses", Collections.emptyList());
    userOwner3.setProperty("taClasses", Collections.emptyList());
    userOwner3.setProperty("ownedClasses", ownedClassList3);
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  // For a user that doesn't own any class, add a class
  public void addOneOwnerEmptyList() throws Exception {

    datastore.put(init);
    datastore.put(userOwner1);

    when(httpRequest.getParameter("ownerEmail")).thenReturn(OWNER_EMAIL_1);
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(auth.verifyOwner(eq(ID_TOKEN), eq(init.getKey()))).thenReturn(true);

    addOwner.doPost(httpRequest, httpResponse);

    // Look for the owner in the user datastore
    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, OWNER_EMAIL_1)));

    Entity owner = queryUser.asSingleEntity();

    List<Key> ownedClasses = (List<Key>) owner.getProperty("ownedClasses");
    assertTrue(ownedClasses.contains(init.getKey()));
    assertTrue(ownedClasses.size() == 1);
  }

  @Test
  // Verify that duplicate classes don't get added
  public void preventDuplicates() throws Exception {

    datastore.put(init);
    datastore.put(init2);
    datastore.put(userOwner2);

    when(httpRequest.getParameter("ownerEmail")).thenReturn(OWNER_EMAIL_2);
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(auth.verifyOwner(eq(ID_TOKEN), eq(init.getKey()))).thenReturn(true);

    addOwner.doPost(httpRequest, httpResponse);

    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, OWNER_EMAIL_2)));

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

    datastore.put(init);
    datastore.put(init2);
    datastore.put(init3);
    datastore.put(init4);
    datastore.put(userOwner3);

    when(httpRequest.getParameter("ownerEmail")).thenReturn(OWNER_EMAIL_3);
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init4.getKey()));
    when(auth.verifyOwner(eq(ID_TOKEN), eq(init4.getKey()))).thenReturn(true);

    addOwner.doPost(httpRequest, httpResponse);

    PreparedQuery queryUser =
        datastore.prepare(
            new Query("User")
                .setFilter(new FilterPredicate("userEmail", FilterOperator.EQUAL, OWNER_EMAIL_3)));

    Entity owner = queryUser.asSingleEntity();

    List<Key> ownedClasses = (List<Key>) owner.getProperty("ownedClasses");

    // Verify that all the classes were stored
    assertTrue(ownedClasses.contains(init.getKey()));
    assertTrue(ownedClasses.contains(init2.getKey()));
    assertTrue(ownedClasses.contains(init3.getKey()));
    assertTrue(ownedClasses.contains(init4.getKey()));
    assertTrue(ownedClasses.size() == 4);
  }
}

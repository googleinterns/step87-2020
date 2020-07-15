package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import java.util.ArrayList;
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
public class CreateUserTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @InjectMocks CreateUser createUser;

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
  // Register a single user and add a User entity
  public void addUser() throws Exception {
    when(httpRequest.getParameter("userToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("123ABC");

    UserRecord mockUser = mock(UserRecord.class);
    when(authInstance.getUser("123ABC")).thenReturn(mockUser);
    when(mockUser.getEmail()).thenReturn("exampleUserEmail");

    createUser.doGet(httpRequest, httpResponse);

    Entity testUserEntity = datastore.prepare(new Query("User")).asSingleEntity();

    assertEquals(testUserEntity.getProperty("userEmail"), "exampleUserEmail");

    ArrayList<Key> ownerList = (ArrayList<Key>) testUserEntity.getProperty("ownedClasses");
    ArrayList<Key> taList = (ArrayList<Key>) testUserEntity.getProperty("taClasses");
    ArrayList<Key> registeredList =
        (ArrayList<Key>) testUserEntity.getProperty("registeredClasses");

    assertTrue(ownerList.isEmpty());
    assertTrue(taList.isEmpty());
    assertTrue(registeredList.isEmpty());
  }

  @Test
  public void addDuplicateUser() throws Exception {

    when(httpRequest.getParameter("userToken")).thenReturn("testID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("123ABC");

    UserRecord mockUser = mock(UserRecord.class);
    when(authInstance.getUser("123ABC")).thenReturn(mockUser);
    when(mockUser.getEmail()).thenReturn("exampleUserEmail");

    Entity userEntity = new Entity("User");

    userEntity.setProperty("userEmail", "exampleUserEmail");
    userEntity.setProperty("ownedClasses", Collections.emptyList());
    userEntity.setProperty("registeredClasses", Collections.emptyList());
    userEntity.setProperty("taClasses", Collections.emptyList());

    datastore.put(userEntity);

    createUser.doGet(httpRequest, httpResponse);

    verify(httpResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

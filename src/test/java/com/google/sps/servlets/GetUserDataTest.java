package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GetUserDataTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @InjectMocks GetUserData getUserData;

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
  public void newUser() throws Exception {
    Entity initClass = new Entity("Class");

    initClass.setProperty("owner", "ownerID");
    initClass.setProperty("name", "testClass");
    initClass.setProperty("beingHelped", new EmbeddedEntity());
    initClass.setProperty("studentQueue", Collections.emptyList());

    datastore.put(initClass);

    when(httpRequest.getParameter("idToken")).thenReturn("uID");
    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("uID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("uID");

    UserRecord mockUser = mock(UserRecord.class);
    when(authInstance.getUser("uID")).thenReturn(mockUser);
    when(mockUser.getEmail()).thenReturn("user@google.com");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    getUserData.doGet(httpRequest, httpResponse);

    assertEquals("null", stringWriter.toString());

    Entity testUserEntity = datastore.prepare(new Query("User")).asSingleEntity();
    assertEquals(testUserEntity.getProperty("userEmail"), "user@google.com");
  }

  @Test
  public void existingUser() throws Exception {
    Entity initClass = new Entity("Class");

    initClass.setProperty("owner", "ownerID");
    initClass.setProperty("name", "testClass");
    initClass.setProperty("beingHelped", new EmbeddedEntity());
    initClass.setProperty("studentQueue", Collections.emptyList());

    datastore.put(initClass);

    Entity initUser = new Entity("User");

    initUser.setProperty("userEmail", "user@google.com");
    initUser.setProperty("registeredClasses", Collections.emptyList());
    initUser.setProperty("ownedClasses", Collections.emptyList());
    initUser.setProperty("taClasses", Arrays.asList(initClass.getKey()));

    datastore.put(initUser);

    when(httpRequest.getParameter("idToken")).thenReturn("uID");
    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("uID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("uID");

    when(httpRequest.getParameter("taClasses")).thenReturn("taClasses");

    UserRecord mockUser = mock(UserRecord.class);
    when(authInstance.getUser("uID")).thenReturn(mockUser);
    when(mockUser.getEmail()).thenReturn("user@google.com");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    getUserData.doGet(httpRequest, httpResponse);

    Map<String, String> classMap = new HashMap<>();
    classMap.put(KeyFactory.keyToString(initClass.getKey()), "testClass");

    assertEquals(new JSONObject(classMap).toString(), stringWriter.toString());

    Entity testUserEntity = datastore.prepare(new Query("User")).asSingleEntity();
    assertTrue(datastore.prepare(new Query("User")).countEntities() == 1);
    assertEquals(testUserEntity.getProperty("userEmail"), "user@google.com");
  }
}

package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.gson.Gson;
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
public class GetUserRoleTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @InjectMocks GetUserRole getUserRole;

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
  public void getTAName() throws Exception {
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
    when(mockToken.getEmail()).thenReturn("user@google.com");

    when(httpRequest.getParameter("classCode"))
        .thenReturn(KeyFactory.keyToString(initClass.getKey()));
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    getUserRole.doGet(httpRequest, httpResponse);

    assertEquals(new Gson().toJson("TA"), stringWriter.toString());
  }

  @Test
  public void getOwnerName() throws Exception {
    Entity initClass = new Entity("Class");

    initClass.setProperty("owner", "ownerID");
    initClass.setProperty("name", "testClass");
    initClass.setProperty("beingHelped", new EmbeddedEntity());
    initClass.setProperty("studentQueue", Collections.emptyList());

    datastore.put(initClass);

    Entity initUser = new Entity("User");

    initUser.setProperty("userEmail", "user@google.com");
    initUser.setProperty("registeredClasses", Collections.emptyList());
    initUser.setProperty("ownedClasses", Arrays.asList(initClass.getKey()));
    initUser.setProperty("taClasses", Collections.emptyList());

    datastore.put(initUser);

    when(httpRequest.getParameter("idToken")).thenReturn("uID");

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("uID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("uID");
    when(mockToken.getEmail()).thenReturn("user@google.com");

    when(httpRequest.getParameter("classCode"))
        .thenReturn(KeyFactory.keyToString(initClass.getKey()));
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    getUserRole.doGet(httpRequest, httpResponse);

    assertEquals(new Gson().toJson("owner"), stringWriter.toString());
  }
}

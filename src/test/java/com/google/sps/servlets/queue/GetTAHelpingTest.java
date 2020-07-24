package com.google.sps.servlets.queue;

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
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
public class GetTAHelpingTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @Mock Gson gson;

  @InjectMocks GetTAHelping getTA;

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
  public void helpedBy() throws Exception {
    Entity init = new Entity("Class");
    ArrayList<String> setQueue = new ArrayList<String>(Arrays.asList("uID1", "uID2"));

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("studentQueue", setQueue);

    EmbeddedEntity queueInfo = new EmbeddedEntity();
    queueInfo.setProperty("taID", "taID");
    queueInfo.setProperty("workspaceID", "workspaceID");

    EmbeddedEntity beingHelped = new EmbeddedEntity();
    beingHelped.setProperty("studentID", queueInfo);

    init.setProperty("beingHelped", beingHelped);

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    when(httpRequest.getParameter("studentToken")).thenReturn("testID");
    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("studentID");

    UserRecord mockTA = mock(UserRecord.class);
    when(authInstance.getUser("taID")).thenReturn(mockTA);
    when(mockTA.getEmail()).thenReturn("taEmail");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    when(gson.toJson("taEmail")).thenReturn("taEmail");

    getTA.doGet(httpRequest, httpResponse);

    assertEquals("taEmail", stringWriter.toString());
  }

  @Test
  public void doneHelped() throws Exception {
    Entity init = new Entity("Class");
    ArrayList<String> setQueue = new ArrayList<String>(Arrays.asList("uID1", "uID2"));

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("studentQueue", setQueue);

    EmbeddedEntity queueInfo = new EmbeddedEntity();
    queueInfo.setProperty("taID", "taID");
    queueInfo.setProperty("workspaceID", "workspaceID");

    EmbeddedEntity beingHelped = new EmbeddedEntity();
    beingHelped.setProperty("studentID", queueInfo);

    init.setProperty("beingHelped", beingHelped);

    datastore.put(init);

    when(httpRequest.getParameter("studentToken")).thenReturn("studentToken");
    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("studentToken")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("dne");

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    when(gson.toJson("null")).thenReturn("null");

    getTA.doGet(httpRequest, httpResponse);

    assertEquals("null", stringWriter.toString());
  }
}

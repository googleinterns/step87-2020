package com.google.sps.servlets.queue;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.google.sps.authentication.Authenticator;
import com.google.sps.queue.Queue;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.server.UID;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
public class GetQueueTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;
  @Mock HttpServletResponse httpResponse;
  @Mock FirebaseAuth authInstance;
  @Mock Authenticator auth;

  @InjectMocks GetQueue queue;

  private static final LocalDate LOCAL_DATE = LocalDate.of(2020, 07, 06);
  private static final Date DATE =
      Date.from(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());
  private String ID_TOKEN = "ID_TOKEN";
  private String UID = "UID";
  private String HELPING_ID = "HELPING_ID";
  private String HELPING_EMAIL = "HELPING_EMAIL";
  private String WORKSPACE_ID = "WORKSPACE_ID";

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
  public void getQueue() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());

    EmbeddedEntity addQueue1 = new EmbeddedEntity();
    addQueue1.setProperty("timeEntered", DATE);
    addQueue1.setProperty("workspaceID", WORKSPACE_ID);
    addQueue1.setProperty("uID", "uID1");

    EmbeddedEntity addQueue2 = new EmbeddedEntity();
    addQueue2.setProperty("timeEntered", DATE);
    addQueue2.setProperty("workspaceID", WORKSPACE_ID);
    addQueue2.setProperty("uID", "uID2");

    init.setProperty("studentQueue", Arrays.asList(addQueue1, addQueue2));

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when((httpRequest.getParameter("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    FirebaseToken token = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken(eq(ID_TOKEN))).thenReturn(token);

    UserRecord mockUser1 = mock(UserRecord.class);
    when(authInstance.getUser("uID1")).thenReturn(mockUser1);
    when(mockUser1.getEmail()).thenReturn("test1@google.com");

    UserRecord mockUser2 = mock(UserRecord.class);
    when(authInstance.getUser("uID2")).thenReturn(mockUser2);
    when(mockUser2.getEmail()).thenReturn("test2@google.com");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    queue.doGet(httpRequest, httpResponse);

    Gson gson = new Gson();
    assertEquals(
        gson.toJson(new Queue(Arrays.asList("test1@google.com", "test2@google.com"), null)),
        stringWriter.toString());
  }

  @Test
  public void emptyQueue() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());

    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when((httpRequest.getParameter("idToken"))).thenReturn(ID_TOKEN);

    FirebaseToken token = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken(eq(ID_TOKEN))).thenReturn(token);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    queue.doGet(httpRequest, httpResponse);

    Gson gson = new Gson();
    assertEquals(gson.toJson(new Queue(Collections.emptyList(), null)), stringWriter.toString());
  }

  @Test
  public void getQueueWithHelping() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    EmbeddedEntity beingHelped = new EmbeddedEntity();
    EmbeddedEntity helping = new EmbeddedEntity();
    helping.setProperty("taID", UID);
    helping.setProperty("workspaceID", WORKSPACE_ID);
    beingHelped.setProperty(HELPING_ID, helping);
    init.setProperty("beingHelped", beingHelped);

    EmbeddedEntity addQueue1 = new EmbeddedEntity();
    addQueue1.setProperty("timeEntered", DATE);
    addQueue1.setProperty("workspaceID", WORKSPACE_ID);
    addQueue1.setProperty("uID", "uID1");

    EmbeddedEntity addQueue2 = new EmbeddedEntity();
    addQueue2.setProperty("timeEntered", DATE);
    addQueue2.setProperty("workspaceID", WORKSPACE_ID);
    addQueue2.setProperty("uID", "uID2");

    init.setProperty("studentQueue", Arrays.asList(addQueue1, addQueue2));

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when((httpRequest.getParameter("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    FirebaseToken token = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken(eq(ID_TOKEN))).thenReturn(token);
    when(token.getUid()).thenReturn(UID);

    UserRecord helpingUser = mock(UserRecord.class);
    when(authInstance.getUser(eq(HELPING_ID))).thenReturn(helpingUser);
    when(helpingUser.getEmail()).thenReturn(HELPING_EMAIL);

    UserRecord mockUser1 = mock(UserRecord.class);
    when(authInstance.getUser("uID1")).thenReturn(mockUser1);
    when(mockUser1.getEmail()).thenReturn("test1@google.com");

    UserRecord mockUser2 = mock(UserRecord.class);
    when(authInstance.getUser("uID2")).thenReturn(mockUser2);
    when(mockUser2.getEmail()).thenReturn("test2@google.com");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    queue.doGet(httpRequest, httpResponse);

    Gson gson = new Gson();
    assertEquals(
        gson.toJson(
            new Queue(
                Arrays.asList("test1@google.com", "test2@google.com"),
                new Queue.Helping(HELPING_EMAIL, WORKSPACE_ID))),
        stringWriter.toString());
  }

  @Test
  public void isStudent() throws Exception {
    Entity init = new Entity("Class");
    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);

    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(false);

    queue.doGet(httpRequest, httpResponse);

    verify(httpResponse, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

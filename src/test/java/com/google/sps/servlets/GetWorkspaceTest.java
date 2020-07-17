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
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;
import java.io.PrintWriter;
import java.io.StringWriter;
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
public class GetWorkspaceTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;
  private static final LocalDate LOCAL_DATE = LocalDate.of(2020, 07, 06);
  private static final Date DATE =
      Date.from(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @Mock Gson gson;

  @InjectMocks GetWorkspace getWorkspace;

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
  public void getTALink() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("studentQueue", Collections.emptyList());

    EmbeddedEntity queueInfo = new EmbeddedEntity();
    queueInfo.setProperty("taID", "taID");
    queueInfo.setProperty("workspaceID", "workspaceID");

    EmbeddedEntity beingHelped = new EmbeddedEntity();
    beingHelped.setProperty("studentID", queueInfo);

    init.setProperty("beingHelped", beingHelped);

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    when(httpRequest.getParameter("studentEmail")).thenReturn("test@google.com");
    UserRecord mockUser = mock(UserRecord.class);
    when(authInstance.getUserByEmail("test@google.com")).thenReturn(mockUser);
    when(mockUser.getUid()).thenReturn("studentID");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    when(gson.toJson("/workspace/?workspaceID=workspaceID"))
        .thenReturn("/workspace/?workspaceID=workspaceID");

    getWorkspace.doGet(httpRequest, httpResponse);

    assertEquals("/workspace/?workspaceID=workspaceID", stringWriter.toString());
  }

  @Test
  public void getStudentLink() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("studentQueue", Collections.emptyList());

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

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    when(gson.toJson("/workspace/?workspaceID=workspaceID"))
        .thenReturn("/workspace/?workspaceID=workspaceID");

    getWorkspace.doGet(httpRequest, httpResponse);

    assertEquals("/workspace/?workspaceID=workspaceID", stringWriter.toString());
  }

  @Test
  public void getStudentLinkEarly() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");

    EmbeddedEntity addQueue = new EmbeddedEntity();
    EmbeddedEntity studentInfo = new EmbeddedEntity();
    studentInfo.setProperty("timeEntered", DATE);
    studentInfo.setProperty("workspaceID", "workspaceID");
    addQueue.setProperty("studentID", studentInfo);

    init.setProperty("studentQueue", Arrays.asList(addQueue));

    init.setProperty("beingHelped", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    when(httpRequest.getParameter("studentToken")).thenReturn("testID");
    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(authInstance.verifyIdToken("testID")).thenReturn(mockToken);
    when(mockToken.getUid()).thenReturn("studentID");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    when(gson.toJson("/workspace/?workspaceID=workspaceID"))
        .thenReturn("/workspace/?workspaceID=workspaceID");

    getWorkspace.doGet(httpRequest, httpResponse);

    assertEquals("/workspace/?workspaceID=workspaceID", stringWriter.toString());
  }
}

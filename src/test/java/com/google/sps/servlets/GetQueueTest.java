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
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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

  @Mock Gson gson;

  @InjectMocks GetQueue queue;

  private static final LocalDate LOCAL_DATE = LocalDate.of(2020, 07, 06);
  private static final Date DATE =
      Date.from(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());

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
    init.setProperty("beingHelped", "");

    EmbeddedEntity addQueue1 = new EmbeddedEntity();
    EmbeddedEntity studentInfo1 = new EmbeddedEntity();
    studentInfo1.setProperty("timeEntered", DATE);
    addQueue1.setProperty("uID1", studentInfo1);

    EmbeddedEntity addQueue2 = new EmbeddedEntity();
    EmbeddedEntity studentInfo2 = new EmbeddedEntity();
    studentInfo2.setProperty("timeEntered", DATE);
    addQueue2.setProperty("uID2", studentInfo2);

    init.setProperty("studentQueue", Arrays.asList(addQueue1, addQueue2));

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    UserRecord mockUser1 = mock(UserRecord.class);
    when(authInstance.getUser("uID1")).thenReturn(mockUser1);
    when(mockUser1.getEmail()).thenReturn("test1@google.com");

    UserRecord mockUser2 = mock(UserRecord.class);
    when(authInstance.getUser("uID2")).thenReturn(mockUser2);
    when(mockUser2.getEmail()).thenReturn("test2@google.com");

    when(gson.toJson(new ArrayList<String>(Arrays.asList("test1@google.com", "test2@google.com"))))
        .thenReturn("test1@google.com, test2@google.com");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    queue.doGet(httpRequest, httpResponse);

    assertEquals("test1@google.com, test2@google.com", stringWriter.toString());
  }

  @Test
  public void emptyQueue() throws Exception {
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", "");

    init.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    when(gson.toJson(new ArrayList<String>())).thenReturn("");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    queue.doGet(httpRequest, httpResponse);

    assertEquals("", stringWriter.toString());
  }
}

package com.google.sps.servlets;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
public class WaitTimeTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @InjectMocks WaitTime wait;

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
  // Get average wait time for a single class, single day
  public void basic() throws Exception {
    Date date1 = new Date(2020, 1, 1);

    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    datastore.put(init);

    // Create a test entity in Wait
    Entity waitEntity = new Entity("Wait");
    waitEntity.setProperty("classKey", init.getKey());

    ArrayList<Long> waitDurList = new ArrayList<Long>(Arrays.asList(10L, 3L, 6L, 1L));

    waitEntity.setProperty("waitDurations", waitDurList);
    waitEntity.setProperty("date", date1);

    datastore.put(waitEntity);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    when(httpResponse.getWriter()).thenReturn(writer);

    wait.doGet(httpRequest, httpResponse); // Servlet response

    Gson gson = new Gson();

    assertTrue(stringWriter.toString().contains(gson.toJson(date1)));
    assertTrue(stringWriter.toString().contains("5"));
  }

  @Test
  public void multipleDatesOneClass() throws Exception {
    Date date1 = new Date(2020, 1, 1);
    Date date2 = new Date(2020, 1, 2);
    Date date3 = new Date(2020, 1, 3);
    Date date4 = new Date(2020, 1, 4);

    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    datastore.put(init);

    ArrayList<Long> waitDurList1 = new ArrayList<Long>(Arrays.asList(10L, 3L, 6L, 1L));
    ArrayList<Long> waitDurList2 = new ArrayList<Long>(Arrays.asList(3L, 6L));
    ArrayList<Long> waitDurList3 = new ArrayList<Long>(Arrays.asList(45L, 17L, 11L));
    ArrayList<Long> waitDurList4 = new ArrayList<Long>(Arrays.asList(10L, 33L, 6L, 19L));

    //
    // Target class has 4 different wait dates
    //
    Entity waitEntity1 = new Entity("Wait");
    waitEntity1.setProperty("classKey", init.getKey());
    waitEntity1.setProperty("waitDurations", waitDurList1);
    waitEntity1.setProperty("date", date1);

    Entity waitEntity2 = new Entity("Wait");
    waitEntity2.setProperty("classKey", init.getKey());
    waitEntity2.setProperty("waitDurations", waitDurList2);
    waitEntity2.setProperty("date", date2);

    Entity waitEntity3 = new Entity("Wait");
    waitEntity3.setProperty("classKey", init.getKey());
    waitEntity3.setProperty("waitDurations", waitDurList3);
    waitEntity3.setProperty("date", date3);

    Entity waitEntity4 = new Entity("Wait");
    waitEntity4.setProperty("classKey", init.getKey());
    waitEntity4.setProperty("waitDurations", waitDurList4);
    waitEntity4.setProperty("date", date4);

    datastore.put(waitEntity1);
    datastore.put(waitEntity2);
    datastore.put(waitEntity3);
    datastore.put(waitEntity4);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    when(httpResponse.getWriter()).thenReturn(writer);

    wait.doGet(httpRequest, httpResponse); // Servlet response

    Gson gson = new Gson();

    assertTrue(stringWriter.toString().contains(gson.toJson(date1)));
    assertTrue(stringWriter.toString().contains(gson.toJson(date2)));
    assertTrue(stringWriter.toString().contains(gson.toJson(date3)));
    assertTrue(stringWriter.toString().contains(gson.toJson(date4)));
    assertTrue(stringWriter.toString().contains("17"));
    assertTrue(stringWriter.toString().contains("5"));
    assertTrue(stringWriter.toString().contains("24"));
    assertTrue(stringWriter.toString().contains("4"));
  }

  @Test
  // Verify empty wait time
  public void emptyWaits() throws Exception {
    Date date1 = new Date(2020, 1, 1);

    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    datastore.put(init);

    // Create a test entity in Wait
    Entity waitEntity = new Entity("Wait");
    waitEntity.setProperty("classKey", init.getKey());

    ArrayList<Long> waitDurList = new ArrayList<Long>(Arrays.asList(10L, 3L, 6L, 1L));

    waitEntity.setProperty("waitDurations", waitDurList);
    waitEntity.setProperty("date", date1);

    datastore.put(waitEntity);
    datastore.delete(waitEntity.getKey());

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    when(httpResponse.getWriter()).thenReturn(writer);

    wait.doGet(httpRequest, httpResponse); // Servlet response

    assertTrue(stringWriter.toString().contains(":[]"));
  }

  @Test
  // Verify correct averages within multiple classes/dates
  public void multipleDatesMultipleClasses() throws Exception {
    Date date1 = new Date(2020, 1, 1);
    Date date2 = new Date(2020, 1, 2);
    Date date3 = new Date(2020, 1, 3);
    Date date4 = new Date(2020, 1, 4);
    Date date5 = new Date(2020, 1, 5);
    Date date6 = new Date(2020, 1, 6);

    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    Entity init2 = new Entity("Class");

    init2.setProperty("owner", "ownerID2");
    init2.setProperty("name", "testClass2");
    init2.setProperty("beingHelped", new EmbeddedEntity());
    init2.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    Entity init3 = new Entity("Class");

    init3.setProperty("owner", "ownerID3");
    init3.setProperty("name", "testClass3");
    init3.setProperty("beingHelped", new EmbeddedEntity());
    init3.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    datastore.put(init);
    datastore.put(init2);
    datastore.put(init3);

    ArrayList<Long> waitDurList1 = new ArrayList<Long>(Arrays.asList(10L, 3L, 6L, 1L));
    ArrayList<Long> waitDurList2 = new ArrayList<Long>(Arrays.asList(3L, 6L));
    ArrayList<Long> waitDurList3 = new ArrayList<Long>(Arrays.asList(45L, 17L, 11L));
    ArrayList<Long> waitDurList4 = new ArrayList<Long>(Arrays.asList(10L, 33L, 6L, 19L));
    ArrayList<Long> waitDurList5 = new ArrayList<Long>(Arrays.asList(33L));
    ArrayList<Long> waitDurList6 = new ArrayList<Long>(Arrays.asList(5L, 6L, 19L));

    //
    // Target class has 4 different wait dates
    //
    Entity waitEntity1 = new Entity("Wait");
    waitEntity1.setProperty("classKey", init.getKey());
    waitEntity1.setProperty("waitDurations", waitDurList1);
    waitEntity1.setProperty("date", date1);

    Entity waitEntity2 = new Entity("Wait");
    waitEntity2.setProperty("classKey", init.getKey());
    waitEntity2.setProperty("waitDurations", waitDurList2);
    waitEntity2.setProperty("date", date2);

    Entity waitEntity3 = new Entity("Wait");
    waitEntity3.setProperty("classKey", init.getKey());
    waitEntity3.setProperty("waitDurations", waitDurList3);
    waitEntity3.setProperty("date", date3);

    Entity waitEntity4 = new Entity("Wait");
    waitEntity4.setProperty("classKey", init.getKey());
    waitEntity4.setProperty("waitDurations", waitDurList4);
    waitEntity4.setProperty("date", date4);

    Entity waitEntity5 = new Entity("Wait");
    waitEntity5.setProperty("classKey", init2.getKey());
    waitEntity5.setProperty("waitDurations", waitDurList4);
    waitEntity5.setProperty("date", date4);

    Entity waitEntity6 = new Entity("Wait");
    waitEntity6.setProperty("classKey", init2.getKey());
    waitEntity6.setProperty("waitDurations", waitDurList5);
    waitEntity6.setProperty("date", date5);

    Entity waitEntity7 = new Entity("Wait");
    waitEntity7.setProperty("classKey", init3.getKey());
    waitEntity7.setProperty("waitDurations", waitDurList6);
    waitEntity7.setProperty("date", date6);

    datastore.put(waitEntity1);
    datastore.put(waitEntity2);
    datastore.put(waitEntity3);
    datastore.put(waitEntity4);
    datastore.put(waitEntity5);
    datastore.put(waitEntity6);
    datastore.put(waitEntity7);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    when(httpResponse.getWriter()).thenReturn(writer);

    wait.doGet(httpRequest, httpResponse); // Servlet response

    Gson gson = new Gson();

    assertTrue(stringWriter.toString().contains(gson.toJson(date1)));
    assertTrue(stringWriter.toString().contains(gson.toJson(date2)));
    assertTrue(stringWriter.toString().contains(gson.toJson(date3)));
    assertTrue(stringWriter.toString().contains(gson.toJson(date4)));
    assertTrue(stringWriter.toString().contains("17"));
    assertTrue(stringWriter.toString().contains("5"));
    assertTrue(stringWriter.toString().contains("24"));
    assertTrue(stringWriter.toString().contains("4"));
  }
}

package com.google.sps.servlets.course;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.google.sps.authentication.Authenticator;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
public class WaitTimeTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock Authenticator auth;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @InjectMocks WaitTime wait;

  private final String ID_TOKEN = "ID_TOKEN";

  private final Date date1 = new Date(2020, 1, 1);
  private final Date date2 = new Date(2020, 1, 2);
  private final Date date3 = new Date(2020, 1, 3);
  private final Date date4 = new Date(2020, 1, 4);
  private final Date date5 = new Date(2020, 1, 5);
  private final Date date6 = new Date(2020, 1, 6);

  private Entity init;
  private Entity init2;
  private Entity init3;
  private Entity init4;
  private Entity waitEntity1;
  private Entity waitEntity2;
  private Entity waitEntity3;
  private Entity waitEntity4;
  private Entity waitEntity5;
  private Entity waitEntity6;
  private Entity waitEntity7;

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
    init.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    init2 = new Entity("Class");

    init2.setProperty("owner", "ownerID2");
    init2.setProperty("name", "testClass2");
    init2.setProperty("beingHelped", new EmbeddedEntity());
    init2.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    init3 = new Entity("Class");

    init3.setProperty("owner", "ownerID3");
    init3.setProperty("name", "testClass3");
    init3.setProperty("beingHelped", new EmbeddedEntity());
    init3.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    init4 = new Entity("Class");

    init4.setProperty("owner", "ownerID4");
    init4.setProperty("name", "testClass4");
    init4.setProperty("beingHelped", new EmbeddedEntity());
    init4.setProperty("studentQueue", Arrays.asList("test1"));

    //
    // Create Wait Entities
    //
    ArrayList<Long> waitDurList1 = new ArrayList<Long>(Arrays.asList(10L, 3L, 6L, 1L));
    ArrayList<Long> waitDurList2 = new ArrayList<Long>(Arrays.asList(3L, 6L));
    ArrayList<Long> waitDurList3 = new ArrayList<Long>(Arrays.asList(45L, 17L, 11L));
    ArrayList<Long> waitDurList4 = new ArrayList<Long>(Arrays.asList(10L, 33L, 6L, 19L));
    ArrayList<Long> waitDurList5 = new ArrayList<Long>(Arrays.asList(33L));
    ArrayList<Long> waitDurList6 = new ArrayList<Long>(Arrays.asList(5L, 6L, 19L));

    waitEntity1 = new Entity("Wait");
    waitEntity1.setProperty("classKey", init.getKey());
    waitEntity1.setProperty("waitDurations", waitDurList1);
    waitEntity1.setProperty("date", date1);

    waitEntity2 = new Entity("Wait");
    waitEntity2.setProperty("classKey", init.getKey());
    waitEntity2.setProperty("waitDurations", waitDurList2);
    waitEntity2.setProperty("date", date2);

    waitEntity3 = new Entity("Wait");
    waitEntity3.setProperty("classKey", init.getKey());
    waitEntity3.setProperty("waitDurations", waitDurList3);
    waitEntity3.setProperty("date", date3);

    waitEntity4 = new Entity("Wait");
    waitEntity4.setProperty("classKey", init.getKey());
    waitEntity4.setProperty("waitDurations", waitDurList4);
    waitEntity4.setProperty("date", date4);

    waitEntity5 = new Entity("Wait");
    waitEntity5.setProperty("classKey", init2.getKey());
    waitEntity5.setProperty("waitDurations", waitDurList4);
    waitEntity5.setProperty("date", date4);

    waitEntity6 = new Entity("Wait");
    waitEntity6.setProperty("classKey", init2.getKey());
    waitEntity6.setProperty("waitDurations", waitDurList5);
    waitEntity6.setProperty("date", date5);

    waitEntity7 = new Entity("Wait");
    waitEntity7.setProperty("classKey", init3.getKey());
    waitEntity7.setProperty("waitDurations", waitDurList6);
    waitEntity7.setProperty("date", date6);
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  // Get average wait time for a single class, single day
  public void basic() throws Exception {

    ArrayList<Date> listOfDates = new ArrayList<Date>();
    ArrayList<ArrayList<Long>> averagesList = new ArrayList<ArrayList<Long>>();

    datastore.put(init);
    datastore.put(waitEntity1);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    Filter classFilter = new FilterPredicate("classKey", FilterOperator.EQUAL, init.getKey());

    // Obtain waits from datastore and filter them into results query
    Query query =
        new Query("Wait").addSort("date", SortDirection.DESCENDING).setFilter(classFilter);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Store the date and time average into two separate lists
    for (Entity entity : results.asIterable()) {
      Date date = (Date) entity.getProperty("date");
      ArrayList<Long> durationList = (ArrayList<Long>) entity.getProperty("waitDurations");

      listOfDates.add(date);
      averagesList.add(durationList);
    }

    // Verify content of lists
    assertEquals(date1, (Date) listOfDates.get(0));
    assertEquals((List<Long>) Arrays.asList(10L, 3L, 6L, 1L), (List<Long>) averagesList.get(0));
    assertTrue(listOfDates.size() == 1);
    assertTrue(averagesList.size() == 1);

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

    ArrayList<Date> listOfDates = new ArrayList<Date>();
    ArrayList<ArrayList<Long>> averagesList = new ArrayList<ArrayList<Long>>();

    datastore.put(init);
    datastore.put(waitEntity1);
    datastore.put(waitEntity2);
    datastore.put(waitEntity3);
    datastore.put(waitEntity4);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    Filter classFilter = new FilterPredicate("classKey", FilterOperator.EQUAL, init.getKey());

    Query query =
        new Query("Wait").addSort("date", SortDirection.DESCENDING).setFilter(classFilter);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    for (Entity entity : results.asIterable()) {
      Date date = (Date) entity.getProperty("date");
      ArrayList<Long> durationList = (ArrayList<Long>) entity.getProperty("waitDurations");

      listOfDates.add(date);
      averagesList.add(durationList);
    }

    // Date/time pairs should be from most recent
    assertEquals(date4, (Date) listOfDates.get(0));
    assertEquals(date3, (Date) listOfDates.get(1));
    assertEquals(date2, (Date) listOfDates.get(2));
    assertEquals(date1, (Date) listOfDates.get(3));
    assertEquals((List<Long>) Arrays.asList(10L, 33L, 6L, 19L), (List<Long>) averagesList.get(0));
    assertEquals((List<Long>) Arrays.asList(45L, 17L, 11L), (List<Long>) averagesList.get(1));
    assertEquals((List<Long>) Arrays.asList(3L, 6L), (List<Long>) averagesList.get(2));
    assertEquals((List<Long>) Arrays.asList(10L, 3L, 6L, 1L), (List<Long>) averagesList.get(3));
    assertTrue(listOfDates.size() == 4);
    assertTrue(averagesList.size() == 4);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    when(httpResponse.getWriter()).thenReturn(writer);

    wait.doGet(httpRequest, httpResponse);

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

    datastore.put(init);
    datastore.put(waitEntity1);
    datastore.delete(waitEntity1.getKey());

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    when(httpResponse.getWriter()).thenReturn(writer);

    wait.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains(":[]"));
  }

  @Test
  // Verify empty wait time for a class that has no time averages
  public void noWaits() throws Exception {

    datastore.put(init);
    datastore.put(init4);
    datastore.put(waitEntity1);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init4.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init4.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    when(httpResponse.getWriter()).thenReturn(writer);

    wait.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains(":[]"));
  }

  @Test
  // Verify correct averages within multiple classes/dates
  public void multipleDatesMultipleClasses() throws Exception {

    ArrayList<Date> listOfDates = new ArrayList<Date>();
    ArrayList<ArrayList<Long>> averagesList = new ArrayList<ArrayList<Long>>();

    datastore.put(init);
    datastore.put(init2);
    datastore.put(init3);
    datastore.put(waitEntity1);
    datastore.put(waitEntity2);
    datastore.put(waitEntity3);
    datastore.put(waitEntity4);
    datastore.put(waitEntity5);
    datastore.put(waitEntity6);
    datastore.put(waitEntity7);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    Filter classFilter = new FilterPredicate("classKey", FilterOperator.EQUAL, init.getKey());

    Query query =
        new Query("Wait").addSort("date", SortDirection.DESCENDING).setFilter(classFilter);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    for (Entity entity : results.asIterable()) {
      Date date = (Date) entity.getProperty("date");
      ArrayList<Long> durationList = (ArrayList<Long>) entity.getProperty("waitDurations");

      listOfDates.add(date);
      averagesList.add(durationList);
    }

    assertEquals(date4, (Date) listOfDates.get(0));
    assertEquals(date3, (Date) listOfDates.get(1));
    assertEquals(date2, (Date) listOfDates.get(2));
    assertEquals(date1, (Date) listOfDates.get(3));
    assertEquals((List<Long>) Arrays.asList(10L, 33L, 6L, 19L), (List<Long>) averagesList.get(0));
    assertEquals((List<Long>) Arrays.asList(45L, 17L, 11L), (List<Long>) averagesList.get(1));
    assertEquals((List<Long>) Arrays.asList(3L, 6L), (List<Long>) averagesList.get(2));
    assertEquals((List<Long>) Arrays.asList(10L, 3L, 6L, 1L), (List<Long>) averagesList.get(3));
    assertTrue(listOfDates.size() == 4);
    assertTrue(averagesList.size() == 4);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    when(httpResponse.getWriter()).thenReturn(writer);

    wait.doGet(httpRequest, httpResponse);

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

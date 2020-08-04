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
public class VisitByDateTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock Authenticator auth;

  @Mock HttpServletResponse httpResponse;

  @InjectMocks VisitsByDate checkVisits;

  private final String ID_TOKEN = "ID_TOKEN";

  private final Date date1 = new Date(2020, 1, 1);
  private final Date date2 = new Date(2020, 1, 5);
  private final Date date3 = new Date(2020, 5, 1);
  private final Date date4 = new Date(2020, 6, 6);
  private final Date date5 = new Date(2020, 5, 1);
  private final Date date6 = new Date(2020, 5, 5);
  private final Date date7 = new Date(2020, 7, 12);
  private final Date date8 = new Date(2020, 5, 10);

  private Entity init;
  private Entity init2;
  private Entity init3;
  private Entity init4;
  private Entity init5;
  private Entity visitEntity1;
  private Entity visitEntity2;
  private Entity visitEntity3;
  private Entity visitEntity4;
  private Entity visitEntity5;
  private Entity visitEntity6;
  private Entity visitEntity7;
  private Entity visitEntity8;

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();

    //
    // Create classes
    //
    init = new Entity("Class");

    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    init2 = new Entity("Class");

    init2.setProperty("name", "testClass2");
    init2.setProperty("beingHelped", new EmbeddedEntity());
    init2.setProperty("studentQueue", Collections.emptyList());

    init3 = new Entity("Class");

    init3.setProperty("name", "testClass3");
    init3.setProperty("beingHelped", new EmbeddedEntity());
    init3.setProperty("studentQueue", Collections.emptyList());

    init4 = new Entity("Class");

    init4.setProperty("name", "testClass4");
    init4.setProperty("beingHelped", new EmbeddedEntity());
    init4.setProperty("studentQueue", Collections.emptyList());

    init5 = new Entity("Class");

    init5.setProperty("name", "testClass5");
    init5.setProperty("beingHelped", new EmbeddedEntity());
    init5.setProperty("studentQueue", Collections.emptyList());

    //
    // Create Visit Entities
    //
    visitEntity1 = new Entity("Visit");
    visitEntity1.setProperty("classKey", init.getKey());
    visitEntity1.setProperty("numVisits", 3);
    visitEntity1.setProperty("date", date1);

    visitEntity2 = new Entity("Visit");
    visitEntity2.setProperty("classKey", init.getKey());
    visitEntity2.setProperty("numVisits", 7);
    visitEntity2.setProperty("date", date2);

    visitEntity3 = new Entity("Visit");
    visitEntity3.setProperty("classKey", init2.getKey());
    visitEntity3.setProperty("numVisits", 20);
    visitEntity3.setProperty("date", date3);

    visitEntity4 = new Entity("Visit");
    visitEntity4.setProperty("classKey", init.getKey());
    visitEntity4.setProperty("numVisits", 10);
    visitEntity4.setProperty("date", date4);

    visitEntity5 = new Entity("Visit");
    visitEntity5.setProperty("classKey", init2.getKey());
    visitEntity5.setProperty("numVisits", 20);
    visitEntity5.setProperty("date", date5);

    visitEntity6 = new Entity("Visit");
    visitEntity6.setProperty("classKey", init3.getKey());
    visitEntity6.setProperty("numVisits", 30);
    visitEntity6.setProperty("date", date6);

    visitEntity7 = new Entity("Visit");
    visitEntity7.setProperty("classKey", init3.getKey());
    visitEntity7.setProperty("numVisits", 40);
    visitEntity7.setProperty("date", date7);

    visitEntity8 = new Entity("Visit");
    visitEntity8.setProperty("classKey", init4.getKey());
    visitEntity8.setProperty("numVisits", 1);
    visitEntity8.setProperty("date", date8);
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  // With a single class in the Visit entity, there should only be one date populated in the lists
  public void oneClassOneDate() throws Exception {

    ArrayList<Date> listOfDates = new ArrayList<Date>();
    ArrayList<Long> visitsPerClass = new ArrayList<Long>();

    datastore.put(init);
    datastore.put(visitEntity1);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    // Obtain visits from datastore
    Query query = new Query("Visit");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Store the class name and number of visits into separate lists
    for (Entity entity : results.asIterable()) {
      Date date = (Date) entity.getProperty("date");
      long classVisits = (long) entity.getProperty("numVisits");

      listOfDates.add(date);
      visitsPerClass.add(classVisits);
    }

    // Verify list order
    assertEquals(date1, (Date) listOfDates.get(0));
    assertEquals((long) 3, (long) visitsPerClass.get(0));
    assertTrue(listOfDates.size() == 1);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    when(httpResponse.getWriter()).thenReturn(writer);

    checkVisits.doGet(httpRequest, httpResponse); // Servlet response

    Gson gson = new Gson();

    assertTrue(stringWriter.toString().contains(gson.toJson(date1)));
    assertTrue(stringWriter.toString().contains("3"));
  }

  @Test
  // Verify that only one target class's data is being retrieved
  public void twoClasses() throws Exception {

    ArrayList<Date> listOfDates = new ArrayList<Date>();
    ArrayList<Long> visitsPerClass = new ArrayList<Long>();

    datastore.put(init);
    datastore.put(init2);
    datastore.put(visitEntity1);
    datastore.put(visitEntity2);
    datastore.put(visitEntity3);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    checkVisits.doGet(httpRequest, httpResponse);

    Gson gson = new Gson();

    assertTrue(stringWriter.toString().contains(gson.toJson(date1)));
    assertTrue(stringWriter.toString().contains(gson.toJson(date2)));
    assertTrue(stringWriter.toString().contains("3"));
    assertTrue(stringWriter.toString().contains("7"));
  }

  @Test
  // Filter one class's visit data from multiple entities
  public void multipleClasses() throws Exception {

    ArrayList<Date> listOfDates = new ArrayList<Date>();
    ArrayList<Long> visitsPerClass = new ArrayList<Long>();

    datastore.put(init);
    datastore.put(init2);
    datastore.put(init3);
    datastore.put(init4);
    datastore.put(init5);
    datastore.put(visitEntity1);
    datastore.put(visitEntity2);
    datastore.put(visitEntity3);
    datastore.put(visitEntity4);
    datastore.put(visitEntity5);
    datastore.put(visitEntity6);
    datastore.put(visitEntity7);
    datastore.put(visitEntity8);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    Filter classFilter = new FilterPredicate("classKey", FilterOperator.EQUAL, init.getKey());

    Query query =
        new Query("Visit").addSort("date", SortDirection.DESCENDING).setFilter(classFilter);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    for (Entity entity : results.asIterable()) {
      Date date = (Date) entity.getProperty("date");
      long classVisits = (long) entity.getProperty("numVisits");

      listOfDates.add(date);
      visitsPerClass.add(classVisits);
    }

    assertEquals(date4, (Date) listOfDates.get(0));
    assertEquals((long) 10, (long) visitsPerClass.get(0));
    assertEquals(date2, (Date) listOfDates.get(1));
    assertEquals((long) 7, (long) visitsPerClass.get(1));
    assertEquals(date1, (Date) listOfDates.get(2));
    assertEquals((long) 3, (long) visitsPerClass.get(2));
    assertTrue(visitsPerClass.size() == 3);
    assertTrue(listOfDates.size() == 3);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    checkVisits.doGet(httpRequest, httpResponse);

    Gson gson = new Gson();

    assertTrue(stringWriter.toString().contains(gson.toJson(date1)));
    assertTrue(stringWriter.toString().contains(gson.toJson(date2)));
    assertTrue(stringWriter.toString().contains(gson.toJson(date4)));
    assertTrue(stringWriter.toString().contains("3"));
    assertTrue(stringWriter.toString().contains("7"));
    assertTrue(stringWriter.toString().contains("10"));
  }

  @Test
  // With no visits, the response should be empty
  public void emptyVisits() throws Exception {
    ArrayList<Date> listOfDates = new ArrayList<Date>();
    ArrayList<Long> visitsPerClass = new ArrayList<Long>();

    datastore.put(init);
    datastore.put(visitEntity1);
    datastore.delete(visitEntity1.getKey());

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    when(httpRequest.getParameter("idToken")).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(ID_TOKEN, KeyFactory.keyToString(init.getKey()))).thenReturn(true);

    Query query = new Query("Visit");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    for (Entity entity : results.asIterable()) {
      Date date = (Date) entity.getProperty("date");
      long classVisits = (long) entity.getProperty("numVisits");

      listOfDates.add(date);
      visitsPerClass.add(classVisits);
    }

    // Verify empty lists
    assertTrue(listOfDates.size() == 0);
    assertTrue(visitsPerClass.size() == 0);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    when(httpResponse.getWriter()).thenReturn(writer);

    checkVisits.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains(":[]"));
  }
}

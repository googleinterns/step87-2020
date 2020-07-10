package com.google.sps.servlets;

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

  @Mock HttpServletResponse httpResponse;

  @InjectMocks VisitsByDate checkVisits;

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
  // With a single class in the Visit entity, there should only be one date (and
  // corresponding number of visits) populated in the lists.
  public void oneClassOneDate() throws Exception {
    ArrayList<Date> listOfDates = new ArrayList<Date>();
    ArrayList<Long> visitsPerClass = new ArrayList<Long>();

    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("taList", Collections.emptyList());
    init.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    datastore.put(init);

    // Create a test entity in Visits
    Entity visitEntity = new Entity("Visit");
    visitEntity.setProperty("classKey", init.getKey());
    visitEntity.setProperty("numVisits", 3);
    visitEntity.setProperty("date", new Date(2020, 1, 1));

    datastore.put(visitEntity);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    // Obtain visits from datastore and filter them into results query
    Query query = new Query("Visit");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Store the class name and number of visits into two separate lists
    for (Entity entity : results.asIterable()) {
      Date date = (Date) entity.getProperty("date");
      long classVisits = (long) entity.getProperty("numVisits");

      listOfDates.add(date);
      visitsPerClass.add(classVisits);
    }

    assertEquals((Date) new Date(2020, 1, 1), (Date) listOfDates.get(0));
    assertEquals((long) 3, (long) visitsPerClass.get(0));
    assertTrue(listOfDates.size() == 1);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    checkVisits.doGet(httpRequest, httpResponse);
    System.out.println(stringWriter.toString());
    assertTrue(stringWriter.toString().contains("Feb 1, 3920, 12:00:00 AM"));
    assertTrue(stringWriter.toString().contains("3"));
  }

  @Test
  // Filter one class visit data from two class entities
  public void twoClasses() throws Exception {
    ArrayList<Date> listOfDates = new ArrayList<Date>();
    ArrayList<Long> visitsPerClass = new ArrayList<Long>();

    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("taList", Collections.emptyList());
    init.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    Entity init2 = new Entity("Class2");

    init2.setProperty("owner", "ownerID");
    init2.setProperty("name", "testClass2");
    init2.setProperty("beingHelped", new EmbeddedEntity());
    init2.setProperty("taList", Collections.emptyList());
    init2.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);
    datastore.put(init2);

    // Class 1 with 15 visits on 1/1/2020
    Entity visitEntity = new Entity("Visit");
    visitEntity.setProperty("classKey", init.getKey());
    visitEntity.setProperty("numVisits", 15);
    visitEntity.setProperty("date", new Date(2020, 2, 1));

    // Class 1 with 7 visits on 1/5/2020
    Entity visitEntity2 = new Entity("Visit");
    visitEntity2.setProperty("classKey", init.getKey());
    visitEntity2.setProperty("numVisits", 7);
    visitEntity2.setProperty("date", new Date(2020, 1, 5));

    // Class 2 with 20 visits on 5/1/2020
    Entity visitEntity3 = new Entity("Visit");
    visitEntity3.setProperty("classKey", init2.getKey());
    visitEntity3.setProperty("numVisits", 20);
    visitEntity3.setProperty("date", new Date(2020, 5, 1));

    datastore.put(visitEntity);
    datastore.put(visitEntity2);
    datastore.put(visitEntity3);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    Filter classFilter = new FilterPredicate("classKey", FilterOperator.EQUAL, init.getKey());

    // Obtain visits from datastore and filter them into results query
    Query query =
        new Query("Visit").addSort("date", SortDirection.ASCENDING).setFilter(classFilter);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Store the class name and number of visits into two separate lists
    for (Entity entity : results.asIterable()) {
      Date date = (Date) entity.getProperty("date");
      long classVisits = (long) entity.getProperty("numVisits");

      listOfDates.add(date);
      visitsPerClass.add(classVisits);
    }

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    checkVisits.doGet(httpRequest, httpResponse);

    assertTrue(stringWriter.toString().contains("Feb 5, 3920, 12:00:00 AM"));
    assertTrue(stringWriter.toString().contains("Mar 1, 3920, 12:00:00 AM"));
    assertTrue(stringWriter.toString().contains("15"));
    assertTrue(stringWriter.toString().contains("7"));
  }

  @Test
  // Filter one class visit data from multiple entities
  public void multipleClasses() throws Exception {
    ArrayList<Date> listOfDates = new ArrayList<Date>();
    ArrayList<Long> visitsPerClass = new ArrayList<Long>();

    //
    // Create 4 different classes
    //
    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("taList", Collections.emptyList());
    init.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));

    Entity init2 = new Entity("Class2");

    init2.setProperty("owner", "ownerID2");
    init2.setProperty("name", "testClass2");
    init2.setProperty("beingHelped", new EmbeddedEntity());
    init2.setProperty("taList", Collections.emptyList());
    init2.setProperty("studentQueue", Collections.emptyList());

    Entity init3 = new Entity("Class3");

    init3.setProperty("owner", "ownerID3");
    init3.setProperty("name", "testClass3");
    init3.setProperty("beingHelped", new EmbeddedEntity());
    init3.setProperty("taList", Collections.emptyList());
    init3.setProperty("studentQueue", Collections.emptyList());

    Entity init4 = new Entity("Class4");

    init4.setProperty("owner", "ownerID4");
    init4.setProperty("name", "testClass4");
    init4.setProperty("beingHelped", new EmbeddedEntity());
    init4.setProperty("taList", Collections.emptyList());
    init4.setProperty("studentQueue", Collections.emptyList());

    Entity init5 = new Entity("Class5");

    init5.setProperty("owner", "ownerID5");
    init5.setProperty("name", "testClass5");
    init5.setProperty("beingHelped", new EmbeddedEntity());
    init5.setProperty("taList", Collections.emptyList());
    init5.setProperty("studentQueue", Collections.emptyList());

    datastore.put(init);
    datastore.put(init2);
    datastore.put(init3);
    datastore.put(init4);
    datastore.put(init5);

    // Target class with 24 visits on 6/10/2020
    Entity visitEntity = new Entity("Visit");
    visitEntity.setProperty("classKey", init.getKey());
    visitEntity.setProperty("numVisits", 24);
    visitEntity.setProperty("date", new Date(2020, 5, 10));

    // Target class with 17 visits on 12/5/2020
    Entity visitEntity2 = new Entity("Visit");
    visitEntity2.setProperty("classKey", init.getKey());
    visitEntity2.setProperty("numVisits", 17);
    visitEntity2.setProperty("date", new Date(2020, 11, 5));

    // Target class with 4 visits on 10/3/2020
    Entity visitEntity3 = new Entity("Visit");
    visitEntity3.setProperty("classKey", init.getKey());
    visitEntity3.setProperty("numVisits", 4);
    visitEntity3.setProperty("date", new Date(2020, 9, 3));

    // Target class with 10 visits on 7/6/2020
    Entity visitEntity4 = new Entity("Visit");
    visitEntity4.setProperty("classKey", init.getKey());
    visitEntity4.setProperty("numVisits", 10);
    visitEntity4.setProperty("date", new Date(2020, 6, 6));

    // Class 2 with 20 visits on 6/1/2020
    Entity visitEntity5 = new Entity("Visit");
    visitEntity5.setProperty("classKey", init2.getKey());
    visitEntity5.setProperty("numVisits", 20);
    visitEntity5.setProperty("date", new Date(2020, 5, 1));

    // Class 3 with 30 visits on 6/5/2020
    Entity visitEntity6 = new Entity("Visit");
    visitEntity6.setProperty("classKey", init3.getKey());
    visitEntity6.setProperty("numVisits", 30);
    visitEntity6.setProperty("date", new Date(2020, 5, 5));

    // Class 3 with 40 visits on 8/12/2020
    Entity visitEntity7 = new Entity("Visit");
    visitEntity7.setProperty("classKey", init3.getKey());
    visitEntity7.setProperty("numVisits", 40);
    visitEntity7.setProperty("date", new Date(2020, 7, 12));

    // Class 4 with 1 visit on 2/1/2020
    Entity visitEntity8 = new Entity("Visit");
    visitEntity8.setProperty("classKey", init4.getKey());
    visitEntity8.setProperty("numVisits", 1);
    visitEntity8.setProperty("date", new Date(2020, 1, 1));

    datastore.put(visitEntity);
    datastore.put(visitEntity2);
    datastore.put(visitEntity3);
    datastore.put(visitEntity4);
    datastore.put(visitEntity5);
    datastore.put(visitEntity6);
    datastore.put(visitEntity7);
    datastore.put(visitEntity8);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    Filter classFilter = new FilterPredicate("classKey", FilterOperator.EQUAL, init.getKey());

    // Obtain visits from datastore and filter them into results query
    Query query =
        new Query("Visit").addSort("date", SortDirection.DESCENDING).setFilter(classFilter);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Store the class name and number of visits into two separate lists
    for (Entity entity : results.asIterable()) {
      Date date = (Date) entity.getProperty("date");
      long classVisits = (long) entity.getProperty("numVisits");

      listOfDates.add(date);
      visitsPerClass.add(classVisits);
    }

    assertEquals((Date) new Date(2020, 11, 5), (Date) listOfDates.get(0));
    assertEquals((long) 17, (long) visitsPerClass.get(0));
    assertEquals((Date) new Date(2020, 9, 3), (Date) listOfDates.get(1));
    assertEquals((long) 4, (long) visitsPerClass.get(1));
    assertEquals((Date) new Date(2020, 6, 6), (Date) listOfDates.get(2));
    assertEquals((long) 10, (long) visitsPerClass.get(2));
    assertEquals((Date) new Date(2020, 5, 10), (Date) listOfDates.get(3));
    assertEquals((long) 24, (long) visitsPerClass.get(3));
    assertTrue(visitsPerClass.size() == 4);
    assertTrue(listOfDates.size() == 4);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    checkVisits.doGet(httpRequest, httpResponse); // Fetch servlet response

    assertTrue(stringWriter.toString().contains("Jun 10, 3920, 12:00:00 AM"));
    assertTrue(stringWriter.toString().contains("Dec 5, 3920, 12:00:00 AM"));
    assertTrue(stringWriter.toString().contains("Oct 3, 3920, 12:00:00 AM"));
    assertTrue(stringWriter.toString().contains("Jul 6, 3920, 12:00:00 AM"));
    assertTrue(stringWriter.toString().contains("24"));
    assertTrue(stringWriter.toString().contains("17"));
    assertTrue(stringWriter.toString().contains("4"));
    assertTrue(stringWriter.toString().contains("10"));
  }
}

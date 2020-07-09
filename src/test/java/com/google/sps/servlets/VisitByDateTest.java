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

    System.out.println(" ----- ");
    System.out.println("The date is: " + (Date) listOfDates.get(0));
    System.out.println(" ----- ");

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

  // --------------------------------------------------------------------------------------------
  @Test
  //
  public void multipleClasses() throws Exception {
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

    System.out.println(" ----- ");
    System.out.println("The date is: " + (Date) listOfDates.get(0));
    System.out.println(" ----- ");
    System.out.println("The date is: " + (Date) listOfDates.get(1));

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    checkVisits.doGet(httpRequest, httpResponse);

    System.out.println(stringWriter.toString());
    assertTrue(stringWriter.toString().contains("Feb 5, 3920, 12:00:00 AM"));
    assertTrue(stringWriter.toString().contains("Mar 1, 3920, 12:00:00 AM"));
    assertTrue(stringWriter.toString().contains("15"));
    assertTrue(stringWriter.toString().contains("7"));
  }
}

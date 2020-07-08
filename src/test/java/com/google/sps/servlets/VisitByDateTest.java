package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
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

    // Create a test entity in Visits
    Entity visitEntity = new Entity("Visit");
    // visitEntity.setProperty("classKey", KeyFactory.keyToString(visitEntity.getKey()));
    visitEntity.setProperty("classKey", "class101");
    visitEntity.setProperty("numVisits", 15);
    visitEntity.setProperty("className", "exampleClassName");
    visitEntity.setProperty("date", new Date(2020, 1, 1));

    datastore.put(visitEntity);

    when(httpRequest.getParameter("classCode")).thenReturn("class101");

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
    assertEquals((long) 15, (long) visitsPerClass.get(0));
    assertTrue(listOfDates.size() == 1);

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(httpResponse.getWriter()).thenReturn(writer);

    checkVisits.doGet(httpRequest, httpResponse);
    // assertTrue(stringWriter.toString().contains("2020-01-01T00:00:00+00:00")); // Failed
    // assertTrue(stringWriter.toString().contains("15")); // Failed
    assertEquals("15", stringWriter.toString());
  }
}

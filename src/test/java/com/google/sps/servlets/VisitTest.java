package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VisitTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

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
  // 1 Class
  public void checkVisits() throws Exception {
    ArrayList<String> listOfClassNames = new ArrayList<String>();
    ArrayList<Long> visitsPerClass = new ArrayList<Long>();

    Entity visitEntity = new Entity("Visit");
    visitEntity.setProperty("classKey", "testClass101");
    visitEntity.setProperty("numVisits", 15);
    visitEntity.setProperty("className", "exampleClassName");

    datastore.put(visitEntity);

    // Obtain visits from datastore and filter them into results query
    Query query = new Query("Visit");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Store the class name and number of visits into two separate lists
    for (Entity entity : results.asIterable()) {
      String className = (String) entity.getProperty("className");
      long classVisits = (long) entity.getProperty("numVisits");

      listOfClassNames.add(className);
      visitsPerClass.add(classVisits);
    }

    assertEquals((String) "exampleClassName", (String) listOfClassNames.get(0));
    assertEquals((long) 15, (long) visitsPerClass.get(0));
  }

  @Test
  public void checkVisitsForMultipleClasses() throws Exception {
    ArrayList<String> listOfClassNames = new ArrayList<String>();
    ArrayList<Long> visitsPerClass = new ArrayList<Long>();

    Entity visitEntity = new Entity("Visit");
    visitEntity.setProperty("classKey", "testClass101");
    visitEntity.setProperty("numVisits", 15);
    visitEntity.setProperty("className", "exampleClassName");

    Entity visitEntity2 = new Entity("Visit");
    visitEntity2.setProperty("classKey", "testClass103");
    visitEntity2.setProperty("numVisits", 34);
    visitEntity2.setProperty("className", "exampleClass2");

    datastore.put(visitEntity);
    datastore.put(visitEntity2);

    // Obtain visits from datastore and filter them into results query
    Query query = new Query("Visit");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // Store the class name and number of visits into two separate lists
    for (Entity entity : results.asIterable()) {
      String className = (String) entity.getProperty("className");
      long classVisits = (long) entity.getProperty("numVisits");

      listOfClassNames.add(className);
      visitsPerClass.add(classVisits);
    }

    assertEquals((String) "exampleClassName", (String) listOfClassNames.get(0));
    assertEquals((long) 15, (long) visitsPerClass.get(0));
    assertEquals((String) "exampleClass2", (String) listOfClassNames.get(1));
    assertEquals((long) 34, (long) visitsPerClass.get(1));
  }
}

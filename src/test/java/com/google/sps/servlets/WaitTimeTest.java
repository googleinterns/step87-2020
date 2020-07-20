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
    init.setProperty("taList", Collections.emptyList());
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
  // Single class, multiples dates  
  public void multipleDatesOneClass() throws Exception {    
    Date date1 = new Date(2020, 1, 1);    
    Date date2 = new Date(2020, 1, 2);    
    Date date3 = new Date(2020, 1, 3);    
    Date date4 = new Date(2020, 1, 4);
    
    Entity init = new Entity("Class");
    init.setProperty("owner", "ownerID");    
    init.setProperty("name", "testClass");    
    init.setProperty("beingHelped", new EmbeddedEntity());    
    init.setProperty("taList", Collections.emptyList());    
    init.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));
    
    datastore.put(init);
    
    ArrayList<Long> waitDurList1 = new ArrayList<Long>(Arrays.asList(10L, 3L, 6L, 1L));    
    ArrayList<Long> waitDurList2 = new ArrayList<Long>(Arrays.asList(3L, 6L));    
    ArrayList<Long> waitDurList3 = new ArrayList<Long>(Arrays.asList(45L, 17L, 11L));    
    ArrayList<Long> waitDurList4 = new ArrayList<Long>(Arrays.asList(10L, 33L, 6L, 19L));

    // 
    // Target class has 4 different wait dates
    //
    Entity waitEntity1 = new Entity("Wait");    
    waitEntity1.setProperty("classKey", init.getKey());    
    waitEntity1.setProperty("waitDurations", waitDurList1);    
    waitEntity1.setProperty("date", date1);

    Entity waitEntity2 = new Entity("Wait");    
    waitEntity2.setProperty("classKey", init.getKey());    
    waitEntity2.setProperty("waitDurations", waitDurList2);    
    waitEntity2.setProperty("date", date2);

    Entity waitEntity3 = new Entity("Wait");    
    waitEntity3.setProperty("classKey", init.getKey());    
    waitEntity3.setProperty("waitDurations", waitDurList3);    
    waitEntity3.setProperty("date", date3);

    Entity waitEntity4 = new Entity("Wait");    
    waitEntity4.setProperty("classKey", init.getKey());   
    waitEntity4.setProperty("waitDurations", waitDurList4);    
    waitEntity4.setProperty("date", date4);

    datastore.put(waitEntity1);    
    datastore.put(waitEntity2);    
    datastore.put(waitEntity3);    
    datastore.put(waitEntity4);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    StringWriter stringWriter = new StringWriter();    
    PrintWriter writer = new PrintWriter(stringWriter);
    
    when(httpResponse.getWriter()).thenReturn(writer);
    
    wait.doGet(httpRequest, httpResponse); // Servlet response
    
    Gson gson = new Gson();

    // Verify all dates have the correct time averages
    assertTrue(stringWriter.toString().contains(gson.toJson(date1)));    
    assertTrue(stringWriter.toString().contains(gson.toJson(date1)));    
    assertTrue(stringWriter.toString().contains(gson.toJson(date1)));    
    assertTrue(stringWriter.toString().contains(gson.toJson(date1)));    
    assertTrue(stringWriter.toString().contains("17"));    
    assertTrue(stringWriter.toString().contains("5"));    
    assertTrue(stringWriter.toString().contains("24"));    
    assertTrue(stringWriter.toString().contains("4"));  
  }

  @Test  
  // Class is deleted, verify empty wait time  
  public void empty() throws Exception {    
    Date date1 = new Date(2020, 1, 1);

    Entity init = new Entity("Class");
    init.setProperty("owner", "ownerID");    
    init.setProperty("name", "testClass");    
    init.setProperty("beingHelped", new EmbeddedEntity());    
    init.setProperty("taList", Collections.emptyList());    
    init.setProperty("studentQueue", Arrays.asList("test1", "test2", "test3"));
    
    datastore.put(init);
    
    // Create a test entity in Wait    
    Entity waitEntity = new Entity("Wait");    
    waitEntity.setProperty("classKey", init.getKey());
    ArrayList<Long> waitDurList = new ArrayList<Long>(Arrays.asList(10L, 3L, 6L, 1L));
    waitEntity.setProperty("waitDurations", waitDurList);    
    waitEntity.setProperty("date", date1);
    
    datastore.put(waitEntity);   
    datastore.delete(waitEntity.getKey());
    
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));
    
    StringWriter stringWriter = new StringWriter();    
    PrintWriter writer = new PrintWriter(stringWriter);
    
    when(httpResponse.getWriter()).thenReturn(writer);
    
    wait.doGet(httpRequest, httpResponse); // Servlet response

    assertTrue(stringWriter.toString().contains(":[]"));  
  }
}

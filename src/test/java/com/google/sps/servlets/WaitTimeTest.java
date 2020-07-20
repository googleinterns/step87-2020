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
}

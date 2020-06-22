package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnterClassTest {

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
  public void addFirstToQueue() throws Exception {
    Entity init = new Entity("Class");
    List<String> emptyQueue = Collections.emptyList();

    init.setProperty("owner", "");
    init.setProperty("name", "");
    init.setProperty("beingHelped", "");
    init.setProperty("studentQueue", emptyQueue);

    datastore.put(init);

    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    EnterQueue addFirst = new EnterQueue();
    addFirst.doPost(httpRequest, httpResponse);

    Entity testEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    List<String> testQueue = (List<String>) testEntity.getProperty("studentQueue");
    assertEquals(
        KeyFactory.keyToString(init.getKey()), KeyFactory.keyToString(testEntity.getKey()));
    assertEquals(1, testQueue.size());
    assertEquals("student", testQueue.get(0));

    verify(httpRequest, times(1)).getParameter("classCode");
  }

  @Test
  public void addNewClass() throws Exception {
    when(httpRequest.getParameter("className")).thenReturn("testClass");

    NewClass addNew = new NewClass();
    addNew.doPost(httpRequest, httpResponse);

    Entity testEntity = datastore.prepare(new Query("Class")).asSingleEntity();

    assertEquals(testEntity.getProperty("owner"), "");
    assertEquals(testEntity.getProperty("name"), "testClass");
    assertEquals(testEntity.getProperty("beingHelped"), "");

    List<String> testQueue = (List<String>) testEntity.getProperty("studentQueue");
    assertTrue(testQueue.isEmpty());

    verify(httpRequest, times(1)).getParameter("className");
  }
}

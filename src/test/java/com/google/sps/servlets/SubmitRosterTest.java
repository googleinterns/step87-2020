package com.google.sps.servlets;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.Collections;
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
public class SubmitRosterTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest httpRequest;

  @Mock HttpServletResponse httpResponse;

  @Mock FirebaseAuth authInstance;

  @InjectMocks SubmitRoster submitRoster;

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
  public void addRoster() throws Exception {

    Entity init = new Entity("Class");

    init.setProperty("owner", "ownerID");
    init.setProperty("name", "testClass");
    init.setProperty("beingHelped", new EmbeddedEntity());
    init.setProperty("studentQueue", Collections.emptyList());
    init.setProperty("taList", Collections.emptyList());

    datastore.put(init);

    when(httpRequest.getParameter("roster")).thenReturn("first@google.com, second@google.com");
    when(httpRequest.getParameter("classCode")).thenReturn(KeyFactory.keyToString(init.getKey()));

    submitRoster.doPost(httpRequest, httpResponse);

    Query query = new Query("User");
    PreparedQuery results = datastore.prepare(query);

    for (Entity user : results.asIterable()) {
      if (user.getProperty("userEmail") == "first@google.com") {
        ArrayList<Key> testRegistered = (ArrayList<Key>) user.getProperty("registeredClasses");
        assertTrue(testRegistered.contains("exampleClassCode"));
      } else if (user.getProperty("userEmail") == "second@google.com") {
        ArrayList<Key> testRegistered = (ArrayList<Key>) user.getProperty("registeredClasses");
        assertTrue(testRegistered.contains("exampleClassCode"));
      }
    }
  }
}

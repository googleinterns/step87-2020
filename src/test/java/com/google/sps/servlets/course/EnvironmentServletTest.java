package com.google.sps.servlets.course;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.google.sps.authentication.Authenticator;
import com.google.sps.models.Environment;
import com.google.sps.tasks.TaskScheduler;
import com.google.sps.tasks.TaskSchedulerFactory;
import java.io.PrintWriter;
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
public class EnvironmentServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock PrintWriter printWriter;
  @Mock TaskSchedulerFactory taskSchedulerFactory;
  @Mock TaskScheduler scheduler;
  @Mock Authenticator auth;

  @InjectMocks EnvironmentServlet servlet;

  private final String ID_TOKEN = "ID_TOKEN";

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
  public void doGetTest() throws Exception {
    String STATUS = "STATUS";
    String NAME = "NAME";

    Key classKey = datastore.put(new Entity("class"));

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("status", STATUS);
    envEntity.setProperty("name", NAME);
    envEntity.setProperty("class", classKey);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    when(req.getParameter(eq("envID"))).thenReturn(envID);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyInClass(eq(ID_TOKEN), eq(classKey))).thenReturn(true);
    when(resp.getWriter()).thenReturn(printWriter);

    servlet.doGet(req, resp);

    verify(printWriter, times(1)).print(new Gson().toJson(new Environment(NAME, STATUS, envID)));
  }

  @Test
  public void doGetTestNoEntity() throws Exception {
    when(req.getParameter(eq("envID"))).thenReturn("invalid key");

    new EnvironmentServlet().doGet(req, resp);

    verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void doGetTestAuthFail() throws Exception {
    String STATUS = "STATUS";
    String NAME = "NAME";

    Key classKey = datastore.put(new Entity("class"));

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("status", STATUS);
    envEntity.setProperty("name", NAME);
    envEntity.setProperty("class", classKey);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    when(req.getParameter(eq("envID"))).thenReturn(envID);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyInClass(eq(ID_TOKEN), eq(classKey))).thenReturn(false);

    servlet.doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  @Test
  public void doDeleteTest() throws Exception {
    String STATUS = "STATUS";
    String NAME = "NAME";
    String QUEUE_NAME = "QUEUE_NAME";

    Key classKey = datastore.put(new Entity("class"));

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("status", STATUS);
    envEntity.setProperty("name", NAME);
    envEntity.setProperty("class", classKey);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    when(req.getParameter(eq("envID"))).thenReturn(envID);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(resp.getWriter()).thenReturn(printWriter);
    when(taskSchedulerFactory.create(anyString(), anyString())).thenReturn(scheduler);
    when(auth.verifyTaOrOwner(eq(ID_TOKEN), eq(classKey))).thenReturn(true);
    servlet.QUEUE_NAME = QUEUE_NAME;

    servlet.doDelete(req, resp);

    verify(taskSchedulerFactory, times(1)).create(eq(QUEUE_NAME), eq("/tasks/deleteEnv"));
    verify(scheduler, times(1)).schedule(eq(envID));

    assertEquals("deleting", datastore.get(envEntity.getKey()).getProperty("status"));
  }

  @Test
  public void doDeleteAuthFail() throws Exception {
    String STATUS = "STATUS";
    String NAME = "NAME";
    String QUEUE_NAME = "QUEUE_NAME";

    Key classKey = datastore.put(new Entity("class"));

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("status", STATUS);
    envEntity.setProperty("name", NAME);
    envEntity.setProperty("class", classKey);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    when(req.getParameter(eq("envID"))).thenReturn(envID);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyTaOrOwner(eq(ID_TOKEN), eq(classKey))).thenReturn(false);
    servlet.QUEUE_NAME = QUEUE_NAME;

    servlet.doDelete(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

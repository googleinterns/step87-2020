package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.gson.Gson;
import com.google.sps.environment.Environment;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
  @Mock CloudTasksClient client;

  @Captor ArgumentCaptor<Task> taskCapture;

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

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("status", STATUS);
    envEntity.setProperty("name", NAME);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    when(req.getParameter(eq("envID"))).thenReturn(envID);
    when(resp.getWriter()).thenReturn(printWriter);

    new EnvironmentServlet().doGet(req, resp);

    verify(printWriter, times(1)).print(new Gson().toJson(new Environment(NAME, STATUS, envID)));
  }

  @Test
  public void doGetTestNoEntity() throws Exception {
    when(req.getParameter(eq("envID"))).thenReturn("invalid key");

    new EnvironmentServlet().doGet(req, resp);

    verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void doDeleteTest() throws Exception {
    String STATUS = "STATUS";
    String NAME = "NAME";
    String PROJECT_ID = "PROJECT_ID";
    String LOCATION = "LOCATION";
    String QUEUE_NAME = "QUEUE_NAME";

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("status", STATUS);
    envEntity.setProperty("name", NAME);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    EnvironmentServlet servlet = spy(new EnvironmentServlet());

    when(req.getParameter(eq("envID"))).thenReturn(envID);
    when(resp.getWriter()).thenReturn(printWriter);
    when(servlet.getClient()).thenReturn(client);
    when(servlet.getProjectID()).thenReturn(PROJECT_ID);
    when(servlet.getLocation()).thenReturn(LOCATION);
    when(servlet.getQueueName()).thenReturn(QUEUE_NAME);

    servlet.doDelete(req, resp);

    verify(client, times(1))
        .createTask(
            eq(QueueName.of(PROJECT_ID, LOCATION, QUEUE_NAME).toString()), taskCapture.capture());

    AppEngineHttpRequest appengineReq = taskCapture.getValue().getAppEngineHttpRequest();
    assertEquals(envID, appengineReq.getBody().toStringUtf8());
    assertEquals("/tasks/deleteEnv", appengineReq.getRelativeUri());
    assertEquals(HttpMethod.POST, appengineReq.getHttpMethod());

    assertEquals("deleting", datastore.get(envEntity.getKey()).getProperty("status"));
  }
}

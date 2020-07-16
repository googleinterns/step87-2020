package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.sps.tasks.TaskScheduler;
import com.google.sps.tasks.servlets.PullNewEnvironment;
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
public class QueueEnvPullTest {
  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock PrintWriter writer;
  @Mock FirebaseAuth auth;
  @Mock TaskScheduler.Builder taskBuilder;
  @Mock TaskScheduler scheduler;

  @Captor ArgumentCaptor<String> envIDCaptor;

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  private final String IMAGE = "IMAGE";
  private final String TAG = "TAG";
  private final String QUEUE_NAME = "QUEUE_NAME";

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
    Entity classEntity = new Entity("Class");
    datastore.put(classEntity);
    String classID = KeyFactory.keyToString(classEntity.getKey());

    QueueEnvPull servlet = spy(new QueueEnvPull());

    when(req.getParameter(eq("classID"))).thenReturn(classID);
    when(req.getParameter(eq("image"))).thenReturn(IMAGE);
    when(req.getParameter(eq("tag"))).thenReturn(TAG);

    when(servlet.getTaskBuilder()).thenReturn(taskBuilder);
    when(taskBuilder.setQueueName(anyString())).thenReturn(taskBuilder);
    when(taskBuilder.setURI(anyString())).thenReturn(taskBuilder);
    when(taskBuilder.build()).thenReturn(scheduler);
    when(servlet.getQueueName()).thenReturn(QUEUE_NAME);
    when(resp.getWriter()).thenReturn(writer);

    servlet.doGet(req, resp);

    verify(writer, times(1)).print(envIDCaptor.capture());

    String envID = envIDCaptor.getValue();

    verify(taskBuilder, times(1)).setQueueName(eq(QUEUE_NAME));
    verify(taskBuilder, times(1)).setURI(eq("/tasks/pullEnv"));
    verify(taskBuilder, times(1)).build();

    verify(scheduler, times(1)).schedule(eq(String.join(",", envID, classID, IMAGE, TAG)));

    assertEquals("pulling", datastore.get(KeyFactory.stringToKey(envID)).getProperty("status"));
  }

  @Test
  public void doGetTestConfict() throws Exception {
    Entity classEntity = new Entity("Class");
    datastore.put(classEntity);
    String classID = KeyFactory.keyToString(classEntity.getKey());

    Entity conflicting = new Entity("Environment");
    conflicting.setProperty("class", classEntity.getKey());
    conflicting.setProperty("image", PullNewEnvironment.getImageName(classID, IMAGE));
    conflicting.setProperty("tag", TAG);
    datastore.put(conflicting);

    QueueEnvPull servlet = spy(new QueueEnvPull());

    when(req.getParameter(eq("classID"))).thenReturn(classID);
    when(req.getParameter(eq("image"))).thenReturn(IMAGE);
    when(req.getParameter(eq("tag"))).thenReturn(TAG);

    servlet.doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_CONFLICT);
  }
}

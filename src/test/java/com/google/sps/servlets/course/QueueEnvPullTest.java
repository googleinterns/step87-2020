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
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.authentication.Authenticator;
import com.google.sps.tasks.TaskScheduler;
import com.google.sps.tasks.TaskSchedulerFactory;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueueEnvPullTest {
  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock PrintWriter writer;
  @Mock Authenticator auth;
  @Mock TaskSchedulerFactory taskSchedulerFactory;
  @Mock TaskScheduler scheduler;

  @InjectMocks QueueEnvPull servlet;

  @Captor ArgumentCaptor<String> envIDCaptor;

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  private final String IMAGE = "IMAGE";
  private final String TAG = "TAG";
  private final String QUEUE_NAME = "QUEUE_NAME";
  private final String ID_TOKEN = "ID_TOKEN";

  private Entity classEntity;

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();

    classEntity = new Entity("Class");
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void doGetTest() throws Exception {
    datastore.put(classEntity);

    String classID = KeyFactory.keyToString(classEntity.getKey());

    when(req.getParameter(eq("classID"))).thenReturn(classID);
    when(req.getParameter(eq("image"))).thenReturn(IMAGE);
    when(req.getParameter(eq("tag"))).thenReturn(TAG);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);

    when(auth.verifyTaOrOwner(ID_TOKEN, classEntity.getKey())).thenReturn(true);

    when(taskSchedulerFactory.create(anyString(), anyString())).thenReturn(scheduler);
    servlet.QUEUE_NAME = QUEUE_NAME;
    when(resp.getWriter()).thenReturn(writer);

    servlet.doGet(req, resp);

    verify(writer, times(1)).print(envIDCaptor.capture());

    String envID = envIDCaptor.getValue();

    verify(taskSchedulerFactory, times(1)).create(eq(QUEUE_NAME), eq("/tasks/pullEnv"));

    verify(scheduler, times(1)).schedule(eq(String.join(",", envID, classID, IMAGE, TAG)));

    assertEquals("pulling", datastore.get(KeyFactory.stringToKey(envID)).getProperty("status"));
  }

  @Test
  public void doGetTestConfict() throws Exception {
    datastore.put(classEntity);

    String classID = KeyFactory.keyToString(classEntity.getKey());

    Entity conflicting = new Entity("Environment");
    conflicting.setProperty("class", classEntity.getKey());
    conflicting.setProperty("image", PullNewEnvironment.getImageName(classID, IMAGE));
    conflicting.setProperty("tag", TAG);
    datastore.put(conflicting);

    when(req.getParameter(eq("classID"))).thenReturn(classID);
    when(req.getParameter(eq("image"))).thenReturn(IMAGE);
    when(req.getParameter(eq("tag"))).thenReturn(TAG);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);

    when(auth.verifyTaOrOwner(ID_TOKEN, classEntity.getKey())).thenReturn(true);

    servlet.doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_CONFLICT);
  }

  @Test
  public void doGetTestAuthFail() throws Exception {
    datastore.put(classEntity);

    String classID = KeyFactory.keyToString(classEntity.getKey());

    Entity conflicting = new Entity("Environment");
    conflicting.setProperty("class", classEntity.getKey());
    conflicting.setProperty("image", PullNewEnvironment.getImageName(classID, IMAGE));
    conflicting.setProperty("tag", TAG);
    datastore.put(conflicting);

    when(req.getParameter(eq("classID"))).thenReturn(classID);
    when(req.getParameter(eq("image"))).thenReturn(IMAGE);
    when(req.getParameter(eq("tag"))).thenReturn(TAG);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);

    when(auth.verifyTaOrOwner(ID_TOKEN, classEntity.getKey())).thenReturn(false);

    servlet.doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

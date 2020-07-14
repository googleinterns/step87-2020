package com.google.sps.tasks;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Arrays;
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
public class DeleteEnvironmentTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock DockerClient docker;
  @Mock RemoveImageCmd rmCmd;
  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock BufferedReader reader;
  @Mock PrintWriter writer;

  @InjectMocks DeleteEnvironment servlet;

  private String IMAGE = "IMAGE";
  private String TAG = "TAG";

  @Before
  public void setUp() {
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test(expected = EntityNotFoundException.class)
  public void doPost() throws Exception {
    Entity env = new Entity("Environment");
    env.setProperty("image", IMAGE);
    env.setProperty("tag", TAG);
    datastore.put(env);

    when(docker.removeImageCmd(anyString())).thenReturn(rmCmd);
    when(req.getReader()).thenReturn(reader);
    when(reader.lines())
        .thenReturn(Arrays.stream(new String[] {KeyFactory.keyToString(env.getKey())}));

    servlet.doPost(req, resp);

    verify(docker, times(1)).removeImageCmd(eq(IMAGE + ':' + TAG));
    verify(rmCmd, times(1)).exec();

    datastore.get(env.getKey());
  }

  @Test(expected = EntityNotFoundException.class)
  public void doPostImageDoesNotExist() throws Exception {
    Entity env = new Entity("Environment");
    env.setProperty("image", IMAGE);
    env.setProperty("tag", TAG);
    datastore.put(env);

    when(docker.removeImageCmd(anyString())).thenReturn(rmCmd);
    when(rmCmd.exec()).thenThrow(new NotFoundException("message"));
    when(req.getReader()).thenReturn(reader);
    when(reader.lines())
        .thenReturn(Arrays.stream(new String[] {KeyFactory.keyToString(env.getKey())}));
    when(resp.getWriter()).thenReturn(writer);

    servlet.doPost(req, resp);

    verify(docker, times(1)).removeImageCmd(eq(IMAGE + ':' + TAG));
    verify(rmCmd, times(1)).exec();

    datastore.get(env.getKey());
  }
}

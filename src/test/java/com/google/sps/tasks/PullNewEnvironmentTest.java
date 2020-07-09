package com.google.sps.tasks;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.command.TagImageCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.PullResponseItem;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.io.BufferedReader;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
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
public class PullNewEnvironmentTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock BufferedReader reader;
  @Mock DockerClient docker;
  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock PullImageCmd imgCmd;
  @Mock ResultCallback.Adapter<PullResponseItem> adapter;
  @Mock TagImageCmd tagCmd;
  @Mock RemoveImageCmd rmCmd;

  @InjectMocks PullNewEnvironment servlet;

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
  public void doPostTest() throws Exception {
    String STATUS = "STATUS";
    String CLASS_ID = "CLASS_ID";
    String IMAGE = "IMAGE";
    String TAG = "TAG";

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("status", STATUS);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    Stream<String> lines = Arrays.asList(String.join(",", envID, CLASS_ID, IMAGE, TAG)).stream();

    when(req.getReader()).thenReturn(reader);
    when(reader.lines()).thenReturn(lines);
    when(docker.pullImageCmd(anyString())).thenReturn(imgCmd);
    when(imgCmd.exec(any())).thenReturn(adapter);
    when(adapter.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(true);
    when(docker.tagImageCmd(anyString(), anyString(), anyString())).thenReturn(tagCmd);
    when(docker.removeImageCmd(anyString())).thenReturn(rmCmd);

    servlet.doPost(req, resp);

    verify(docker, times(1)).pullImageCmd(eq(IMAGE + ':' + TAG));
    verify(adapter, times(1)).awaitCompletion(anyLong(), any(TimeUnit.class));
    verify(docker, times(1))
        .tagImageCmd(eq(IMAGE), eq(CLASS_ID.toLowerCase() + '-' + IMAGE), eq(TAG));
    verify(tagCmd, times(1)).exec();
    verify(docker, times(1)).removeImageCmd(IMAGE);
    verify(rmCmd, times(1)).exec();

    envEntity = datastore.get(KeyFactory.stringToKey(envID));
    assertEquals("ready", envEntity.getProperty("status"));
    assertEquals(CLASS_ID.toLowerCase() + '-' + IMAGE, envEntity.getProperty("image"));
    assertEquals(TAG, envEntity.getProperty("tag"));
  }

  @Test
  public void doPostTestTimeOut() throws Exception {
    String STATUS = "STATUS";
    String CLASS_ID = "CLASS_ID";
    String IMAGE = "IMAGE";
    String TAG = "TAG";

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("status", STATUS);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    Stream<String> lines = Arrays.asList(String.join(",", envID, CLASS_ID, IMAGE, TAG)).stream();

    when(req.getReader()).thenReturn(reader);
    when(reader.lines()).thenReturn(lines);
    when(docker.pullImageCmd(anyString())).thenReturn(imgCmd);
    when(imgCmd.exec(any())).thenReturn(adapter);
    when(adapter.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(false);

    servlet.doPost(req, resp);

    verify(docker, times(1)).pullImageCmd(eq(IMAGE + ':' + TAG));
    verify(adapter, times(1)).awaitCompletion(anyLong(), any(TimeUnit.class));

    envEntity = datastore.get(KeyFactory.stringToKey(envID));
    assertEquals("timeout", envEntity.getProperty("status"));
  }

  @Test
  public void doPostTestFail() throws Exception {
    String STATUS = "STATUS";
    String CLASS_ID = "CLASS_ID";
    String IMAGE = "IMAGE";
    String TAG = "TAG";

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("status", STATUS);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    Stream<String> lines = Arrays.asList(String.join(",", envID, CLASS_ID, IMAGE, TAG)).stream();

    when(req.getReader()).thenReturn(reader);
    when(reader.lines()).thenReturn(lines);
    when(docker.pullImageCmd(anyString())).thenReturn(imgCmd);
    when(imgCmd.exec(any())).thenThrow(new DockerException("message", 500));

    servlet.doPost(req, resp);

    verify(docker, times(1)).pullImageCmd(eq(IMAGE + ':' + TAG));

    envEntity = datastore.get(KeyFactory.stringToKey(envID));
    assertEquals("failed", envEntity.getProperty("status"));
  }
}

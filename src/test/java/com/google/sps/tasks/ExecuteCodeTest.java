package com.google.sps.tasks;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.AttachContainerCmd;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.KillContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Frame;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceArchive;
import com.google.sps.workspace.WorkspaceArchive.ArchiveType;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class ExecuteCodeTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastore;

  @Mock DockerClient docker;
  @Mock WorkspaceFactory workspaceFactory;
  @Mock Workspace w;
  @Mock WorkspaceArchive archive;
  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;

  @Mock BufferedReader reader;
  @Mock CreateContainerCmd contCmd;
  @Mock CreateContainerResponse container;
  @Mock CopyArchiveToContainerCmd cpCmd;
  @Mock AttachContainerCmd attachCmd;
  @Mock ResultCallback.Adapter<Frame> adapter;
  @Mock StartContainerCmd startCmd;
  @Mock KillContainerCmd killCmd;
  @Mock OutputStream output;
  @Mock Frame frame;

  @Captor ArgumentCaptor<InputStream> inputCaptor;

  @InjectMocks ExecuteCode servlet;

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
    String WORKSPACE_ID = "WORKSPACE_ID";
    String EXECUTION_ID = "EXECUTION_ID";
    String IMAGE = "IMAGE";
    String TAG = "TAG";
    byte[] tar = "TAR".getBytes();
    String CONTAINER_ID = "CONTAINER_ID";

    Stream<String> lines = Arrays.asList(WORKSPACE_ID + "," + EXECUTION_ID).stream();

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("image", IMAGE);
    envEntity.setProperty("tag", TAG);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    CompletableFuture<String> envIDFuture = new CompletableFuture<>();
    envIDFuture.complete(envID);

    when(req.getReader()).thenReturn(reader);
    when(reader.lines()).thenReturn(lines);
    when(workspaceFactory.fromWorkspaceID(eq(WORKSPACE_ID))).thenReturn(w);
    when(w.getEnvironment()).thenReturn(envIDFuture);
    when(docker.createContainerCmd(anyString())).thenReturn(contCmd);
    when(contCmd.withAttachStdout(anyBoolean())).thenReturn(contCmd);
    when(contCmd.withAttachStderr(anyBoolean())).thenReturn(contCmd);
    when(contCmd.withTty(anyBoolean())).thenReturn(contCmd);
    when(contCmd.withHostConfig(any())).thenReturn(contCmd);
    when(contCmd.exec()).thenReturn(container);
    when(w.getArchive(ArchiveType.TAR)).thenReturn(archive);
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                ((OutputStream) invocation.getArgument(0)).write(tar);
                return null;
              }
            })
        .when(archive)
        .archive(any());
    when(container.getId()).thenReturn(CONTAINER_ID);
    when(docker.copyArchiveToContainerCmd(anyString())).thenReturn(cpCmd);
    when(cpCmd.withTarInputStream(any())).thenReturn(cpCmd);
    when(cpCmd.withRemotePath(any())).thenReturn(cpCmd);
    when(docker.attachContainerCmd(anyString())).thenReturn(attachCmd);
    when(attachCmd.withStdOut(anyBoolean())).thenReturn(attachCmd);
    when(attachCmd.withStdErr(anyBoolean())).thenReturn(attachCmd);
    when(attachCmd.withFollowStream(anyBoolean())).thenReturn(attachCmd);
    when(attachCmd.exec(any())).thenReturn(adapter);
    when(docker.startContainerCmd(anyString())).thenReturn(startCmd);
    when(adapter.awaitCompletion(anyLong(), any())).thenReturn(true);

    servlet.doPost(req, resp);

    verify(docker, times(1)).createContainerCmd(eq(IMAGE + ":" + TAG));
    verify(contCmd, times(1)).withAttachStdout(true);
    verify(contCmd, times(1)).withAttachStderr(true);
    verify(contCmd, times(1)).exec();

    verify(docker, times(1)).copyArchiveToContainerCmd(eq(CONTAINER_ID));
    verify(cpCmd, times(1)).withTarInputStream(inputCaptor.capture());
    verify(cpCmd, times(1)).withRemotePath(eq("/workspace"));
    verify(cpCmd, times(1)).exec();

    byte[] input = new byte[tar.length];
    inputCaptor.getValue().read(input);
    assertArrayEquals(tar, input);

    verify(docker, times(1)).attachContainerCmd(eq(CONTAINER_ID));
    verify(attachCmd, times(1)).withStdOut(true);
    verify(attachCmd, times(1)).withStdErr(true);
    verify(attachCmd, times(1)).exec(any(ExecuteCode.OutputAdapter.class));

    verify(docker, times(1)).startContainerCmd(eq(CONTAINER_ID));
    verify(startCmd, times(1)).exec();
    verify(adapter, times(1)).awaitCompletion(anyLong(), any());

    verify(w, times(1)).setExitCode(eq(EXECUTION_ID), anyInt());
  }

  @Test
  public void doPostTestTimeOut() throws Exception {
    String WORKSPACE_ID = "WORKSPACE_ID";
    String EXECUTION_ID = "EXECUTION_ID";
    String IMAGE = "IMAGE";
    String TAG = "TAG";
    byte[] tar = "TAR".getBytes();
    String CONTAINER_ID = "CONTAINER_ID";

    Stream<String> lines = Arrays.asList(WORKSPACE_ID + "," + EXECUTION_ID).stream();

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("image", IMAGE);
    envEntity.setProperty("tag", TAG);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    CompletableFuture<String> envIDFuture = new CompletableFuture<>();
    envIDFuture.complete(envID);

    when(req.getReader()).thenReturn(reader);
    when(reader.lines()).thenReturn(lines);
    when(workspaceFactory.fromWorkspaceID(eq(WORKSPACE_ID))).thenReturn(w);
    when(w.getEnvironment()).thenReturn(envIDFuture);
    when(docker.createContainerCmd(anyString())).thenReturn(contCmd);
    when(contCmd.withAttachStdout(anyBoolean())).thenReturn(contCmd);
    when(contCmd.withAttachStderr(anyBoolean())).thenReturn(contCmd);
    when(contCmd.withTty(anyBoolean())).thenReturn(contCmd);
    when(contCmd.withHostConfig(any())).thenReturn(contCmd);
    when(contCmd.exec()).thenReturn(container);
    when(w.getArchive(ArchiveType.TAR)).thenReturn(archive);
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                ((OutputStream) invocation.getArgument(0)).write(tar);
                return null;
              }
            })
        .when(archive)
        .archive(any());
    when(container.getId()).thenReturn(CONTAINER_ID);
    when(docker.copyArchiveToContainerCmd(anyString())).thenReturn(cpCmd);
    when(cpCmd.withTarInputStream(any())).thenReturn(cpCmd);
    when(cpCmd.withRemotePath(any())).thenReturn(cpCmd);
    when(docker.attachContainerCmd(anyString())).thenReturn(attachCmd);
    when(attachCmd.withStdOut(anyBoolean())).thenReturn(attachCmd);
    when(attachCmd.withStdErr(anyBoolean())).thenReturn(attachCmd);
    when(attachCmd.withFollowStream(anyBoolean())).thenReturn(attachCmd);
    when(attachCmd.exec(any())).thenReturn(adapter);
    when(docker.startContainerCmd(anyString())).thenReturn(startCmd);
    when(adapter.awaitCompletion(anyLong(), any())).thenReturn(false);
    when(docker.killContainerCmd(anyString())).thenReturn(killCmd);

    servlet.doPost(req, resp);

    verify(docker, times(1)).createContainerCmd(eq(IMAGE + ":" + TAG));
    verify(contCmd, times(1)).withAttachStdout(true);
    verify(contCmd, times(1)).withAttachStderr(true);
    verify(contCmd, times(1)).exec();

    verify(docker, times(1)).copyArchiveToContainerCmd(eq(CONTAINER_ID));
    verify(cpCmd, times(1)).withTarInputStream(inputCaptor.capture());
    verify(cpCmd, times(1)).withRemotePath(eq("/workspace"));
    verify(cpCmd, times(1)).exec();

    byte[] input = new byte[tar.length];
    inputCaptor.getValue().read(input);
    assertArrayEquals(tar, input);

    verify(docker, times(1)).attachContainerCmd(eq(CONTAINER_ID));
    verify(attachCmd, times(1)).withStdOut(true);
    verify(attachCmd, times(1)).withStdErr(true);
    verify(attachCmd, times(1)).exec(any(ExecuteCode.OutputAdapter.class));

    verify(docker, times(1)).startContainerCmd(eq(CONTAINER_ID));
    verify(startCmd, times(1)).exec();
    verify(adapter, times(1)).awaitCompletion(anyLong(), any());

    verify(docker, times(1)).killContainerCmd(eq(CONTAINER_ID));
    verify(killCmd, times(1)).exec();
  }

  @Test
  public void doPostTestFail() throws Exception {
    String WORKSPACE_ID = "WORKSPACE_ID";
    String EXECUTION_ID = "EXECUTION_ID";
    String IMAGE = "IMAGE";
    String TAG = "TAG";
    byte[] tar = "TAR".getBytes();
    String CONTAINER_ID = "CONTAINER_ID";

    Stream<String> lines = Arrays.asList(WORKSPACE_ID + "," + EXECUTION_ID).stream();

    Entity envEntity = new Entity("Environment");
    envEntity.setProperty("image", IMAGE);
    envEntity.setProperty("tag", TAG);
    String envID = KeyFactory.keyToString(datastore.put(envEntity));

    CompletableFuture<String> envIDFuture = new CompletableFuture<>();
    envIDFuture.complete(envID);

    when(req.getReader()).thenReturn(reader);
    when(reader.lines()).thenReturn(lines);
    when(workspaceFactory.fromWorkspaceID(eq(WORKSPACE_ID))).thenReturn(w);
    when(w.getEnvironment()).thenReturn(envIDFuture);
    when(docker.createContainerCmd(anyString())).thenReturn(contCmd);
    when(contCmd.withAttachStdout(anyBoolean())).thenReturn(contCmd);
    when(contCmd.withAttachStderr(anyBoolean())).thenReturn(contCmd);
    when(contCmd.withTty(anyBoolean())).thenReturn(contCmd);
    when(contCmd.withHostConfig(any())).thenReturn(contCmd);
    when(contCmd.exec()).thenReturn(container);
    when(w.getArchive(ArchiveType.TAR)).thenReturn(archive);
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                ((OutputStream) invocation.getArgument(0)).write(tar);
                return null;
              }
            })
        .when(archive)
        .archive(any());
    when(container.getId()).thenReturn(CONTAINER_ID);
    when(docker.copyArchiveToContainerCmd(anyString()))
        .thenThrow(new DockerException("message", 500));
    when(docker.killContainerCmd(anyString())).thenReturn(killCmd);

    servlet.doPost(req, resp);

    verify(docker, times(1)).createContainerCmd(eq(IMAGE + ":" + TAG));
    verify(contCmd, times(1)).withAttachStdout(true);
    verify(contCmd, times(1)).withAttachStderr(true);
    verify(contCmd, times(1)).exec();

    verify(docker, times(1)).copyArchiveToContainerCmd(eq(CONTAINER_ID));

    verify(docker, times(1)).killContainerCmd(eq(CONTAINER_ID));
    verify(killCmd, times(1)).exec();

    verify(resp, times(1)).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test(expected = NullPointerException.class)
  public void OutputAdapterNullStream() throws Exception {
    new ExecuteCode.OutputAdapter(null, "").close();
    ;
  }

  @Test(expected = NullPointerException.class)
  public void OutputAdapterNullExecID() throws Exception {
    new ExecuteCode.OutputAdapter(w, null).close();
  }

  @Test
  public void OutputAdapterOnNext() throws Exception {
    String EXECUTION_ID = "EXECUTION_ID";
    byte[] payload = "PAYLOAD".getBytes();

    when(frame.getPayload()).thenReturn(payload);

    new ExecuteCode.OutputAdapter(w, EXECUTION_ID).onNext(frame);

    verify(w, times(1)).writeOutput(eq(EXECUTION_ID), eq(new String(payload)));
  }
}

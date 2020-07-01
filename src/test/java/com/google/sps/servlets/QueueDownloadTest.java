package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.protobuf.ByteString;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueueDownloadTest {
  @Mock WorkspaceFactory workspaceFactory;
  @Mock Workspace workspace;
  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock CloudTasksClient client;
  @Mock PrintWriter writer;
  @Mock FirebaseAuth auth;

  @Captor ArgumentCaptor<Task> taskCaptor;

  private final String WORKSPACE_ID = "WORKSPACE_ID";
  private final String DOWNLOAD_ID = "DOWNLOAD_ID";

  private final String PROJECT_ID = "PROJECT_ID";
  private final String LOCATION = "LOCATION";
  private final String QUEUE_NAME = "QUEUE_NAME";
  private final String ID_TOKEN = "ID_TOKEN";
  private final String UID = "UID";
  private final String BAD_UID = "BAD_UID";

  @Test
  public void doGetTestStudent() throws Exception {
    QueueDownload servlet = spy(new QueueDownload(workspaceFactory, auth));
    FirebaseToken tok = mock(FirebaseToken.class);

    final CompletableFuture<String> future = new CompletableFuture<>();
    future.complete(UID);

    when(req.getParameter(eq("workspaceID"))).thenReturn(WORKSPACE_ID);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyIdToken(eq(ID_TOKEN))).thenReturn(tok);
    when(tok.getUid()).thenReturn(UID);
    when(workspaceFactory.fromWorkspaceID(eq(WORKSPACE_ID))).thenReturn(workspace);
    when(workspace.newDownloadID()).thenReturn(DOWNLOAD_ID);
    when(workspace.getWorkspaceID()).thenReturn(WORKSPACE_ID);
    when(workspace.getStudentUID()).thenReturn(future);
    when(servlet.getClient()).thenReturn(client);
    when(servlet.getProjectID()).thenReturn(PROJECT_ID);
    when(servlet.getLocation()).thenReturn(LOCATION);
    when(servlet.getQueueName()).thenReturn(QUEUE_NAME);
    when(resp.getWriter()).thenReturn(writer);

    servlet.doGet(req, resp);

    verify(client, times(1))
        .createTask(
            eq(QueueName.of(PROJECT_ID, LOCATION, QUEUE_NAME).toString()), taskCaptor.capture());

    AppEngineHttpRequest apppengineReq = taskCaptor.getValue().getAppEngineHttpRequest();

    assertEquals(
        ByteString.copyFrom(WORKSPACE_ID + ',' + DOWNLOAD_ID, Charset.defaultCharset()),
        apppengineReq.getBody());
    assertEquals("/tasks/prepareDownload", apppengineReq.getRelativeUri());
    assertEquals(HttpMethod.POST, apppengineReq.getHttpMethod());

    verify(writer, times(1)).print(eq(DOWNLOAD_ID));
    verify(client, times(1)).close();
  }

  @Test
  public void doGetTestTA() throws Exception {
    QueueDownload servlet = spy(new QueueDownload(workspaceFactory, auth));
    FirebaseToken tok = mock(FirebaseToken.class);

    final CompletableFuture<String> studentFuture = new CompletableFuture<>();
    studentFuture.complete(BAD_UID);
    final CompletableFuture<String> taFuture = new CompletableFuture<>();
    taFuture.complete(UID);

    when(req.getParameter(eq("workspaceID"))).thenReturn(WORKSPACE_ID);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyIdToken(eq(ID_TOKEN))).thenReturn(tok);
    when(tok.getUid()).thenReturn(UID);
    when(workspaceFactory.fromWorkspaceID(eq(WORKSPACE_ID))).thenReturn(workspace);
    when(workspace.newDownloadID()).thenReturn(DOWNLOAD_ID);
    when(workspace.getWorkspaceID()).thenReturn(WORKSPACE_ID);
    when(workspace.getStudentUID()).thenReturn(studentFuture);
    when(workspace.getTaUID()).thenReturn(taFuture);
    when(servlet.getClient()).thenReturn(client);
    when(servlet.getProjectID()).thenReturn(PROJECT_ID);
    when(servlet.getLocation()).thenReturn(LOCATION);
    when(servlet.getQueueName()).thenReturn(QUEUE_NAME);
    when(resp.getWriter()).thenReturn(writer);

    servlet.doGet(req, resp);

    verify(client, times(1))
        .createTask(
            eq(QueueName.of(PROJECT_ID, LOCATION, QUEUE_NAME).toString()), taskCaptor.capture());

    AppEngineHttpRequest apppengineReq = taskCaptor.getValue().getAppEngineHttpRequest();

    assertEquals(
        ByteString.copyFrom(WORKSPACE_ID + ',' + DOWNLOAD_ID, Charset.defaultCharset()),
        apppengineReq.getBody());
    assertEquals("/tasks/prepareDownload", apppengineReq.getRelativeUri());
    assertEquals(HttpMethod.POST, apppengineReq.getHttpMethod());

    verify(writer, times(1)).print(eq(DOWNLOAD_ID));
    verify(client, times(1)).close();
  }

  @Test
  public void authFailFirebaseException() throws Exception {
    when(req.getParameter(anyString())).thenReturn("idToken");
    when(auth.verifyIdToken(anyString())).thenThrow(new FirebaseAuthException("code", "message"));

    new QueueDownload(workspaceFactory, auth).doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  @Test
  public void authFailIllegalArgException() throws Exception {
    when(req.getParameter(anyString())).thenReturn("idToken");
    when(auth.verifyIdToken(anyString())).thenThrow(new IllegalArgumentException());

    new QueueDownload(workspaceFactory, auth).doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  @Test
  public void authFailWrongUID() throws Exception {
    FirebaseToken tok = mock(FirebaseToken.class);

    final CompletableFuture<String> future = new CompletableFuture<>();
    future.complete(UID);

    when(req.getParameter(eq("workspaceID"))).thenReturn(WORKSPACE_ID);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyIdToken(eq(ID_TOKEN))).thenReturn(tok);
    when(tok.getUid()).thenReturn(BAD_UID);
    when(workspaceFactory.fromWorkspaceID(eq(WORKSPACE_ID))).thenReturn(workspace);
    when(workspace.getStudentUID()).thenReturn(future);
    when(workspace.getTaUID()).thenReturn(future);

    new QueueDownload(workspaceFactory, auth).doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

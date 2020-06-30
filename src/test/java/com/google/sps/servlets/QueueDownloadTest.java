package com.google.sps.servlets;

import static org.junit.Assert.assertEquals;
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

  @Test
  public void doGetTest() throws Exception {
    QueueDownload servlet = spy(new QueueDownload(workspaceFactory, auth));
    FirebaseToken tok = mock(FirebaseToken.class);

    final String WORKSPACE_ID = "WORKSPACE_ID";
    final String DOWNLOAD_ID = "DOWNLOAD_ID";

    final String PROJECT_ID = "PROJECT_ID";
    final String LOCATION = "LOCATION";
    final String QUEUE_NAME = "QUEUE_NAME";
    final String ID_TOKEN = "ID_TOKEN";
    final String UID = "UID";
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
    when(workspace.getTaUID()).thenReturn(future);
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
}

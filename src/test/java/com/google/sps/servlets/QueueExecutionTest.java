package com.google.sps.servlets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.sps.tasks.TaskScheduler;
import com.google.sps.tasks.TaskSchedulerFactory;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueueExecutionTest {
  @Mock WorkspaceFactory workspaceFactory;
  @Mock Workspace workspace;
  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock PrintWriter writer;
  @Mock FirebaseAuth auth;
  @Mock TaskSchedulerFactory taskSchedulerFactory;
  @Mock TaskScheduler scheduler;

  @InjectMocks QueueExecution servlet;

  private final String WORKSPACE_ID = "WORKSPACE_ID";
  private final String EXECUTION_ID = "DOWNLOAD_ID";

  private final String QUEUE_NAME = "QUEUE_NAME";
  private final String ID_TOKEN = "ID_TOKEN";
  private final String UID = "UID";
  private final String BAD_UID = "BAD_UID";
  private final String ENV_ID = "ENV_ID";

  @Test
  public void doGetTestStudent() throws Exception {
    FirebaseToken tok = mock(FirebaseToken.class);

    CompletableFuture<String> future = new CompletableFuture<>();
    future.complete(UID);

    when(req.getParameter(eq("workspaceID"))).thenReturn(WORKSPACE_ID);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(req.getParameter("envID")).thenReturn(ENV_ID);
    when(auth.verifyIdToken(eq(ID_TOKEN))).thenReturn(tok);
    when(tok.getUid()).thenReturn(UID);
    when(workspaceFactory.fromWorkspaceID(eq(WORKSPACE_ID))).thenReturn(workspace);
    when(workspace.newExecutionID()).thenReturn(EXECUTION_ID);
    when(workspace.getWorkspaceID()).thenReturn(WORKSPACE_ID);
    when(workspace.getStudentUID()).thenReturn(future);
    when(taskSchedulerFactory.create(anyString(), anyString())).thenReturn(scheduler);
    servlet.QUEUE_NAME = QUEUE_NAME;
    when(resp.getWriter()).thenReturn(writer);

    servlet.doGet(req, resp);

    verify(taskSchedulerFactory, times(1)).create(eq(QUEUE_NAME), eq("/tasks/executeCode"));
    verify(scheduler, times(1)).schedule(eq(WORKSPACE_ID + ',' + ENV_ID + ',' + EXECUTION_ID));

    verify(writer, times(1)).print(eq(EXECUTION_ID));
  }

  @Test
  public void doGetTestTA() throws Exception {
    FirebaseToken tok = mock(FirebaseToken.class);

    CompletableFuture<String> studentFuture = new CompletableFuture<>();
    studentFuture.complete(BAD_UID);
    CompletableFuture<String> taFuture = new CompletableFuture<>();
    taFuture.complete(UID);

    when(req.getParameter(eq("workspaceID"))).thenReturn(WORKSPACE_ID);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(req.getParameter("envID")).thenReturn(ENV_ID);
    when(auth.verifyIdToken(eq(ID_TOKEN))).thenReturn(tok);
    when(tok.getUid()).thenReturn(UID);
    when(workspaceFactory.fromWorkspaceID(eq(WORKSPACE_ID))).thenReturn(workspace);
    when(workspace.newExecutionID()).thenReturn(EXECUTION_ID);
    when(workspace.getWorkspaceID()).thenReturn(WORKSPACE_ID);
    when(workspace.getStudentUID()).thenReturn(studentFuture);
    when(workspace.getTaUID()).thenReturn(taFuture);
    when(taskSchedulerFactory.create(anyString(), anyString())).thenReturn(scheduler);
    servlet.QUEUE_NAME = QUEUE_NAME;
    when(resp.getWriter()).thenReturn(writer);

    servlet.doGet(req, resp);

    verify(taskSchedulerFactory, times(1)).create(eq(QUEUE_NAME), eq("/tasks/executeCode"));
    verify(scheduler, times(1)).schedule(eq(WORKSPACE_ID + ',' + ENV_ID + ',' + EXECUTION_ID));

    verify(writer, times(1)).print(eq(EXECUTION_ID));
  }

  @Test
  public void authFailFirebaseException() throws Exception {
    when(req.getParameter(anyString())).thenReturn("idToken");
    when(auth.verifyIdToken(anyString())).thenThrow(new FirebaseAuthException("code", "message"));

    servlet.doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  @Test
  public void authFailIllegalArgException() throws Exception {
    when(req.getParameter(anyString())).thenReturn("idToken");
    when(auth.verifyIdToken(anyString())).thenThrow(new IllegalArgumentException());

    servlet.doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  @Test
  public void authFailWrongUID() throws Exception {
    FirebaseToken tok = mock(FirebaseToken.class);

    CompletableFuture<String> future = new CompletableFuture<>();
    future.complete(UID);

    when(req.getParameter(eq("workspaceID"))).thenReturn(WORKSPACE_ID);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyIdToken(eq(ID_TOKEN))).thenReturn(tok);
    when(tok.getUid()).thenReturn(BAD_UID);
    when(workspaceFactory.fromWorkspaceID(eq(WORKSPACE_ID))).thenReturn(workspace);
    when(workspace.getStudentUID()).thenReturn(future);
    when(workspace.getTaUID()).thenReturn(future);

    servlet.doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

package com.google.sps.servlets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.sps.authentication.Authenticator;
import com.google.sps.tasks.TaskScheduler;
import com.google.sps.tasks.TaskSchedulerFactory;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueueDownloadTest {
  @Mock WorkspaceFactory workspaceFactory;
  @Mock Workspace workspace;
  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock PrintWriter writer;
  @Mock Authenticator auth;
  @Mock TaskSchedulerFactory taskSchedulerFactory;
  @Mock TaskScheduler scheduler;

  @InjectMocks QueueDownload servlet;

  private final String WORKSPACE_ID = "WORKSPACE_ID";
  private final String DOWNLOAD_ID = "DOWNLOAD_ID";

  private final String QUEUE_NAME = "QUEUE_NAME";
  private final String ID_TOKEN = "ID_TOKEN";

  @Test
  public void doGetTestStudent() throws Exception {
    when(req.getParameter(eq("workspaceID"))).thenReturn(WORKSPACE_ID);
    when(req.getParameter(eq("idToken"))).thenReturn(ID_TOKEN);
    when(auth.verifyWorkspace(eq(ID_TOKEN), eq(workspace))).thenReturn(true);
    when(workspaceFactory.fromWorkspaceID(eq(WORKSPACE_ID))).thenReturn(workspace);
    when(workspace.newDownloadID()).thenReturn(DOWNLOAD_ID);
    when(workspace.getWorkspaceID()).thenReturn(WORKSPACE_ID);
    when(taskSchedulerFactory.create(anyString(), anyString())).thenReturn(scheduler);
    servlet.QUEUE_NAME = QUEUE_NAME;
    when(resp.getWriter()).thenReturn(writer);

    servlet.doGet(req, resp);

    verify(taskSchedulerFactory, times(1)).create(eq(QUEUE_NAME), eq("/tasks/prepareDownload"));
    verify(scheduler, times(1)).schedule(eq(WORKSPACE_ID + ',' + DOWNLOAD_ID));

    verify(writer, times(1)).print(eq(DOWNLOAD_ID));
  }

  @Test
  public void authFail() throws Exception {
    when(req.getParameter(anyString())).thenReturn("idToken");
    when(auth.verifyWorkspace(anyString(), any())).thenReturn(false);

    servlet.doGet(req, resp);

    verify(resp, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}

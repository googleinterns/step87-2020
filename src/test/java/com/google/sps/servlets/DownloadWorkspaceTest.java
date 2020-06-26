package com.google.sps.servlets;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceArchive;
import com.google.sps.workspace.WorkspaceFactory;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DownloadWorkspaceTest {
  @Mock WorkspaceFactory workspaceFactory;
  @Mock HttpServletRequest req;
  @Mock HttpServletResponse resp;
  @Mock Workspace workspace;
  @Mock WorkspaceArchive archive;
  @Mock ServletOutputStream out;

  @InjectMocks DownloadWorkspace servlet;

  @Test
  public void doGet() throws Exception {
    final String WORKSPACE_ID = "WORKSPACE_ID";
    when(req.getParameter(eq("workspaceID"))).thenReturn(WORKSPACE_ID);
    when(workspaceFactory.fromWorkspaceID(WORKSPACE_ID)).thenReturn(workspace);
    when(workspace.getArchive()).thenReturn(archive);
    when(resp.getOutputStream()).thenReturn(out);

    servlet.doGet(req, resp);

    verify(req, times(1)).getParameter(eq("workspaceID"));
    verify(resp, times(1)).setContentType("application/x-gzip");
    verify(archive, times(1)).archive(out);
  }
}

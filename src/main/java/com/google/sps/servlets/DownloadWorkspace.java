package com.google.sps.servlets;

import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/workspace/downloadWorkspace")
public class DownloadWorkspace extends HttpServlet {
  WorkspaceFactory workspaceFactory;

  @Override
  public void init() throws ServletException {
    // TODO Auto-generated method stub
    super.init();
    workspaceFactory = WorkspaceFactory.getInstance();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Workspace w = workspaceFactory.fromWorkspaceID(req.getParameter("workspaceID"));

    resp.setContentType("application/x-gzip");

    try {
      w.getArchive().archive(resp.getOutputStream());
    } catch (InterruptedException | ExecutionException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}

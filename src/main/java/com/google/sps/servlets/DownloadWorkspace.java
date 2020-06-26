package com.google.sps.servlets;

import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceArchive;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/workspace/downloadWorkspace")
public class DownloadWorkspace extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Workspace w = new Workspace(req.getParameter("workspaceID"));

    resp.setContentType("application/x-gzip");

    try {
      new WorkspaceArchive(w).archive(resp.getOutputStream());
    } catch (InterruptedException | ExecutionException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}

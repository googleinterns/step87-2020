package com.google.sps.tasks.servlets;

import com.google.sps.workspace.WorkspaceFactory;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/tasks/deleteWorkspace")
public class DeleteWorkspace extends HttpServlet {
  private WorkspaceFactory workspaceFactory;

  @Override
  public void init() throws ServletException {
    workspaceFactory = WorkspaceFactory.getInstance();
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String workspaceID =
        req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);

    try {
      workspaceFactory.fromWorkspaceID(workspaceID).delete();
    } catch (InterruptedException | ExecutionException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}

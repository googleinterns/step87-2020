package com.google.sps.servlets.workspace;

import com.google.common.annotations.VisibleForTesting;
import com.google.sps.authentication.Authenticator;
import com.google.sps.tasks.TaskSchedulerFactory;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/workspace/queueExecution")
public class QueueExecution extends HttpServlet {
  private WorkspaceFactory workspaceFactory;
  private TaskSchedulerFactory taskSchedulerFactory;
  private Authenticator auth;

  @VisibleForTesting protected String QUEUE_NAME;

  @Override
  public void init() throws ServletException {
    super.init();
    workspaceFactory = WorkspaceFactory.getInstance();
    taskSchedulerFactory = TaskSchedulerFactory.getInstance();
    QUEUE_NAME = System.getenv("EXECUTION_QUEUE_ID");
    try {
      auth = new Authenticator();
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String idToken = req.getParameter("idToken");
    String envID = req.getParameter("envID");
    try {
      Workspace w = workspaceFactory.fromWorkspaceID(req.getParameter("workspaceID"));

      if (auth.verifyWorkspace(idToken, w)) {
        String execID = w.newExecutionID();

        taskSchedulerFactory
            .create(QUEUE_NAME, "/tasks/executeCode")
            .schedule(String.join(",", w.getWorkspaceID(), envID, execID));

        resp.getWriter().print(execID);

      } else {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      }
    } catch (InterruptedException | ExecutionException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}

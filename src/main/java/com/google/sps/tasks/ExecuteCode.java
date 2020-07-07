package com.google.sps.tasks;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceArchive.ArchiveType;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/tasks/executeCode")
public class ExecuteCode extends HttpServlet {
  DockerClient docker;
  WorkspaceFactory workspaceFactory;

  @Override
  public void init() throws ServletException {
    DockerClientConfig cfg = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    DockerHttpClient httpClient =
        new ApacheDockerHttpClient.Builder()
            .dockerHost(cfg.getDockerHost())
            .sslConfig(cfg.getSSLConfig())
            .build();
    docker = DockerClientImpl.getInstance(cfg, httpClient);

    workspaceFactory = WorkspaceFactory.getInstance();
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String body = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);

    String args[] = body.split(",");

    String workspaceID = args[0];
    String image = args[1];
    String executionID = args[2];

    try {
      if (docker
          .pullImageCmd(image)
          .exec(new ResultCallback.Adapter<>())
          .awaitCompletion(5, TimeUnit.MINUTES)) {

        CreateContainerResponse container =
            docker
                .createContainerCmd(image)
                .withAttachStdout(true)
                .withAttachStderr(true)
                // .withHostConfig(HostConfig.newHostConfig().withAutoRemove(true))
                .exec();

        try {
          PipedOutputStream tarOut = new PipedOutputStream();
          PipedInputStream tarIn = new PipedInputStream(tarOut);

          Workspace w = workspaceFactory.fromWorkspaceID(workspaceID);
          w.getArchive(ArchiveType.TAR).archive(tarOut);

          docker.copyArchiveToContainerCmd(container.getId()).withTarInputStream(tarIn).exec();

          ByteArrayOutputStream stdOut = new ByteArrayOutputStream();

          ResultCallback.Adapter<Frame> adapter =
              docker
                  .attachContainerCmd(container.getId())
                  .withStdOut(true)
                  .withStdErr(true)
                  .withFollowStream(true)
                  .exec(
                      new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame object) {
                          try {
                            stdOut.write(object.getPayload());
                            stdOut.flush();
                          } catch (IOException e) {
                            onError(e);
                          }
                        }
                      });

          Thread.sleep(100);
          docker.startContainerCmd(container.getId()).exec();

          if (adapter.awaitCompletion(5, TimeUnit.MINUTES)) {
            w.updateExecutionOutput(executionID, stdOut.toString());
            resp.getWriter().println(executionID);
            resp.getWriter().println(stdOut.toString());
          } else {
            docker.killContainerCmd(container.getId()).exec();
            resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
          }
        } catch (InterruptedException | ExecutionException e) {
          docker.killContainerCmd(container.getId()).exec();
          resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
      } else {
        resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
      }
    } catch (InterruptedException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}

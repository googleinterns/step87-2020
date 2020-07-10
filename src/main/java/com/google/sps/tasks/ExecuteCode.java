package com.google.sps.tasks;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.sps.workspace.Workspace;
import com.google.sps.workspace.WorkspaceArchive.ArchiveType;
import com.google.sps.workspace.WorkspaceFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    String executionID = args[1];

    try {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

      Workspace w = workspaceFactory.fromWorkspaceID(workspaceID);
      Entity env =
          datastore.get(KeyFactory.stringToKey(w.getEnvironment().get(10, TimeUnit.SECONDS)));
      String image = (String) env.getProperty("image");
      String tag = (String) env.getProperty("tag");

      CreateContainerResponse container =
          docker
              .createContainerCmd(image + ':' + tag)
              .withAttachStdout(true)
              .withAttachStderr(true)
              .withTty(true)
              .withHostConfig(HostConfig.newHostConfig().withAutoRemove(true))
              .exec();

      try {
        ByteArrayOutputStream tarOut = new ByteArrayOutputStream();

        w.getArchive(ArchiveType.TAR).archive(tarOut);

        ByteArrayInputStream tarIn = new ByteArrayInputStream(tarOut.toByteArray());

        docker
            .copyArchiveToContainerCmd(container.getId())
            .withTarInputStream(tarIn)
            .withRemotePath("/workspace")
            .exec();

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
                          w.writeOutput(executionID, new String(object.getPayload()));
                        } catch (ExecutionException | InterruptedException e) {
                          onError(e);
                        }
                      }
                    });

        // Without this sometimes the adapter will complete with out eny output.
        // This may be because the container is started before the adapter is added.
        Thread.sleep(100);
        docker.startContainerCmd(container.getId()).exec();

        if (adapter.awaitCompletion(5, TimeUnit.MINUTES)) {
          // In the future we will get the exit code of the container.
          w.setExitCode(executionID, 0);
        } else {
          docker.killContainerCmd(container.getId()).exec();
          resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
        }
      } catch (InterruptedException | ExecutionException | DockerException e) {
        docker.killContainerCmd(container.getId()).exec();
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } catch (InterruptedException
        | EntityNotFoundException
        | ExecutionException
        | TimeoutException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  static class OutputAdapter extends ResultCallback.Adapter<Frame> {
    private OutputStream stdOut;

    public OutputAdapter(OutputStream stdOut) {
      this.stdOut = Objects.requireNonNull(stdOut);
    }

    @Override
    public void onNext(Frame object) {
      try {
        stdOut.write(object.getPayload());
        stdOut.flush();
      } catch (IOException e) {
        onError(e);
      }
    }
  }
}

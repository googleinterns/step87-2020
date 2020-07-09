package com.google.sps.tasks;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.DockerException;
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
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/tasks/pullEnv")
public class PullNewEnvironment extends HttpServlet {
  DockerClient docker;

  @Override
  public void init() throws ServletException {
    DockerClientConfig cfg = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    DockerHttpClient httpClient =
        new ApacheDockerHttpClient.Builder()
            .dockerHost(cfg.getDockerHost())
            .sslConfig(cfg.getSSLConfig())
            .build();
    docker = DockerClientImpl.getInstance(cfg, httpClient);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String body = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);

    String args[] = body.split(",");

    String entityID = args[0];
    String classID = args[1];
    String image = args[2];
    String tag = args[3];

    String dockerSafeClassID = classID.replace("-", "").toLowerCase();

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    try {
      try {
        if (docker
            .pullImageCmd(image + ":" + tag)
            .exec(new ResultCallback.Adapter<>())
            .awaitCompletion(5, TimeUnit.MINUTES)) {

          docker.tagImageCmd(image, dockerSafeClassID + '-' + image, tag).exec();

          docker.removeImageCmd(image).exec();

          Entity e = datastore.get(KeyFactory.stringToKey(entityID));
          e.setProperty("status", "ready");
          e.setProperty("image", dockerSafeClassID + '-' + image);
          e.setProperty("tag", tag);
          datastore.put(e);
        } else {
          Entity e = datastore.get(KeyFactory.stringToKey(entityID));
          e.setProperty("status", "timeout");
          datastore.put(e);
        }
      } catch (InterruptedException | DockerException e) {
        Entity entity = datastore.get(KeyFactory.stringToKey(entityID));
        entity.setProperty("status", "failed");
        datastore.put(entity);
      }
    } catch (EntityNotFoundException e) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }
}

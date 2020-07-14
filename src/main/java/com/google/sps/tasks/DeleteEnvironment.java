package com.google.sps.tasks;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
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
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/tasks/deleteEnv")
public class DeleteEnvironment extends HttpServlet {
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
    String entityID =
        req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    try {
      Entity e = datastore.get(KeyFactory.stringToKey(entityID));
      docker
          .removeImageCmd(((String) e.getProperty("image")) + ':' + (String) e.getProperty("tag"))
          .exec();
      datastore.delete(e.getKey());
    } catch (EntityNotFoundException e) {
      resp.getWriter().write("Could not find entity");
    } catch (NotFoundException e) {
      resp.getWriter().write("Image does not exist");
      datastore.delete(KeyFactory.stringToKey(entityID));
    }
  }
}

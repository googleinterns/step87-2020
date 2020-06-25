package com.google.sps.servlets;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/requestDownload")
public class RequestDownload extends HttpServlet {
  private static String project = "fulfillment-deco-step-2020";
  private static String location = "us-central1";
  private static String queue = "download-queue";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try (CloudTasksClient client = CloudTasksClient.create()) {
      String queuePath = QueueName.of(project, location, queue).toString();

      Task.Builder taskBuilder =
          Task.newBuilder()
              .setAppEngineHttpRequest(
                  AppEngineHttpRequest.newBuilder()
                      .setBody(ByteString.copyFrom("test", Charset.defaultCharset()))
                      .setRelativeUri("/tasks/download")
                      .setHttpMethod(HttpMethod.POST)
                      .build());

      Task task = client.createTask(queuePath, taskBuilder.build());
    }
  }
}
